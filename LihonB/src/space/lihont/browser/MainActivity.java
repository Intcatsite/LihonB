package space.lihont.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
        setContentView(R.layout.activity_main);

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

        cardAdblockStatus = findViewById(R.id.card_adblock_status);
        imgAdblockShield = findViewById(R.id.img_adblock_shield);
        tvAdblockTitle = findViewById(R.id.tv_adblock_title);
        tvAdblockStats = findViewById(R.id.tv_adblock_stats);
        tvAdblockPill = findViewById(R.id.tv_adblock_pill);

        tvIncognitoText = findViewById(R.id.tv_incognito_text);
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
        }

        isIncognito = active.isIncognito();
        updateIncognitoBanner(isIncognito);
        if (isIncognito) {
            tvIncognitoText.setText("✕ Закрыть Инкогнито (стереть всё)");
            imgIncognitoIcon.setImageResource(R.drawable.ic_close);
            imgOmniboxStatus.setImageResource(R.drawable.ic_incognito);
        } else {
            tvIncognitoText.setText("Перейти в режим Инкогнито");
            imgIncognitoIcon.setImageResource(R.drawable.ic_incognito);
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
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Google" + ("Google".equals(currentEngine) ? " ✓" : ""));
        popup.getMenu().add(0, 2, 1, "Yandex" + ("Yandex".equals(currentEngine) ? " ✓" : ""));
        popup.getMenu().add(0, 3, 2, "DuckDuckGo" + ("DuckDuckGo".equals(currentEngine) ? " ✓" : ""));

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        applyEngine("Google");
                        return true;
                    case 2:
                        applyEngine("Yandex");
                        return true;
                    case 3:
                        applyEngine("DuckDuckGo");
                        return true;
                }
                return false;
            }
        });
        popup.show();
    }

    // =========================================================================
    // INCOGNITO MODE (FULL DATA PURGE & TAB DESTRUCTION)
    // =========================================================================
    private void toggleIncognitoMode() {
        if (!isIncognito) {
            isIncognito = true;
            updateIncognitoBanner(true);
            tvIncognitoText.setText("✕ Закрыть Инкогнито (стереть всё)");
            imgIncognitoIcon.setImageResource(R.drawable.ic_close);
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
        tvIncognitoText.setText("Перейти в режим Инкогнито");
        imgIncognitoIcon.setImageResource(R.drawable.ic_incognito);
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
    private void toggleAdBlock() {
        boolean nextState = !AdBlocker.isEnabled();
        AdBlocker.setEnabled(nextState);
        updateAdBlockVisuals();
        Toast.makeText(this, nextState ? "AdBlock включён" : "AdBlock выключен", Toast.LENGTH_SHORT).show();
    }

    private void updateAdBlockVisuals() {
        boolean on = AdBlocker.isEnabled();
        if (on) {
            cardAdblockStatus.setBackgroundResource(R.drawable.bg_card_adblock_on);
            imgAdblockShield.setImageResource(R.drawable.ic_shield);
            tvAdblockTitle.setText("Lihon AdBlock включён");
            tvAdblockStats.setText("Блокирует баннеры, трекеры и рекламу");
            tvAdblockStats.setTextColor(Color.parseColor("#80E89E"));
            tvAdblockPill.setText("ВКЛ");
            tvAdblockPill.setTextColor(Color.parseColor("#80E89E"));
        } else {
            cardAdblockStatus.setBackgroundResource(R.drawable.bg_card_adblock_off);
            imgAdblockShield.setImageResource(R.drawable.ic_shield_off);
            tvAdblockTitle.setText("Lihon AdBlock выключен");
            tvAdblockStats.setText("Фильтрация рекламы приостановлена");
            tvAdblockStats.setTextColor(Color.parseColor("#CAC4D0"));
            tvAdblockPill.setText("ВЫКЛ");
            tvAdblockPill.setTextColor(Color.parseColor("#CAC4D0"));
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

        // Close Search Overlay
        findViewById(R.id.btn_close_search).setOnClickListener(v -> closeSearchOverlay());

        // Back Button
        findViewById(R.id.btn_back).setOnClickListener(v -> handleBackAction());

        // Home Button
        findViewById(R.id.btn_home).setOnClickListener(v -> showStartPage());

        // Tabs Button -> Open Real Tabs Dialog
        View.OnClickListener tabsClick = v -> showTabsDialog();
        findViewById(R.id.btn_tabs).setOnClickListener(tabsClick);
        tvTabCount.setOnClickListener(tabsClick);

        // More Menu
        findViewById(R.id.btn_more).setOnClickListener(v -> showMoreMenu(v));

        // AdBlock Card Click
        cardAdblockStatus.setOnClickListener(v -> toggleAdBlock());

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
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, isDesktopMode ? "✓ Версия для ПК" : "Версия для ПК");
        popup.getMenu().add(0, 2, 1, isIncognito ? "✓ Режим Инкогнито" : "Режим Инкогнито");
        popup.getMenu().add(0, 3, 2, AdBlocker.isEnabled() ? "✓ Lihon AdBlock" : "Lihon AdBlock");
        popup.getMenu().add(0, 7, 3, "🧩 Плагины (.lbp)");
        popup.getMenu().add(0, 4, 4, "Обновить страницу");
        popup.getMenu().add(0, 5, 5, "Очистить кэш и cookies");
        popup.getMenu().add(0, 6, 6, "О браузере LihonB");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    toggleDesktopMode();
                    return true;
                case 2:
                    toggleIncognitoMode();
                    return true;
                case 3:
                    toggleAdBlock();
                    return true;
                case 7:
                    showPluginsManager();
                    return true;
                case 4:
                    tabs.get(currentTabIndex).getWebView().reload();
                    return true;
                case 5:
                    clearBrowserData();
                    return true;
                case 6:
                    showAboutDialog();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private static final int RC_PICK_PLUGIN = 1001;

    private void showPluginsManager() {
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
            options.add("📦 " + f.getName());
        }
        options.add("📁 Выбрать через системный проводник...");

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

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        Tab cur = tabs.get(currentTabIndex);
        WebSettings settings = cur.getWebView().getSettings();
        if (isDesktopMode) {
            settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        } else {
            settings.setUserAgentString(defaultUserAgent);
        }
        cur.getWebView().reload();
        Toast.makeText(this, isDesktopMode ? "Режим ПК активирован" : "Мобильный режим", Toast.LENGTH_SHORT).show();
    }

    private void clearBrowserData() {
        for (Tab t : tabs) {
            t.getWebView().clearCache(true);
            t.getWebView().clearHistory();
        }
        CookieManager.getInstance().removeAllCookies(null);
        AdBlocker.resetCount();
        updateAdBlockVisuals();
        Toast.makeText(this, "Кэш и данные сессий очищены", Toast.LENGTH_SHORT).show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("LihonB 1.0.0-beta")
            .setMessage("Фирменный Material Design 3 Expressive веб-браузер экосистемы Lihon.\n\n" +
                        "• Движок: Google Android WebView\n" +
                        "• Встроенный Lihon AdBlocker\n" +
                        "• Мульти-вкладки и режим Инкогнито\n" +
                        "• Выбор поисковика (Google, Yandex, DuckDuckGo)\n" +
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
