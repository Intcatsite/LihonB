package space.lihont.browser.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import space.lihont.browser.MainActivity;

/**
 * Execution context provided to each LihonPlugin.
 * Manages plugin settings declaration, isolated storage, and browser interaction.
 */
public class PluginContext {

    private final String pluginId;
    private final Activity activity;
    private final SharedPreferences prefs;
    private final List<SettingItem> settings = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PluginContext(String pluginId, Activity activity) {
        this.pluginId = pluginId;
        this.activity = activity;
        this.prefs = activity.getSharedPreferences("plugin_" + pluginId, Context.MODE_PRIVATE);
    }

    public Activity getActivity() {
        return activity;
    }

    public SharedPreferences prefs() {
        return prefs;
    }

    public String getPluginId() {
        return pluginId;
    }

    // Declarative Settings API (like MargyT)
    public void addSettingsSection(String title) {
        settings.add(SettingItem.section(title));
    }

    public void addSettingsToggle(String title, String desc, String key, boolean defaultVal) {
        settings.add(SettingItem.toggle(title, desc, key, defaultVal));
    }

    public void addSettingsSlider(String title, String desc, String key, int min, int max, int defaultVal) {
        settings.add(SettingItem.slider(title, desc, key, min, max, defaultVal));
    }

    public void addSettingsChoice(String title, String desc, String key, String[] choices, int defaultIndex) {
        settings.add(SettingItem.choice(title, desc, key, choices, defaultIndex));
    }

    public List<SettingItem> getSettings() {
        return settings;
    }

    // Browser Actions
    public void openUrl(String url) {
        mainHandler.post(() -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).loadUrl(url);
            }
        });
    }

    public void injectJs(WebView webView, String js) {
        mainHandler.post(() -> {
            if (webView != null) {
                webView.evaluateJavascript(js, null);
            }
        });
    }

    public void injectJs(String js) {
        if (activity instanceof MainActivity) {
            injectJs(((MainActivity) activity).getCurrentWebView(), js);
        }
    }

    public void injectCss(WebView webView, String css) {
        String cleanCss = css.replace("'", "\\'").replace("\n", " ");
        String js = "(function() { var s = document.createElement('style'); s.innerHTML = '" + cleanCss + "'; document.head.appendChild(s); })();";
        injectJs(webView, js);
    }

    public void injectCss(String css) {
        if (activity instanceof MainActivity) {
            injectCss(((MainActivity) activity).getCurrentWebView(), css);
        }
    }

    public void showToast(String text) {
        mainHandler.post(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
    }

    public void log(String message) {
        Log.d("LihonPlugin [" + pluginId + "]", message);
    }
}
