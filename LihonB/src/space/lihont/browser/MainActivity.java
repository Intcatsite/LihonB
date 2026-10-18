package space.lihont.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import space.lihont.browser.plugin.PluginManager;
import space.lihont.browser.plugin.PluginsDialog;

public class MainActivity extends Activity {

    // UI elements
    private FrameLayout webContainer;
    private View startPageView;
    private ProgressBar progressBar;
    private TextView tvUrlDisplay;
    private ImageView imgOmniboxStatus;
    private TextView tvTabCount;
    private LinearLayout bannerIncognito;
    private LinearLayout searchOverlay;
    private EditText editSearchInput;

    // Search Bar on start page
    private TextView tvSearchPlaceholder;
    private ImageView imgCurrentEngine;
    private TextView tvCurrentEngineName;
    private TextView tvGreeting;

    // AdBlock Card UI
    private LinearLayout cardAdblockStatus;
    private ImageView imgAdblockShield;
    private TextView tvAdblockTitle;
    private TextView tvAdblockStats;
    private TextView tvAdblockPill;

    // Incognito button UI
    private TextView tvIncognitoText;
    private ImageView imgIncognitoIcon;

    // Tab Management
    private final List<Tab> tabs = new ArrayList<>();
    private int currentTabIndex = 0;
    private int nextTabId = 1;

    // Search Engine State
    private static final String PREF_ENGINE = "search_engine";
    private String currentEngine = "Google";
    private String currentEngineUrl = "https://www.google.com/search?q=";

    // State flags
    private boolean isIncognito = false;
    private boolean isDesktopMode = false;
    private String defaultUserAgent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-Edge Navigation: content extends under transparent navigation bar
        Window window = getWindow();
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
        window.getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.root_layout);
        if (root != null) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
                return insets;
            });
        }

        initViews();
        loadSavedEngine();
        initTabs();
        setupListeners();
        updateAdBlockVisuals();
        PluginManager.getInstance().init(this);
        PluginManager.getInstance().dispatchHomeScreenCreated(this, startPageView);
        handleIntent(getIntent());
    }

    private void initViews() {
        webContainer = findViewById(R.id.web_container);
        startPageView = findViewById(R.id.start_page);
        progressBar = findViewById(R.id.page_progress);
        tvUrlDisplay = findViewById(R.id.tv_url_display);
        imgOmniboxStatus = findViewById(R.id.img_omnibox_status);
        tvTabCount = findViewById(R.id.tv_tab_count);
        bannerIncognito = findViewById(R.id.banner_incognito);
        searchOverlay = findViewById(R.id.search_overlay);
        editSearchInput = findViewById(R.id.edit_search_input);

        tvSearchPlaceholder = findViewById(R.id.tv_search_placeholder);
        imgCurrentEngine = findViewById(R.id.img_current_engine);
        tvCurrentEngineName = findViewById(R.id.tv_current_engine_name);
        tvGreeting = findViewById(R.id.tv_greeting);
        updateGreeting();

        imgIncognitoIcon = findViewById(R.id.img_incognito_icon);
    }

    // =========================================================================
    // TAB MANAGEMENT (REAL MULTI-TAB)
    // =========================================================================
    private void initTabs() {
        // Create initial default tab
        createTab(null, isIncognito);
    }

    private Tab createTab(String initialUrl, boolean incognitoMode) {
        WebView wv = new WebView(this);
        wv.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        wv.setVisibility(View.GONE);

        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(incognitoMode ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        if (defaultUserAgent == null) {
            defaultUserAgent = s.getUserAgentString();
        }

        SharedPreferences prefs = getSharedPreferences(BrowserSettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        String savedUa = prefs.getString(BrowserSettingsDialog.KEY_USER_AGENT, null);
        if (savedUa != null && !savedUa.isEmpty()) {
            s.setUserAgentString(savedUa);
        } else if (isDesktopMode) {
            s.setUserAgentString(BrowserSettingsDialog.UA_DESKTOP);
        }

        wv.setWebChromeClient(new LihonWebChromeClient(progressBar, tvUrlDisplay));
        wv.setWebViewClient(new LihonWebViewClient(this, startPageView, tvUrlDisplay, tvAdblockStats, incognitoMode));

        Tab tab = new Tab(nextTabId++, "Новая вкладка", initialUrl, incognitoMode, wv);
        tabs.add(tab);
        webContainer.addView(wv);

        selectTab(tabs.size() - 1);
        updateTabCounter();

        if (initialUrl != null && !initialUrl.isEmpty()) {
            loadUrl(initialUrl);
        }

        return tab;
    }

    private void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        // Hide old active WebView
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            tabs.get(currentTabIndex).getWebView().setVisibility(View.GONE);
        }

        currentTabIndex = index;
        Tab active = tabs.get(currentTabIndex);
        WebView activeWv = active.getWebView();

        if (active.getUrl() != null && !active.getUrl().isEmpty()) {
            startPageView.setVisibility(View.GONE);
            activeWv.setVisibility(View.VISIBLE);
            tvUrlDisplay.setText(active.getUrl());
        } else {
            activeWv.setVisibility(View.GONE);
            startPageView.setVisibility(View.VISIBLE);
            tvUrlDisplay.setText("Поиск или веб-адрес");
            updateGreeting();
            PluginManager.getInstance().dispatchHomeScreenCreated(this, startPageView);
        }

        isIncognito = active.isIncognito();
        updateIncognitoBanner(isIncognito);
        if (isIncognito) {
            if (tvIncognitoText != null) tvIncognitoText.setText("✕ Закрыть Инкогнито (стереть всё)");
            if (imgIncognitoIcon != null) imgIncognitoIcon.setImageResource(R.drawable.ic_close);
            imgOmniboxStatus.setImageResource(R.drawable.ic_incognito);
        } else {
            if (tvIncognitoText != null) tvIncognitoText.setText("Перейти в режим Инкогнито");
            if (imgIncognitoIcon != null) imgIncognitoIcon.setImageResource(R.drawable.ic_incognito);
            imgOmniboxStatus.setImageResource(R.drawable.ic_shield);
        }
        updateTabCounter();
    }

    public WebView getCurrentWebView() {
        if (tabs != null && currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex).getWebView();
        }
        return null;
    }

    private void closeTab(int index) {
        if (tabs.size() <= 1) {
            // If closing last tab, check if it's incognito
            Tab only = tabs.get(0);
            if (only.isIncognito()) {
                purgeWebViewData(only.getWebView());
                webContainer.removeView(only.getWebView());
                only.getWebView().destroy();
                tabs.clear();
                createTab(null, false);
                try {
                    WebStorage.getInstance().deleteAllData();
                    CookieManager.getInstance().removeSessionCookies(null);
                    CookieManager.getInstance().flush();
                } catch (Exception ignored) {}
                Toast.makeText(this, "Инкогнито вкладка закрыта, данные стёрты", Toast.LENGTH_SHORT).show();
                return;
            }
            only.setUrl(null);
            only.setTitle("Новая вкладка");
            only.getWebView().loadUrl("about:blank");
            selectTab(0);
            return;
        }

        Tab toRemove = tabs.remove(index);
        if (toRemove.isIncognito()) {
            purgeWebViewData(toRemove.getWebView());
            try {
                WebStorage.getInstance().deleteAllData();
                CookieManager.getInstance().removeSessionCookies(null);
            } catch (Exception ignored) {}
        }
        webContainer.removeView(toRemove.getWebView());
        toRemove.getWebView().destroy();

        if (currentTabIndex >= tabs.size()) {
            currentTabIndex = tabs.size() - 1;
        }
        selectTab(currentTabIndex);
    }

    private void updateTabCounter() {
        tvTabCount.setText(String.valueOf(tabs.size()));
    }

    private void showTabsDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_tabs, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_tabs_title);
        tvTitle.setText("Открытые вкладки (" + tabs.size() + ")");

        Button btnNewTab = dialogView.findViewById(R.id.btn_dialog_new_tab);
        btnNewTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTab(null, isIncognito);
                dialog.dismiss();
            }
        });

        LinearLayout container = dialogView.findViewById(R.id.tabs_container);

        for (int i = 0; i < tabs.size(); i++) {
            final int tabIdx = i;
            final Tab t = tabs.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(32, 24, 24, 24);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            if (tabIdx == currentTabIndex) {
                card.setBackgroundResource(R.drawable.bg_card_adblock_on);
            } else {
                card.setBackgroundResource(R.drawable.bg_card);
            }

            // Tab Icon
            ImageView icon = new ImageView(this);
            icon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            icon.setImageResource(t.isIncognito() ? R.drawable.ic_incognito : R.drawable.ic_globe);
            card.addView(icon);

            // Tab Title & URL
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tclp.setMargins(24, 0, 16, 0);
            textCol.setLayoutParams(tclp);

            TextView title = new TextView(this);
            title.setText(t.getTitle());
            title.setTextColor(Color.parseColor("#E6E0E9"));
            title.setTextSize(14);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            textCol.addView(title);

            TextView url = new TextView(this);
            url.setText(t.getUrl() != null ? t.getUrl() : "Стартовый экран");
            url.setTextColor(Color.parseColor("#CAC4D0"));
            url.setTextSize(12);
            textCol.addView(url);

            card.addView(textCol);

            // Close Tab Button (✕)
            ImageView closeBtn = new ImageView(this);
            closeBtn.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
            closeBtn.setPadding(12, 12, 12, 12);
            closeBtn.setImageResource(R.drawable.ic_close);
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeTab(tabIdx);
                    dialog.dismiss();
                    showTabsDialog();
                }
            });
            card.addView(closeBtn);

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectTab(tabIdx);
                    dialog.dismiss();
                }
            });

            container.addView(card);
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    // =========================================================================
    // SEARCH ENGINE MANAGEMENT
    // =========================================================================
    private void loadSavedEngine() {
        SharedPreferences p = getSharedPreferences("lihonb_prefs", MODE_PRIVATE);
        currentEngine = p.getString(PREF_ENGINE, "Google");
        applyEngine(currentEngine);
    }

    private void applyEngine(String engine) {
        currentEngine = engine;
        if ("Yandex".equalsIgnoreCase(engine)) {
            currentEngineUrl = "https://ya.ru/search/?text=";
            imgCurrentEngine.setImageResource(R.drawable.ic_engine_yandex);
            tvCurrentEngineName.setText("Yandex");
            tvSearchPlaceholder.setText("Поиск в Yandex или веб-адрес");
        } else if ("DuckDuckGo".equalsIgnoreCase(engine)) {
            currentEngineUrl = "https://duckduckgo.com/?q=";
            imgCurrentEngine.setImageResource(R.drawable.ic_engine_duck);
            tvCurrentEngineName.setText("Duck");
            tvSearchPlaceholder.setText("Поиск в DuckDuckGo или веб-адрес");
        } else {
            currentEngineUrl = "https://www.google.com/search?q=";
            imgCurrentEngine.setImageResource(R.drawable.ic_engine_google);
            tvCurrentEngineName.setText("Google");
            tvSearchPlaceholder.setText("Поиск в Google или веб-адрес");
        }

        SharedPreferences p = getSharedPreferences("lihonb_prefs", MODE_PRIVATE);
        p.edit().putString(PREF_ENGINE, currentEngine).apply();
    }

    private void showEngineSelector(View anchor) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        float density = getResources().getDisplayMetrics().density;
        int p16 = (int) (16 * density);
        int p20 = (int) (20 * density);
        int p12 = (int) (12 * density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p20, p20, p20, p20);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E1A22"));
        bg.setCornerRadius(24 * density);
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("Поисковая система");
        title.setTextColor(Color.parseColor("#E6E0E9"));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, p16);
        root.addView(title);

        String[][] engines = {
            {"Google", "Основной глобальный поиск", "ic_engine_google"},
            {"Yandex", "Быстрый поиск по рунету и картинки", "ic_engine_yandex"},
            {"DuckDuckGo", "Приватный поиск без отслеживания", "ic_engine_duck"}
        };

        for (String[] eng : engines) {
            final String engName = eng[0];
            String engDesc = eng[1];
            String iconResName = eng[2];
            boolean isSelected = engName.equalsIgnoreCase(currentEngine);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(p16, p12, p16, p12);

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(isSelected ? Color.parseColor("#382E47") : Color.parseColor("#2B2733"));
            rowBg.setCornerRadius(16 * density);
            row.setBackground(rowBg);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = (int) (8 * density);
            row.setLayoutParams(rowLp);

            // Icon
            ImageView icon = new ImageView(this);
            int iconSize = (int) (28 * density);
            icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            int resId = getResources().getIdentifier(iconResName, "drawable", getPackageName());
            if (resId != 0) {
                icon.setImageResource(resId);
            }
            row.addView(icon);

            // Text column
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textColLp.setMarginStart((int) (14 * density));
            textCol.setLayoutParams(textColLp);

            TextView tvName = new TextView(this);
            tvName.setText(engName);
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(isSelected ? Color.parseColor("#D0BCFF") : Color.parseColor("#E6E0E9"));
            textCol.addView(tvName);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(engDesc);
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(Color.parseColor("#CAC4D0"));
            textCol.addView(tvDesc);

            row.addView(textCol);

            // Selection checkmark
            if (isSelected) {
                ImageView check = new ImageView(this);
                int checkSize = (int) (20 * density);
                check.setLayoutParams(new LinearLayout.LayoutParams(checkSize, checkSize));
                check.setImageResource(R.drawable.ic_check);
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                applyEngine(engName);
                dialog.dismiss();
            });

            root.addView(row);
        }

        dialog.setContentView(root);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (340 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // =========================================================================
    // INCOGNITO MODE (FULL DATA PURGE & TAB DESTRUCTION)
    // =========================================================================
    public void toggleIncognitoMode() {
        if (!isIncognito) {
            isIncognito = true;
            updateIncognitoBanner(true);
            if (tvIncognitoText != null) tvIncognitoText.setText("✕ Закрыть Инкогнито (стереть всё)");
            if (imgIncognitoIcon != null) imgIncognitoIcon.setImageResource(R.drawable.ic_close);
            imgOmniboxStatus.setImageResource(R.drawable.ic_incognito);
            Toast.makeText(this, "Режим Инкогнито включён (сессия изолирована)", Toast.LENGTH_SHORT).show();
            createTab(null, true);
        } else {
            destroyAllIncognitoTabs(true);
        }
    }

    private void destroyAllIncognitoTabs(boolean showToast) {
        int destroyedCount = 0;
        Iterator<Tab> it = tabs.iterator();
        while (it.hasNext()) {
            Tab t = it.next();
            if (t.isIncognito()) {
                WebView wv = t.getWebView();
                purgeWebViewData(wv);
                webContainer.removeView(wv);
                wv.destroy();
                it.remove();
                destroyedCount++;
            }
        }

        // Global purge of storage, cookies & geolocation for incognito session
        try {
            WebStorage.getInstance().deleteAllData();
            GeolocationPermissions.getInstance().clearAll();
            CookieManager.getInstance().removeSessionCookies(null);
            CookieManager.getInstance().flush();
        } catch (Exception ignored) {}

        isIncognito = false;
        updateIncognitoBanner(false);
        if (tvIncognitoText != null) tvIncognitoText.setText("Перейти в режим Инкогнито");
        if (imgIncognitoIcon != null) imgIncognitoIcon.setImageResource(R.drawable.ic_incognito);
        imgOmniboxStatus.setImageResource(R.drawable.ic_shield);

        if (tabs.isEmpty()) {
            createTab(null, false);
        } else {
            if (currentTabIndex >= tabs.size() || currentTabIndex < 0) {
                currentTabIndex = tabs.size() - 1;
            }
            selectTab(currentTabIndex);
        }
        updateTabCounter();

        if (showToast && destroyedCount > 0) {
            Toast.makeText(this, "Инкогнито закрыт: все сайты (" + destroyedCount + ") и данные стёрты", Toast.LENGTH_LONG).show();
        }
    }

    private void purgeWebViewData(WebView wv) {
        if (wv == null) return;
        try {
            wv.stopLoading();
            wv.clearHistory();
            wv.clearFormData();
            wv.clearCache(true);
            wv.clearSslPreferences();
            wv.loadUrl("about:blank");
        } catch (Exception ignored) {}
    }

    private void updateIncognitoBanner(boolean active) {
        bannerIncognito.setVisibility(active ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // ADBLOCK VISUAL STATES (ACTIVE VS INACTIVE)
    // =========================================================================
    public void toggleAdBlock() {
        boolean nextState = !AdBlocker.isEnabled();
        AdBlocker.setEnabled(nextState);
        updateAdBlockVisuals();
        Toast.makeText(this, nextState ? "AdBlock включён" : "AdBlock выключен", Toast.LENGTH_SHORT).show();
    }

    private void updateGreeting() {
        if (tvGreeting == null) return;
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            tvGreeting.setText("Доброе утро!");
        } else if (hour >= 12 && hour < 18) {
            tvGreeting.setText("Добрый день!");
        } else if (hour >= 18 && hour < 23) {
            tvGreeting.setText("Добрый вечер!");
        } else {
            tvGreeting.setText("Доброй ночи!");
        }
    }

    private void updateAdBlockVisuals() {
        if (cardAdblockStatus == null) return;
        boolean on = AdBlocker.isEnabled();
        if (on) {
            cardAdblockStatus.setBackgroundResource(R.drawable.bg_card_adblock_on);
            if (imgAdblockShield != null) imgAdblockShield.setImageResource(R.drawable.ic_shield);
            if (tvAdblockTitle != null) tvAdblockTitle.setText("Lihon AdBlock включён");
            if (tvAdblockStats != null) {
                tvAdblockStats.setText("Блокирует баннеры, трекеры и рекламу");
                tvAdblockStats.setTextColor(Color.parseColor("#80E89E"));
            }
            if (tvAdblockPill != null) {
                tvAdblockPill.setText("ВКЛ");
                tvAdblockPill.setTextColor(Color.parseColor("#80E89E"));
            }
        } else {
            cardAdblockStatus.setBackgroundResource(R.drawable.bg_card_adblock_off);
            if (imgAdblockShield != null) imgAdblockShield.setImageResource(R.drawable.ic_shield_off);
            if (tvAdblockTitle != null) tvAdblockTitle.setText("Lihon AdBlock выключен");
            if (tvAdblockStats != null) {
                tvAdblockStats.setText("Фильтрация рекламы приостановлена");
                tvAdblockStats.setTextColor(Color.parseColor("#CAC4D0"));
            }
            if (tvAdblockPill != null) {
                tvAdblockPill.setText("ВЫКЛ");
                tvAdblockPill.setTextColor(Color.parseColor("#CAC4D0"));
            }
        }
    }

    // =========================================================================
    // EVENT LISTENERS & NAVIGATION
    // =========================================================================
    private void setupListeners() {
        // Omnibox Click -> Open Search Overlay
        findViewById(R.id.omnibox_capsule).setOnClickListener(v -> openSearchOverlay());
        findViewById(R.id.big_search_bar).setOnClickListener(v -> openSearchOverlay());

        // Engine Selector Click
        findViewById(R.id.btn_engine_selector).setOnClickListener(v -> showEngineSelector(v));

        // Incognito Button
        findViewById(R.id.btn_toggle_incognito).setOnClickListener(v -> toggleIncognitoMode());

        // Incognito Banner Exit Button
        View btnBannerExit = findViewById(R.id.btn_banner_exit_incognito);
        if (btnBannerExit != null) {
            btnBannerExit.setOnClickListener(v -> destroyAllIncognitoTabs(true));
        }

        // Quick Sites Shortcuts
        View siteYt = findViewById(R.id.site_youtube);
        if (siteYt != null) siteYt.setOnClickListener(v -> loadUrl("https://m.youtube.com"));
        View siteTg = findViewById(R.id.site_telegram);
        if (siteTg != null) siteTg.setOnClickListener(v -> loadUrl("https://web.telegram.org"));
        View siteVk = findViewById(R.id.site_vk);
        if (siteVk != null) siteVk.setOnClickListener(v -> loadUrl("https://m.vk.com"));
        View siteGh = findViewById(R.id.site_github);
        if (siteGh != null) siteGh.setOnClickListener(v -> loadUrl("https://github.com"));
        View siteWiki = findViewById(R.id.site_wikipedia);
        if (siteWiki != null) siteWiki.setOnClickListener(v -> loadUrl("https://ru.wikipedia.org"));
        View siteReddit = findViewById(R.id.site_reddit);
        if (siteReddit != null) siteReddit.setOnClickListener(v -> loadUrl("https://reddit.com"));
        View siteLihont = findViewById(R.id.site_lihont);
        if (siteLihont != null) siteLihont.setOnClickListener(v -> loadUrl("https://lihont.space"));
        View siteTt = findViewById(R.id.site_tiktok);
        if (siteTt != null) siteTt.setOnClickListener(v -> loadUrl("https://www.tiktok.com"));

        // Close Search Overlay
        findViewById(R.id.btn_close_search).setOnClickListener(v -> closeSearchOverlay());

        // Tabs Button -> Open Real Tabs Dialog
        View.OnClickListener tabsClick = v -> showTabsDialog();
        findViewById(R.id.btn_tabs).setOnClickListener(tabsClick);
        tvTabCount.setOnClickListener(tabsClick);

        // More Menu
        findViewById(R.id.btn_more).setOnClickListener(v -> showMoreMenu(v));

        // Search Input Action (Enter / Go)
        editSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitQuery(editSearchInput.getText().toString());
                return true;
            }
            return false;
        });

        // Quick Engine Buttons in overlay
        findViewById(R.id.btn_engine_google).setOnClickListener(v -> searchWithEngine("https://www.google.com/search?q="));
        findViewById(R.id.btn_engine_yandex).setOnClickListener(v -> searchWithEngine("https://ya.ru/search/?text="));
        findViewById(R.id.btn_engine_duck).setOnClickListener(v -> searchWithEngine("https://duckduckgo.com/?q="));
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String str = data.toString().toLowerCase();
                if (str.endsWith(".lbp") || (intent.getType() != null && intent.getType().contains("lbp"))) {
                    installPluginFromUri(data);
                    return;
                }
                loadUrl(data.toString());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public void loadUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        url = url.trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = currentEngineUrl + Uri.encode(url);
            }
        }

        closeSearchOverlay();
        startPageView.setVisibility(View.GONE);

        Tab cur = tabs.get(currentTabIndex);
        cur.setUrl(url);
        cur.getWebView().setVisibility(View.VISIBLE);
        cur.getWebView().loadUrl(url);
    }

    private void submitQuery(String query) {
        if (query != null && !query.trim().isEmpty()) {
            loadUrl(query);
        }
    }

    private void searchWithEngine(String engineBase) {
        String query = editSearchInput.getText().toString().trim();
        if (!query.isEmpty()) {
            loadUrl(engineBase + Uri.encode(query));
        } else {
            loadUrl(engineBase.split("\\?")[0]);
        }
    }

    public void showStartPage() {
        Tab cur = tabs.get(currentTabIndex);
        cur.getWebView().setVisibility(View.GONE);
        startPageView.setVisibility(View.VISIBLE);
        tvUrlDisplay.setText("Поиск или веб-адрес");
        updateGreeting();
        PluginManager.getInstance().dispatchHomeScreenCreated(this, startPageView);
    }

    private void handleBackAction() {
        if (searchOverlay.getVisibility() == View.VISIBLE) {
            closeSearchOverlay();
            return;
        }

        Tab cur = tabs.get(currentTabIndex);
        WebView wv = cur.getWebView();
        if (wv.getVisibility() == View.VISIBLE && wv.canGoBack()) {
            wv.goBack();
        } else if (wv.getVisibility() == View.VISIBLE) {
            showStartPage();
        } else if (tabs.size() > 1) {
            closeTab(currentTabIndex);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackAction();
    }

    private void openSearchOverlay() {
        searchOverlay.setVisibility(View.VISIBLE);
        Tab cur = tabs.get(currentTabIndex);
        String current = cur.getWebView().getUrl();
        editSearchInput.setText(current != null ? current : "");
        editSearchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editSearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void closeSearchOverlay() {
        searchOverlay.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editSearchInput.getWindowToken(), 0);
        }
    }

    private void showMoreMenu(View anchor) {
        Md3MenuDialog.show(this);
    }

    public void reloadCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            Tab cur = tabs.get(currentTabIndex);
            if (cur != null && cur.getWebView() != null) {
                cur.getWebView().reload();
            }
        }
    }

    public boolean isDesktopMode() {
        return isDesktopMode;
    }

    public boolean isIncognito() {
        return isIncognito;
    }

    public String getCurrentUserAgentString() {
        WebView cur = getCurrentWebView();
        if (cur != null && cur.getSettings() != null) {
            return cur.getSettings().getUserAgentString();
        }
        if (defaultUserAgent != null) {
            return defaultUserAgent;
        }
        return BrowserSettingsDialog.UA_ANDROID;
    }

    public void applyCustomUserAgent(String ua) {
        SharedPreferences prefs = getSharedPreferences(BrowserSettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(BrowserSettingsDialog.KEY_USER_AGENT, ua).apply();
        for (Tab tab : tabs) {
            if (tab.getWebView() != null) {
                tab.getWebView().getSettings().setUserAgentString(ua);
            }
        }
        WebView active = getCurrentWebView();
        if (active != null && active.getUrl() != null && !active.getUrl().isEmpty()) {
            active.reload();
        }
    }



    private static final int RC_PICK_PLUGIN = 1001;

    public void showPluginsManager() {
        PluginsDialog.show(this, this::openPluginFilePicker);
    }

    private void openPluginFilePicker() {
        java.util.List<java.io.File> candidates = new java.util.ArrayList<>();

        // 1. Check external files dir
        try {
            java.io.File extDir = getExternalFilesDir(null);
            if (extDir != null && extDir.exists()) {
                java.io.File[] files = extDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".lbp"));
                if (files != null) candidates.addAll(java.util.Arrays.asList(files));
            }
        } catch (Exception ignored) {}

        // 2. Check DIRECTORY_DOWNLOADS
        try {
            java.io.File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (downloadDir != null && downloadDir.exists()) {
                java.io.File[] files = downloadDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".lbp"));
                if (files != null) candidates.addAll(java.util.Arrays.asList(files));
            }
        } catch (Exception ignored) {}

        // 3. Check /sdcard/Download
        try {
            java.io.File directDl = new java.io.File("/sdcard/Download");
            if (directDl.exists()) {
                java.io.File[] files = directDl.listFiles((dir, name) -> name.toLowerCase().endsWith(".lbp"));
                if (files != null) {
                    for (java.io.File f : files) {
                        boolean exists = false;
                        for (java.io.File c : candidates) {
                            if (c.getAbsolutePath().equals(f.getAbsolutePath())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) candidates.add(f);
                    }
                }
            }
        } catch (Exception ignored) {}

        java.util.List<String> options = new java.util.ArrayList<>();
        for (java.io.File f : candidates) {
            options.add(f.getName());
        }
        options.add("Выбрать через системный проводник...");

        new AlertDialog.Builder(this)
            .setTitle("Установка плагина (.lbp)")
            .setItems(options.toArray(new String[0]), (dialog, which) -> {
                if (which < candidates.size()) {
                    java.io.File target = candidates.get(which);
                    boolean ok = PluginManager.getInstance().installPluginFromFile(target);
                    if (ok) {
                        Toast.makeText(this, "Плагин " + target.getName() + " успешно установлен!", Toast.LENGTH_SHORT).show();
                        showStartPage();
                        showPluginsManager();
                    } else {
                        Toast.makeText(this, "Ошибка установки: повреждённый .lbp", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    launchSystemFilePicker();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void launchSystemFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Выберите плагин (.lbp)"), RC_PICK_PLUGIN);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть проводник: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void installPluginFromUri(Uri uri) {
        if (uri == null) return;
        try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
            boolean ok = PluginManager.getInstance().installPlugin(is);
            if (ok) {
                Toast.makeText(this, "Плагин успешно установлен!", Toast.LENGTH_SHORT).show();
                showStartPage();
                showPluginsManager();
            } else {
                Toast.makeText(this, "Ошибка установки: повреждённый .lbp", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка установки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_PLUGIN && resultCode == RESULT_OK && data != null && data.getData() != null) {
            installPluginFromUri(data.getData());
        }
    }

    public void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        Tab cur = tabs.get(currentTabIndex);
        WebSettings settings = cur.getWebView().getSettings();
        if (isDesktopMode) {
            settings.setUserAgentString(BrowserSettingsDialog.UA_DESKTOP);
        } else {
            SharedPreferences prefs = getSharedPreferences(BrowserSettingsDialog.PREFS_NAME, Context.MODE_PRIVATE);
            String savedUa = prefs.getString(BrowserSettingsDialog.KEY_USER_AGENT, null);
            settings.setUserAgentString(savedUa != null && !savedUa.isEmpty() ? savedUa : defaultUserAgent);
        }
        cur.getWebView().reload();
        Toast.makeText(this, isDesktopMode ? "Режим ПК активирован" : "Мобильный режим", Toast.LENGTH_SHORT).show();
    }

    public void clearBrowserData() {
        for (Tab t : tabs) {
            t.getWebView().clearCache(true);
            t.getWebView().clearHistory();
        }
        CookieManager.getInstance().removeAllCookies(null);
        AdBlocker.resetCount();
        updateAdBlockVisuals();
        Toast.makeText(this, "Кэш и данные сессий очищены", Toast.LENGTH_SHORT).show();
    }

    public void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("LihonB 1.0.0-beta")
            .setMessage("Фирменный Material Design 3 Expressive веб-браузер экосистемы Lihon.\n\n" +
                        "• Движок: Google Android WebView\n" +
                        "• Встроенный Lihon AdBlocker\n" +
                        "• Мульти-вкладки и режим Инкогнито (Wipe-on-Exit)\n" +
                        "• Динамические плагины .LBP\n" +
                        "• Настройка User-Agent и быстрой кнопки\n" +
                        "• 100% векторная графика SVG\n\n" +
                        "Разработано для lihont.space")
            .setPositiveButton("Отлично", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        destroyAllIncognitoTabs(false);
        super.onDestroy();
    }
}
