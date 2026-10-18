package space.lihont.plugin.lighttheme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import space.lihont.browser.R;
import space.lihont.browser.plugin.LihonPlugin;
import space.lihont.browser.plugin.PluginContext;

public class LightThemePlugin extends LihonPlugin {

    private static final String KEY_HOME_LIGHT   = "home_light_enabled";
    private static final String KEY_PALETTE      = "theme_palette";
    private static final String KEY_DARK_ICONS   = "status_dark_icons";
    private static final String KEY_WEB_LIGHT    = "web_light_enabled";

    private static final String[] PALETTE_NAMES = {
        "Кремовый Monet (По умолчанию)",
        "Снежно-белый",
        "Лавандовый пастель",
        "Мягкий серый"
    };

    // Palette Definitions [bg, card, stroke, textPrimary, textSecondary, accent, chip]
    private static final int[][] PALETTES = {
        { // 0: Cream Monet
            Color.parseColor("#FAF8FC"),
            Color.parseColor("#F2EDF6"),
            Color.parseColor("#E2DCED"),
            Color.parseColor("#1C1A22"),
            Color.parseColor("#484550"),
            Color.parseColor("#65558F"),
            Color.parseColor("#E6E0EC")
        },
        { // 1: Pure White
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#F5F5F7"),
            Color.parseColor("#E1E2E6"),
            Color.parseColor("#111113"),
            Color.parseColor("#50525A"),
            Color.parseColor("#005FB0"),
            Color.parseColor("#E8EAED")
        },
        { // 2: Lavender Pastel
            Color.parseColor("#F7F2FA"),
            Color.parseColor("#ECE6F0"),
            Color.parseColor("#D9D0E3"),
            Color.parseColor("#1D192B"),
            Color.parseColor("#49454F"),
            Color.parseColor("#6750A4"),
            Color.parseColor("#E8DEF8")
        },
        { // 3: Soft Gray
            Color.parseColor("#F8F9FA"),
            Color.parseColor("#EFF1F3"),
            Color.parseColor("#DDE0E4"),
            Color.parseColor("#202124"),
            Color.parseColor("#5F6368"),
            Color.parseColor("#1A73E8"),
            Color.parseColor("#E2E5E9")
        }
    };

    private Activity currentActivity;
    private View currentHomeRoot;

    @Override
    public void onStart(PluginContext ctx) {
        ctx.log("MD3 Light Theme активирована");
        ctx.addSettingsSection("Оформление интерфейса");
        ctx.addSettingsToggle("Светлый интерфейс", "Применять светлую тему Material 3 к главному экрану и панелям",
                KEY_HOME_LIGHT, true);
        ctx.addSettingsChoice("Палитра темы", "Цветовой оттенок светлого оформления",
                KEY_PALETTE, PALETTE_NAMES, 0);
        ctx.addSettingsToggle("Тёмные значки статус-бара", "Отображать чёткие значки времени и батареи на светлом фоне",
                KEY_DARK_ICONS, true);

        ctx.addSettingsSection("Веб-страницы");
        ctx.addSettingsToggle("Светлый режим сайтов", "Принудительно применять светлый фон к веб-страницам",
                KEY_WEB_LIGHT, false);

        ctx.showToast("Светлая тема MD3 включена!");

        Activity a = ctx.getActivity();
        if (a != null) {
            a.runOnUiThread(this::applyTheme);
        }
    }

    @Override
    public void onStop() {
        Activity a = currentActivity != null ? currentActivity : (getContext() != null ? getContext().getActivity() : null);
        if (a != null) {
            a.runOnUiThread(this::restoreDarkTheme);
        }
        currentActivity = null;
        currentHomeRoot = null;
    }

    @Override
    public void onSettingsChanged(String key) {
        Activity a = currentActivity != null ? currentActivity : (getContext() != null ? getContext().getActivity() : null);
        if (a != null) {
            a.runOnUiThread(this::applyTheme);
        }
    }

    @Override
    public void onHomeScreenCreated(Activity activity, View homeScreenRoot) {
        this.currentActivity = activity;
        this.currentHomeRoot = homeScreenRoot;
        applyTheme();
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
        if (getContext() != null && getContext().prefs().getBoolean(KEY_WEB_LIGHT, false)) {
            String css = "@media (prefers-color-scheme: dark) { :root { color-scheme: light !important; } } " +
                         "body { background-color: #FFFFFF !important; color: #1D1B20 !important; }";
            getContext().injectCss(webView, css);
        }
    }

    // =========================================================================
    // THEME APPLICATION
    // =========================================================================

    private void applyTheme() {
        Activity a = currentActivity != null ? currentActivity : (getContext() != null ? getContext().getActivity() : null);
        if (a == null) return;

        SharedPreferences prefs = getContext() != null ? getContext().prefs() : null;
        boolean enabled = prefs == null || prefs.getBoolean(KEY_HOME_LIGHT, true);
        if (!enabled) {
            restoreDarkTheme();
            return;
        }

        int paletteIdx = prefs != null ? prefs.getInt(KEY_PALETTE, 0) : 0;
        paletteIdx = Math.max(0, Math.min(paletteIdx, PALETTES.length - 1));
        int[] palette = PALETTES[paletteIdx];

        int colBg             = palette[0];
        int colCard           = palette[1];
        int colStroke         = palette[2];
        int colTextPrimary    = palette[3];
        int colTextSecondary  = palette[4];
        int colAccent         = palette[5];
        int colChip           = palette[6];

        boolean darkIcons = prefs == null || prefs.getBoolean(KEY_DARK_ICONS, true);

        // 1. Status Bar and Navigation Bar Icons
        Window win = a.getWindow();
        if (win != null) {
            View decor = win.getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (darkIcons) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decor.setSystemUiVisibility(flags);
        }

        // 2. Start Page Background & Views
        View homeRoot = currentHomeRoot != null ? currentHomeRoot : a.findViewById(R.id.start_page);
        if (homeRoot != null) {
            homeRoot.setBackgroundColor(colBg);

            // Greeting & Title
            TextView tvGreeting = homeRoot.findViewById(R.id.tv_greeting);
            if (tvGreeting != null) {
                tvGreeting.setTextColor(colTextPrimary);
            }
            TextView tvTitle = homeRoot.findViewById(R.id.tv_app_title_home);
            if (tvTitle != null) {
                tvTitle.setTextColor(colTextPrimary);
            }

            // Incognito Button
            View btnIncognito = homeRoot.findViewById(R.id.btn_toggle_incognito);
            if (btnIncognito != null) {
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setCornerRadius(dp(a, 20));
                gd.setColor(colChip);
                gd.setStroke(dp(a, 1), colStroke);
                btnIncognito.setBackground(gd);
            }

            // Big Search Bar
            View searchBar = homeRoot.findViewById(R.id.big_search_bar);
            if (searchBar != null) {
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setCornerRadius(dp(a, 28));
                gd.setColor(colCard);
                gd.setStroke(dp(a, 1.5f), colStroke);
                searchBar.setBackground(gd);

                TextView tvPlaceholder = searchBar.findViewById(R.id.tv_search_placeholder);
                if (tvPlaceholder != null) {
                    tvPlaceholder.setTextColor(colTextSecondary);
                }

                // Search icon tint
                if (searchBar instanceof ViewGroup) {
                    ViewGroup sg = (ViewGroup) searchBar;
                    for (int i = 0; i < sg.getChildCount(); i++) {
                        View child = sg.getChildAt(i);
                        if (child instanceof ImageView && child.getId() != R.id.img_current_engine) {
                            ((ImageView) child).setColorFilter(colTextSecondary, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
            }

            // Engine Selector Chip
            View btnEngine = homeRoot.findViewById(R.id.btn_engine_selector);
            if (btnEngine != null) {
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setCornerRadius(dp(a, 18));
                gd.setColor(colChip);
                gd.setStroke(dp(a, 1), colStroke);
                btnEngine.setBackground(gd);

                TextView tvEngineName = btnEngine.findViewById(R.id.tv_current_engine_name);
                if (tvEngineName != null) {
                    tvEngineName.setTextColor(colAccent);
                }
            }

            // Quick Sites Grid Labels & Tiles
            View quickSites = homeRoot.findViewById(R.id.layout_quick_sites);
            if (quickSites instanceof ViewGroup) {
                applyLightToQuickSites(a, (ViewGroup) quickSites, colCard, colStroke, colTextPrimary);
            }
        }

        // 3. Floating Bottom Bar & Omnibox Capsule
        View bottomBar = a.findViewById(R.id.bottom_bar_card);
        if (bottomBar != null) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(dp(a, 29));
            gd.setColor(colCard);
            gd.setStroke(dp(a, 1.5f), colStroke);
            bottomBar.setBackground(gd);
        }

        View omnibox = a.findViewById(R.id.omnibox_capsule);
        if (omnibox != null) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(dp(a, 22));
            gd.setColor(colChip);
            gd.setStroke(dp(a, 1), colStroke);
            omnibox.setBackground(gd);

            TextView tvUrl = omnibox.findViewById(R.id.tv_url_display);
            if (tvUrl != null) {
                tvUrl.setTextColor(colTextPrimary);
            }

            ImageView imgStatus = omnibox.findViewById(R.id.img_omnibox_status);
            if (imgStatus != null) {
                imgStatus.setColorFilter(colTextPrimary, PorterDuff.Mode.SRC_IN);
            }
        }

        TextView tvTabCount = a.findViewById(R.id.tv_tab_count);
        if (tvTabCount != null) {
            tvTabCount.setTextColor(colTextPrimary);
        }

        ImageView btnMore = a.findViewById(R.id.btn_more);
        if (btnMore != null) {
            btnMore.setColorFilter(colTextPrimary, PorterDuff.Mode.SRC_IN);
        }
    }

    private void applyLightToQuickSites(Context c, ViewGroup root, int colCard, int colStroke, int colTextPrimary) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) child;
                for (int j = 0; j < vg.getChildCount(); j++) {
                    View sub = vg.getChildAt(j);
                    if (sub instanceof FrameLayout) {
                        GradientDrawable gd = new GradientDrawable();
                        gd.setShape(GradientDrawable.RECTANGLE);
                        gd.setCornerRadius(dp(c, 18));
                        gd.setColor(colCard);
                        gd.setStroke(dp(c, 1.2f), colStroke);
                        sub.setBackground(gd);
                    } else if (sub instanceof TextView) {
                        ((TextView) sub).setTextColor(colTextPrimary);
                    } else if (sub instanceof ViewGroup) {
                        applyLightToQuickSites(c, (ViewGroup) sub, colCard, colStroke, colTextPrimary);
                    }
                }
            }
        }
    }

    // =========================================================================
    // RESTORE ORIGINAL DARK THEME
    // =========================================================================

    private void restoreDarkTheme() {
        Activity a = currentActivity != null ? currentActivity : (getContext() != null ? getContext().getActivity() : null);
        if (a == null) return;

        // 1. Reset Status and Navigation Bar Flags
        Window win = a.getWindow();
        if (win != null) {
            View decor = win.getDecorView();
            int flags = decor.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }

        // 2. Restore Home Screen Views
        View homeRoot = currentHomeRoot != null ? currentHomeRoot : a.findViewById(R.id.start_page);
        if (homeRoot != null) {
            homeRoot.setBackgroundColor(Color.parseColor("#141218"));

            TextView tvGreeting = homeRoot.findViewById(R.id.tv_greeting);
            if (tvGreeting != null) {
                tvGreeting.setTextColor(Color.parseColor("#E6E0E9"));
            }
            TextView tvTitle = homeRoot.findViewById(R.id.tv_app_title_home);
            if (tvTitle != null) {
                tvTitle.setTextColor(Color.parseColor("#E6E0E9"));
            }

            View btnIncognito = homeRoot.findViewById(R.id.btn_toggle_incognito);
            if (btnIncognito != null) {
                btnIncognito.setBackgroundResource(R.drawable.bg_pill);
            }

            View searchBar = homeRoot.findViewById(R.id.big_search_bar);
            if (searchBar != null) {
                searchBar.setBackgroundResource(R.drawable.bg_search_large);
                TextView tvPlaceholder = searchBar.findViewById(R.id.tv_search_placeholder);
                if (tvPlaceholder != null) {
                    tvPlaceholder.setTextColor(Color.parseColor("#938F99"));
                }
                if (searchBar instanceof ViewGroup) {
                    ViewGroup sg = (ViewGroup) searchBar;
                    for (int i = 0; i < sg.getChildCount(); i++) {
                        View child = sg.getChildAt(i);
                        if (child instanceof ImageView && child.getId() != R.id.img_current_engine) {
                            ((ImageView) child).clearColorFilter();
                        }
                    }
                }
            }

            View btnEngine = homeRoot.findViewById(R.id.btn_engine_selector);
            if (btnEngine != null) {
                btnEngine.setBackgroundResource(R.drawable.bg_omnibox);
                TextView tvEngineName = btnEngine.findViewById(R.id.tv_current_engine_name);
                if (tvEngineName != null) {
                    tvEngineName.setTextColor(Color.parseColor("#D0BCFF"));
                }
            }

            View quickSites = homeRoot.findViewById(R.id.layout_quick_sites);
            if (quickSites instanceof ViewGroup) {
                restoreQuickSitesDark((ViewGroup) quickSites);
            }
        }

        // 3. Restore Bottom Bar & Omnibox
        View bottomBar = a.findViewById(R.id.bottom_bar_card);
        if (bottomBar != null) {
            bottomBar.setBackgroundResource(R.drawable.bg_bottom_bar);
        }

        View omnibox = a.findViewById(R.id.omnibox_capsule);
        if (omnibox != null) {
            omnibox.setBackgroundResource(R.drawable.bg_omnibox);
            TextView tvUrl = omnibox.findViewById(R.id.tv_url_display);
            if (tvUrl != null) {
                tvUrl.setTextColor(Color.parseColor("#CAC4D0"));
            }
            ImageView imgStatus = omnibox.findViewById(R.id.img_omnibox_status);
            if (imgStatus != null) {
                imgStatus.clearColorFilter();
            }
        }

        TextView tvTabCount = a.findViewById(R.id.tv_tab_count);
        if (tvTabCount != null) {
            tvTabCount.setTextColor(Color.parseColor("#D0BCFF"));
        }

        ImageView btnMore = a.findViewById(R.id.btn_more);
        if (btnMore != null) {
            btnMore.clearColorFilter();
        }
    }

    private void restoreQuickSitesDark(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) child;
                for (int j = 0; j < vg.getChildCount(); j++) {
                    View sub = vg.getChildAt(j);
                    if (sub instanceof FrameLayout) {
                        sub.setBackgroundResource(R.drawable.bg_quick_site);
                    } else if (sub instanceof TextView) {
                        ((TextView) sub).setTextColor(Color.parseColor("#E6E0E9"));
                    } else if (sub instanceof ViewGroup) {
                        restoreQuickSitesDark((ViewGroup) sub);
                    }
                }
            }
        }
    }

    private int dp(Context c, float value) {
        float density = 2.0f;
        if (c != null) {
            density = c.getResources().getDisplayMetrics().density;
        }
        return Math.round(value * density);
    }
}
