package space.lihont.browser;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import space.lihont.browser.plugin.PluginManager;

/**
 * Compact floating bottom card menu for LihonB (3-dots menu).
 * Clean single-line items, anchored to bottom, no marketing subtitles.
 */
public class Md3MenuDialog {

    public static void show(MainActivity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        // Outer wrapper to give card floating margins
        FrameLayout wrapper = new FrameLayout(activity);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        int marginH = (int) (14 * activity.getResources().getDisplayMetrics().density);
        int marginB = (int) (10 * activity.getResources().getDisplayMetrics().density);
        wrapper.setPadding(marginH, 0, marginH, marginB);

        // Menu Card
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_menu_sheet);
        int padH = (int) (16 * activity.getResources().getDisplayMetrics().density);
        int padV = (int) (14 * activity.getResources().getDisplayMetrics().density);
        card.setPadding(padH, padV, padH, padV);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(clp);
        wrapper.addView(card);

        // 1. Header: "Меню" + Close button
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, (int) (10 * activity.getResources().getDisplayMetrics().density));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Меню");
        tvTitle.setTextColor(Color.parseColor("#E6E0E9"));
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvTitle.setLayoutParams(tlp);
        header.addView(tvTitle);

        ImageView btnClose = new ImageView(activity);
        int closeSize = (int) (28 * activity.getResources().getDisplayMetrics().density);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(closeSize, closeSize));
        btnClose.setImageResource(R.drawable.ic_close);
        btnClose.setBackgroundResource(R.drawable.bg_pill);
        int cPad = (int) (6 * activity.getResources().getDisplayMetrics().density);
        btnClose.setPadding(cPad, cPad, cPad, cPad);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        header.addView(btnClose);

        card.addView(header);

        // Divider
        View divider = new View(activity);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        dlp.setMargins(0, 0, 0, (int) (8 * activity.getResources().getDisplayMetrics().density));
        divider.setLayoutParams(dlp);
        divider.setBackgroundColor(Color.parseColor("#36343B"));
        card.addView(divider);

        // ScrollView for items
        ScrollView scroll = new ScrollView(activity);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout itemsList = new LinearLayout(activity);
        itemsList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(itemsList);
        card.addView(scroll);

        // 2. Menu Items (Clean single-line, NO subtitles)
        // Reload
        addMenuItem(activity, itemsList, R.drawable.ic_refresh, "Обновить", null, false, v -> {
            dialog.dismiss();
            activity.reloadCurrentTab();
        });

        // Desktop Mode
        boolean isDesktop = activity.isDesktopMode();
        addMenuItem(activity, itemsList, R.drawable.ic_desktop, "Версия для ПК", isDesktop ? "ВКЛ" : "ВЫКЛ", isDesktop, v -> {
            dialog.dismiss();
            activity.toggleDesktopMode();
        });

        // Incognito Mode
        boolean isIncognito = activity.isIncognito();
        addMenuItem(activity, itemsList, R.drawable.ic_incognito, "Режим Инкогнито", isIncognito ? "ВКЛ" : "ВЫКЛ", isIncognito, v -> {
            dialog.dismiss();
            activity.toggleIncognitoMode();
        });

        // Lihon AdBlock
        boolean isAdBlock = AdBlocker.isEnabled();
        addMenuItem(activity, itemsList, R.drawable.ic_shield, "Lihon AdBlock", isAdBlock ? "ВКЛ" : "ВЫКЛ", isAdBlock, v -> {
            dialog.dismiss();
            activity.toggleAdBlock();
        });

        // Plugins (.lbp)
        int pluginCount = PluginManager.getInstance().getLoadedPlugins().size();
        String pPill = pluginCount > 0 ? (pluginCount + " плаг.") : "0";
        addMenuItem(activity, itemsList, R.drawable.ic_extension, "Плагины (.lbp)", pPill, pluginCount > 0, v -> {
            dialog.dismiss();
            activity.showPluginsManager();
        });

        // Settings
        addMenuItem(activity, itemsList, R.drawable.ic_settings, "Настройки браузера", null, false, v -> {
            dialog.dismiss();
            BrowserSettingsDialog.show(activity);
        });

        // Clear Cache
        addMenuItem(activity, itemsList, R.drawable.ic_trash, "Очистить кэш", null, false, v -> {
            dialog.dismiss();
            activity.clearBrowserData();
        });

        // About LihonB (NON-CLICKABLE: no action, no popup)
        addMenuItem(activity, itemsList, R.drawable.ic_globe, "О браузере LihonB", "v1.0", false, null);

        dialog.setContentView(wrapper);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setDimAmount(0.40f);
            window.setAttributes(lp);
        }

        dialog.show();
    }

    private static void addMenuItem(Activity activity, LinearLayout container, int iconRes,
                                    String title, String pillText, boolean isPillActive,
                                    View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int padH = (int) (12 * activity.getResources().getDisplayMetrics().density);
        int padV = (int) (10 * activity.getResources().getDisplayMetrics().density);
        row.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rlp.setMargins(0, 0, 0, (int) (4 * activity.getResources().getDisplayMetrics().density));
        row.setLayoutParams(rlp);

        if (onClick != null) {
            row.setBackgroundResource(R.drawable.bg_menu_item);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(onClick);
        } else {
            // Non-clickable item
            row.setBackgroundColor(Color.TRANSPARENT);
            row.setClickable(false);
            row.setFocusable(false);
        }

        // Icon
        ImageView img = new ImageView(activity);
        int isz = (int) (20 * activity.getResources().getDisplayMetrics().density);
        img.setLayoutParams(new LinearLayout.LayoutParams(isz, isz));
        img.setImageResource(iconRes);
        row.addView(img);

        // Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(onClick != null ? Color.parseColor("#E6E0E9") : Color.parseColor("#938F99"));
        tvTitle.setTextSize(14);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tlp.setMargins((int) (12 * activity.getResources().getDisplayMetrics().density), 0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0);
        tvTitle.setLayoutParams(tlp);
        row.addView(tvTitle);

        // Status Pill (Optional)
        if (pillText != null) {
            TextView tvPill = new TextView(activity);
            tvPill.setText(pillText);
            tvPill.setTextSize(11);
            tvPill.setTypeface(null, android.graphics.Typeface.BOLD);
            int pPadH = (int) (8 * activity.getResources().getDisplayMetrics().density);
            int pPadV = (int) (3 * activity.getResources().getDisplayMetrics().density);
            tvPill.setPadding(pPadH, pPadV, pPadH, pPadV);

            if (isPillActive) {
                tvPill.setTextColor(Color.parseColor("#81C784"));
                tvPill.setBackgroundResource(R.drawable.bg_card_adblock_on);
            } else {
                tvPill.setTextColor(Color.parseColor("#CAC4D0"));
                tvPill.setBackgroundResource(R.drawable.bg_card_adblock_off);
            }
            row.addView(tvPill);
        }

        container.addView(row);
    }
}
