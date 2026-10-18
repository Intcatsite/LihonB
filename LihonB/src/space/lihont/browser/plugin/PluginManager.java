package space.lihont.browser.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Core engine managing the discovery, dynamic loading, and lifecycle
 * of Lihon Browser Plugins (.lbp).
 */
public class PluginManager {

    private static final String TAG = "PluginManager";
    private static PluginManager sInstance;

    private Activity activity;
    private File pluginsRootDir;
    private File codeCacheDir;
    private SharedPreferences prefs;
    private final List<LoadedPlugin> loadedPlugins = new ArrayList<>();

    public static synchronized PluginManager getInstance() {
        if (sInstance == null) {
            sInstance = new PluginManager();
        }
        return sInstance;
    }

    private PluginManager() {}

    public void init(Activity activity) {
        this.activity = activity;
        this.pluginsRootDir = activity.getDir("plugins", Context.MODE_PRIVATE);
        this.codeCacheDir = activity.getCodeCacheDir();
        this.prefs = activity.getSharedPreferences("lihon_plugins_prefs", Context.MODE_PRIVATE);

        loadAllPlugins();
    }

    public synchronized List<LoadedPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(loadedPlugins));
    }

    public synchronized void loadAllPlugins() {
        loadedPlugins.clear();

        if (pluginsRootDir == null || !pluginsRootDir.exists()) return;

        File[] subdirs = pluginsRootDir.listFiles(File::isDirectory);
        if (subdirs == null) return;

        for (File dir : subdirs) {
            try {
                LoadedPlugin p = loadPluginFromDirectory(dir);
                if (p != null) {
                    loadedPlugins.add(p);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed loading plugin from " + dir.getName(), e);
            }
        }
    }

    private LoadedPlugin loadPluginFromDirectory(File dir) throws Exception {
        File manifestFile = new File(dir, "manifest.json");
        File dexFile = new File(dir, "classes.dex");

        if (!manifestFile.exists() || !dexFile.exists()) {
            Log.w(TAG, "Missing manifest or classes.dex in " + dir.getAbsolutePath());
            return null;
        }

        // Read manifest
        byte[] bytes = new byte[(int) manifestFile.length()];
        try (FileInputStream fis = new FileInputStream(manifestFile)) {
            fis.read(bytes);
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        PluginManifest manifest = PluginManifest.fromJson(json);

        // ClassLoader targeting classes.dex
        File optimizedDir = new File(codeCacheDir, "opt_" + manifest.id);
        optimizedDir.mkdirs();

        // Android 14+ ART requires dynamically loaded dex files to be read-only!
        dexFile.setReadOnly();

        DexClassLoader classLoader = new DexClassLoader(
            dexFile.getAbsolutePath(),
            optimizedDir.getAbsolutePath(),
            null,
            activity.getClassLoader()
        );

        Class<?> clazz = classLoader.loadClass(manifest.entry);
        if (!LihonPlugin.class.isAssignableFrom(clazz)) {
            Log.e(TAG, "Entry class " + manifest.entry + " does not inherit LihonPlugin");
            return null;
        }

        LihonPlugin pluginInstance = (LihonPlugin) clazz.getDeclaredConstructor().newInstance();
        PluginContext ctx = new PluginContext(manifest.id, activity);
        pluginInstance.init(ctx);

        boolean enabled = prefs.getBoolean("plugin_enabled_" + manifest.id, true);
        LoadedPlugin loaded = new LoadedPlugin(manifest, dir, classLoader, pluginInstance, ctx, enabled);

        if (enabled) {
            try {
                pluginInstance.onStart(ctx);
                Log.i(TAG, "Plugin started: " + manifest.getDisplayName() + " v" + manifest.version);
            } catch (Throwable t) {
                Log.e(TAG, "Error in onStart of " + manifest.id, t);
            }
        }

        return loaded;
    }

    public synchronized boolean installPlugin(InputStream zipInput) {
        File tempDir = new File(pluginsRootDir, "temp_install_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(zipInput)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                File target = new File(tempDir, entry.getName());
                target.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }

            File manifestFile = new File(tempDir, "manifest.json");
            File dexFile = new File(tempDir, "classes.dex");

            if (!manifestFile.exists() || !dexFile.exists()) {
                deleteDir(tempDir);
                return false;
            }

            byte[] b = new byte[(int) manifestFile.length()];
            try (FileInputStream fis = new FileInputStream(manifestFile)) {
                fis.read(b);
            }
            PluginManifest manifest = PluginManifest.fromJson(new String(b, StandardCharsets.UTF_8));

            // Move to final directory plugins/<id>/
            File finalDir = new File(pluginsRootDir, manifest.id);
            if (finalDir.exists()) {
                deletePlugin(manifest.id);
            }
            tempDir.renameTo(finalDir);

            // Load and activate
            LoadedPlugin loaded = loadPluginFromDirectory(finalDir);
            if (loaded != null) {
                loadedPlugins.add(loaded);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed installing plugin", e);
            deleteDir(tempDir);
        }
        return false;
    }

    public synchronized boolean installPluginFromFile(File lbpFile) {
        if (!lbpFile.exists()) return false;
        try (FileInputStream fis = new FileInputStream(lbpFile)) {
            return installPlugin(fis);
        } catch (Exception e) {
            Log.e(TAG, "Failed installing from file: " + lbpFile, e);
            return false;
        }
    }

    public synchronized boolean installDemoPlugin(Context ctx) {
        if (ctx == null) return false;
        try (InputStream is = ctx.getAssets().open("plugins/cyber_home.lbp")) {
            return installPlugin(is);
        } catch (Exception e) {
            Log.e(TAG, "Failed installing demo plugin from assets", e);
            return false;
        }
    }

    public synchronized void setPluginEnabled(String id, boolean enabled) {
        for (LoadedPlugin p : loadedPlugins) {
            if (p.manifest.id.equals(id)) {
                if (p.enabled != enabled) {
                    p.enabled = enabled;
                    prefs.edit().putBoolean("plugin_enabled_" + id, enabled).apply();
                    try {
                        if (enabled) {
                            p.instance.onStart(p.context);
                        } else {
                            p.instance.onStop();
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Error toggling plugin " + id, t);
                    }
                }
                break;
            }
        }
    }

    public synchronized void deletePlugin(String id) {
        LoadedPlugin toRemove = null;
        for (LoadedPlugin p : loadedPlugins) {
            if (p.manifest.id.equals(id)) {
                toRemove = p;
                break;
            }
        }

        if (toRemove != null) {
            try {
                if (toRemove.enabled) {
                    toRemove.instance.onStop();
                }
            } catch (Throwable ignored) {}

            loadedPlugins.remove(toRemove);
            deleteDir(toRemove.pluginDir);
            prefs.edit().remove("plugin_enabled_" + id).apply();
        }
    }

    private void deleteDir(File f) {
        if (f == null || !f.exists()) return;
        try { f.setWritable(true); } catch (Exception ignored) {}
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File c : files) deleteDir(c);
            }
        }
        f.delete();
    }

    // =========================================================================
    // DISPATCHERS (BROWSER & UI HOOKS)
    // =========================================================================

    public synchronized void dispatchHomeScreenCreated(Activity a, View homeScreenRoot) {
        for (LoadedPlugin p : loadedPlugins) {
            if (p.enabled) {
                try {
                    p.instance.onHomeScreenCreated(a, homeScreenRoot);
                } catch (Throwable t) {
                    Log.e(TAG, "Error in onHomeScreenCreated of " + p.manifest.id, t);
                }
            }
        }
    }

    public synchronized void dispatchPageStarted(WebView wv, String url) {
        for (LoadedPlugin p : loadedPlugins) {
            if (p.enabled) {
                try {
                    p.instance.onPageStarted(wv, url);
                } catch (Throwable t) {
                    Log.e(TAG, "Error in onPageStarted of " + p.manifest.id, t);
                }
            }
        }
    }

    public synchronized void dispatchPageFinished(WebView wv, String url) {
        for (LoadedPlugin p : loadedPlugins) {
            if (p.enabled) {
                try {
                    p.instance.onPageFinished(wv, url);
                } catch (Throwable t) {
                    Log.e(TAG, "Error in onPageFinished of " + p.manifest.id, t);
                }
            }
        }
    }

    public synchronized WebResourceResponse dispatchIntercept(String url) {
        for (LoadedPlugin p : loadedPlugins) {
            if (p.enabled) {
                try {
                    WebResourceResponse resp = p.instance.shouldInterceptRequest(url);
                    if (resp != null) return resp;
                } catch (Throwable t) {
                    Log.e(TAG, "Error in shouldInterceptRequest of " + p.manifest.id, t);
                }
            }
        }
        return null;
    }
}
