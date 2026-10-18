package space.lihont.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Material Design 3 Expressive Browser Settings Dialog for LihonB.
 * Allows configuring:
 * 1. User-Agent string (Presets: Android Mobile, Desktop Chrome, iOS Safari, or Custom text).
 * 2. App Launcher Icon (Choose from 9 LihonB SVG and PNG styles).
 */
public class BrowserSettingsDialog {

    public static final String PREFS_NAME = "lihon_browser_settings";
    public static final String KEY_USER_AGENT = "pref_user_agent";

    // Standard UA Presets
    public static final String UA_ANDROID = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36";
    public static final String UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
    public static final String UA_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";

    public static void show(MainActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ScrollView scroll = new ScrollView(activity);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_menu_sheet);
        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        builder.setView(scroll);
        AlertDialog dialog = builder.create();

        // 1. Header
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, (int) (14 * activity.getResources().getDisplayMetrics().density));

        ImageView icon = new ImageView(activity);
        int isz = (int) (32 * activity.getResources().getDisplayMetrics().density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(isz, isz));
        icon.setImageResource(R.drawable.ic_settings);
        header.addView(icon);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tclp.setMargins((int) (12 * activity.getResources().getDisplayMetrics().density), 0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0);
        titleCol.setLayoutParams(tclp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Настройки LihonB");
        tvTitle.setTextColor(Color.parseColor("#E6E0E9"));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("User-Agent и иконка приложения");
        tvSub.setTextColor(Color.parseColor("#938F99"));
        tvSub.setTextSize(12);
        titleCol.addView(tvSub);
        header.addView(titleCol);

        ImageView btnClose = new ImageView(activity);
        int csz = (int) (30 * activity.getResources().getDisplayMetrics().density);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(csz, csz));
        btnClose.setImageResource(R.drawable.ic_close);
        btnClose.setBackgroundResource(R.drawable.bg_pill);
        int cp = (int) (6 * activity.getResources().getDisplayMetrics().density);
        btnClose.setPadding(cp, cp, cp, cp);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        header.addView(btnClose);

        root.addView(header);

        // ====================================================================
        // SECTION 1: USER-AGENT SETTINGS
        // ====================================================================
        // ====================================================================
        // SECTION 1: USER-AGENT SETTINGS
        // ====================================================================
        addSectionHeader(activity, root, "ПОЛЬЗОВАТЕЛЬСКИЙ АГЕНТ (USER-AGENT)");

        TextView tvUaDesc = new TextView(activity);
        tvUaDesc.setText("Задаёт идентификатор браузера для сайтов:");
        tvUaDesc.setTextColor(Color.parseColor("#CAC4D0"));
        tvUaDesc.setTextSize(13);
        int mb8 = (int) (8 * activity.getResources().getDisplayMetrics().density);
        tvUaDesc.setPadding(0, 0, 0, mb8);
        root.addView(tvUaDesc);

        String currentUa = activity.getCurrentUserAgentString();

        // Preset 1: Android Chrome
        addUaPresetRow(activity, root, "Android Chrome (По умолчанию)", "Pixel 8, Android 14, Chrome 128",
            currentUa.contains("Android") && !currentUa.contains("Windows") && !currentUa.contains("iPhone"),
            v -> {
                activity.applyCustomUserAgent(UA_ANDROID);
                dialog.dismiss();
                show(activity);
            });

        // Preset 2: Desktop Windows Chrome
        addUaPresetRow(activity, root, "Desktop Chrome (Windows)", "Windows 11 x64, полные версии сайтов",
            currentUa.contains("Windows NT"),
            v -> {
                activity.applyCustomUserAgent(UA_DESKTOP);
                dialog.dismiss();
                show(activity);
            });

        // Preset 3: Apple iPhone iOS Safari
        addUaPresetRow(activity, root, "Apple iPhone (Safari iOS)", "iPhone 15 Pro, iOS 17.5 Safari",
            currentUa.contains("iPhone"),
            v -> {
                activity.applyCustomUserAgent(UA_IOS);
                dialog.dismiss();
                show(activity);
            });

        // Custom UA Input Box
        LinearLayout customBox = new LinearLayout(activity);
        customBox.setOrientation(LinearLayout.VERTICAL);
        customBox.setBackgroundResource(R.drawable.bg_menu_item);
        int cbPad = (int) (12 * activity.getResources().getDisplayMetrics().density);
        customBox.setPadding(cbPad, cbPad, cbPad, cbPad);
        LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cblp.setMargins(0, 0, 0, (int) (16 * activity.getResources().getDisplayMetrics().density));
        customBox.setLayoutParams(cblp);

        TextView tvCustomTitle = new TextView(activity);
        tvCustomTitle.setText("Кастомный User-Agent:");
        tvCustomTitle.setTextColor(Color.parseColor("#E6E0E9"));
        tvCustomTitle.setTextSize(14);
        tvCustomTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        customBox.addView(tvCustomTitle);

        EditText etCustomUa = new EditText(activity);
        etCustomUa.setText(currentUa);
        etCustomUa.setBackgroundResource(R.drawable.bg_omnibox);
        etCustomUa.setTextColor(Color.parseColor("#D0BCFF"));
        etCustomUa.setTextSize(12);
        int etPad = (int) (10 * activity.getResources().getDisplayMetrics().density);
        etCustomUa.setPadding(etPad, etPad, etPad, etPad);
        etCustomUa.setMaxLines(3);
        LinearLayout.LayoutParams etlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etlp.setMargins(0, mb8, 0, mb8);
        customBox.addView(etCustomUa);

        Button btnApplyCustom = new Button(activity);
        btnApplyCustom.setText("Применить свой User-Agent");
        btnApplyCustom.setTextColor(Color.parseColor("#381E72"));
        btnApplyCustom.setBackgroundResource(R.drawable.bg_pill);
        btnApplyCustom.setOnClickListener(v -> {
            String newUa = etCustomUa.getText().toString().trim();
            if (!newUa.isEmpty()) {
                activity.applyCustomUserAgent(newUa);
                dialog.dismiss();
                Toast.makeText(activity, "Кастомный User-Agent применён", Toast.LENGTH_SHORT).show();
            }
        });
        customBox.addView(btnApplyCustom);

        root.addView(customBox);

        // ====================================================================
        // SECTION 2: APP LAUNCHER ICON SELECTION (DYNAMIC APP ICON)
        // ====================================================================
        addSectionHeader(activity, root, "ИКОНКА ПРИЛОЖЕНИЯ (НА РАБОЧЕМ СТОЛЕ)");

        TextView tvIconDesc = new TextView(activity);
        tvIconDesc.setText("Выберите внешний вид иконки LihonB на рабочем столе телефона:");
        tvIconDesc.setTextColor(Color.parseColor("#CAC4D0"));
        tvIconDesc.setTextSize(13);
        tvIconDesc.setPadding(0, 0, 0, mb8);
        root.addView(tvIconDesc);

        String activeIcon = AppIconManager.getActiveAppIcon(activity);

        // Icon 1: LihonB Pink (Буква B)
        addAppIconRow(activity, root, R.drawable.ic_icon_pink, "LihonB Pink (Векторный B)", "Розовый фон, лавандовый цветок, литера B",
            AppIconManager.ALIAS_PINK.equals(activeIcon), v -> {
                AppIconManager.setAppIcon(activity, AppIconManager.ALIAS_PINK);
                dialog.dismiss();
                Toast.makeText(activity, "Иконка приложения: LihonB Pink. Обновится на рабочем столе", Toast.LENGTH_SHORT).show();
                show(activity);
            });


        // Icon 6: Pastel Pink
        addAppIconRow(activity, root, R.drawable.ic_icon_pastel, "LihonB Pastel (Нежно-розовый)", "Пудрово-розовая пастельная палитра",
            AppIconManager.ALIAS_PASTEL.equals(activeIcon), v -> {
                AppIconManager.setAppIcon(activity, AppIconManager.ALIAS_PASTEL);
                dialog.dismiss();
                Toast.makeText(activity, "Иконка приложения: Нежный пастель. Обновится на рабочем столе", Toast.LENGTH_SHORT).show();
                show(activity);
            });

        // Icon 7: Flat Vibrant Pink
        addAppIconRow(activity, root, R.drawable.ic_icon_flat, "LihonB Flat (Контрастный)", "Контрастный стиль с белым цветком",
            AppIconManager.ALIAS_FLAT.equals(activeIcon), v -> {
                AppIconManager.setAppIcon(activity, AppIconManager.ALIAS_FLAT);
                dialog.dismiss();
                Toast.makeText(activity, "Иконка приложения: Контрастный Flat. Обновится на рабочем столе", Toast.LENGTH_SHORT).show();
                show(activity);
            });

        // Icon 8: LB Monogram
        addAppIconRow(activity, root, R.drawable.ic_icon_lb, "LihonB Monogram (LB)", "Розовый фон с монограммой LB",
            AppIconManager.ALIAS_LB.equals(activeIcon), v -> {
                AppIconManager.setAppIcon(activity, AppIconManager.ALIAS_LB);
                dialog.dismiss();
                Toast.makeText(activity, "Иконка приложения: Монограмма LB. Обновится на рабочем столе", Toast.LENGTH_SHORT).show();
                show(activity);
            });

        // Icon 9: Classic Lavender (Original)
        addAppIconRow(activity, root, R.drawable.ic_icon_original, "LihonB Original (Оригинал)", "Лавандовый цветок, темная тема",
            AppIconManager.ALIAS_ORIGINAL.equals(activeIcon), v -> {
                AppIconManager.setAppIcon(activity, AppIconManager.ALIAS_ORIGINAL);
                dialog.dismiss();
                Toast.makeText(activity, "Иконка приложения: Классический LihonB. Обновится на рабочем столе", Toast.LENGTH_SHORT).show();
                show(activity);
            });

        // Bottom Done Button
        Button btnDone = new Button(activity);
        btnDone.setText("Готово");
        btnDone.setTextColor(Color.parseColor("#381E72"));
        btnDone.setBackgroundResource(R.drawable.bg_pill);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * activity.getResources().getDisplayMetrics().density)
        );
        dlp.setMargins(0, (int) (12 * activity.getResources().getDisplayMetrics().density), 0, 0);
        btnDone.setLayoutParams(dlp);
        btnDone.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnDone);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private static void addSectionHeader(Activity activity, LinearLayout container, String title) {
        TextView tv = new TextView(activity);
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#D0BCFF"));
        tv.setTextSize(12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        int vPad = (int) (12 * activity.getResources().getDisplayMetrics().density);
        tv.setPadding(0, vPad, 0, (int) (6 * activity.getResources().getDisplayMetrics().density));
        container.addView(tv);
    }

    private static void addUaPresetRow(Activity activity, LinearLayout container, String title, String subtitle,
                                       boolean isSelected, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_menu_item);
        row.setClickable(true);
        row.setFocusable(true);

        int padH = (int) (14 * activity.getResources().getDisplayMetrics().density);
        int padV = (int) (10 * activity.getResources().getDisplayMetrics().density);
        row.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 0, 0, (int) (6 * activity.getResources().getDisplayMetrics().density));
        row.setLayoutParams(rlp);

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(tlp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor(isSelected ? "#D0BCFF" : "#E6E0E9"));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText(subtitle);
        tvSub.setTextColor(Color.parseColor("#938F99"));
        tvSub.setTextSize(11);
        textCol.addView(tvSub);

        row.addView(textCol);

        if (isSelected) {
            TextView tvActive = new TextView(activity);
            tvActive.setText("АКТИВЕН ✓");
            tvActive.setTextColor(Color.parseColor("#81C784"));
            tvActive.setTextSize(11);
            tvActive.setTypeface(null, android.graphics.Typeface.BOLD);
            tvActive.setBackgroundResource(R.drawable.bg_card_adblock_on);
            int ph = (int) (8 * activity.getResources().getDisplayMetrics().density);
            int pv = (int) (3 * activity.getResources().getDisplayMetrics().density);
            tvActive.setPadding(ph, pv, ph, pv);
            row.addView(tvActive);
        }

        row.setOnClickListener(onClick);
        container.addView(row);
    }



    private static void addAppIconRow(Activity activity, LinearLayout container, int iconRes,
                                      String title, String subtitle, boolean isSelected,
                                      View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_menu_item);
        row.setClickable(true);
        row.setFocusable(true);

        int padH = (int) (14 * activity.getResources().getDisplayMetrics().density);
        int padV = (int) (10 * activity.getResources().getDisplayMetrics().density);
        row.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 0, 0, (int) (6 * activity.getResources().getDisplayMetrics().density));
        row.setLayoutParams(rlp);

        // App Icon Thumbnail
        ImageView img = new ImageView(activity);
        int isz = (int) (38 * activity.getResources().getDisplayMetrics().density);
        img.setLayoutParams(new LinearLayout.LayoutParams(isz, isz));
        img.setImageResource(iconRes);
        row.addView(img);

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tlp.setMargins((int) (14 * activity.getResources().getDisplayMetrics().density), 0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0);
        textCol.setLayoutParams(tlp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor(isSelected ? "#D0BCFF" : "#E6E0E9"));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText(subtitle);
        tvSub.setTextColor(Color.parseColor("#938F99"));
        tvSub.setTextSize(11);
        textCol.addView(tvSub);
        row.addView(textCol);

        if (isSelected) {
            TextView tvActive = new TextView(activity);
            tvActive.setText("АКТИВНА ✓");
            tvActive.setTextColor(Color.parseColor("#81C784"));
            tvActive.setTextSize(11);
            tvActive.setTypeface(null, android.graphics.Typeface.BOLD);
            tvActive.setBackgroundResource(R.drawable.bg_card_adblock_on);
            int ph = (int) (8 * activity.getResources().getDisplayMetrics().density);
            int pv = (int) (3 * activity.getResources().getDisplayMetrics().density);
            tvActive.setPadding(ph, pv, ph, pv);
            row.addView(tvActive);
        }

        row.setOnClickListener(onClick);
        container.addView(row);
    }
}
