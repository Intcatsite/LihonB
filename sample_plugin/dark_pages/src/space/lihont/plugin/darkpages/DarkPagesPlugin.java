package space.lihont.plugin.darkpages;

import android.app.Activity;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;
import android.webkit.WebView;
import space.lihont.browser.plugin.LihonPlugin;
import space.lihont.browser.plugin.PluginContext;

/**
 * Dark Pages Plugin for LihonB (.lbp)
 * Injects smart dark mode CSS into web pages and provides home screen inversion.
 */
public class DarkPagesPlugin extends LihonPlugin {

    private static final String KEY_PAGES = "dark_pages_enabled";
    private static final String KEY_HOME = "home_invert";

    // High quality dark mode CSS filter with image preservation
    private static final String DARK_CSS =
            "html { filter: invert(0.92) hue-rotate(180deg) !important; background: #141218 !important; } " +
            "img, video, picture, svg, canvas { filter: invert(1) hue-rotate(180deg) !important; }";

    private View homeRoot;

    @Override
    public void onStart(PluginContext ctx) {
        ctx.addSettingsSection("Режим чтения и темы");
        ctx.addSettingsToggle("Тёмный режим веб-сайтов",
                "Инвертирует цвета страниц для комфортного чтения в темноте", KEY_PAGES, true);
        ctx.addSettingsToggle("Инверсия стартового экрана",
                "Экспериментальная инверсия интерфейса главного экрана", KEY_HOME, false);

        ctx.showToast("🌙 Плагин 'Тёмные страницы' включён!");
        ctx.log("Dark Pages activated");
    }

    @Override
    public void onStop() {
        if (homeRoot != null) {
            homeRoot.post(() -> homeRoot.setLayerType(View.LAYER_TYPE_NONE, null));
            homeRoot = null;
        }
    }

    @Override
    public void onHomeScreenCreated(Activity activity, View homeScreenRoot) {
        homeRoot = homeScreenRoot;
        applyHomeInvert();
    }

    @Override
    public void onSettingsChanged(String key) {
        if (KEY_HOME.equals(key)) {
            Activity a = getContext().getActivity();
            if (a != null) {
                a.runOnUiThread(this::applyHomeInvert);
            }
        }
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
        if (getContext().prefs().getBoolean(KEY_PAGES, true)) {
            getContext().injectCss(webView, DARK_CSS);
        }
    }

    private void applyHomeInvert() {
        if (homeRoot == null) return;

        boolean on = getContext().prefs().getBoolean(KEY_HOME, false);
        if (on) {
            ColorMatrix matrix = new ColorMatrix(new float[]{
                    -1,  0,  0, 0, 255,
                     0, -1,  0, 0, 255,
                     0,  0, -1, 0, 255,
                     0,  0,  0, 1,   0
            });
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(matrix));
            homeRoot.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        } else {
            homeRoot.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }
}
