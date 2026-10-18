package space.lihont.browser.plugin;

import android.app.Activity;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

/**
 * Base class for all Lihon Browser Plugins (.lbp).
 * Inspired by MargyT plugin architecture, extended for web & browser customization.
 */
public abstract class LihonPlugin {

    private PluginContext context;

    public void init(PluginContext context) {
        this.context = context;
    }

    public PluginContext getContext() {
        return context;
    }

    // Lifecycle
    public void onStart(PluginContext ctx) {}
    public void onStop() {}
    public void onSettingsChanged(String key) {}

    // Web Events
    public void onPageStarted(WebView view, String url) {}
    public void onPageFinished(WebView view, String url) {}
    public WebResourceResponse shouldInterceptRequest(String url) {
        return null;
    }

    // UI Customization Hooks
    public void onHomeScreenCreated(Activity activity, View homeScreenRoot) {}
}
