package space.lihont.browser.plugin;

import dalvik.system.DexClassLoader;
import java.io.File;

public class LoadedPlugin {
    public final PluginManifest manifest;
    public final File pluginDir;
    public final DexClassLoader classLoader;
    public final LihonPlugin instance;
    public final PluginContext context;
    public boolean enabled;

    public LoadedPlugin(PluginManifest manifest, File pluginDir, DexClassLoader classLoader,
                        LihonPlugin instance, PluginContext context, boolean enabled) {
        this.manifest = manifest;
        this.pluginDir = pluginDir;
        this.classLoader = classLoader;
        this.instance = instance;
        this.context = context;
        this.enabled = enabled;
    }
}
