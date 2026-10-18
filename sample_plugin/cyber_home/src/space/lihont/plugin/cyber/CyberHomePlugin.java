package space.lihont.plugin.cyber;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import space.lihont.browser.plugin.LihonPlugin;
import space.lihont.browser.plugin.PluginContext;

/**
 * Example LihonB Plugin (.lbp) demonstrating dynamic home screen customization,
 * declarative Material 3 settings, and browser interaction.
 */
public class CyberHomePlugin extends LihonPlugin {

    @Override
    public void onStart(PluginContext ctx) {
        ctx.log("CyberHomePlugin started!");

        // Auto-generated MD3 Settings (like MargyT)
        ctx.addSettingsSection("Кибер-стиль экрана");
        ctx.addSettingsToggle("Неоновый виджет", "Отображать кибер-панель на стартовом экране", "show_widget", true);
        ctx.addSettingsChoice("Стиль акцента", "Цветовой акцент карточки", "banner_color",
            new String[]{"Неоново-фиолетовый (#D0BCFF)", "Кибер-зелёный (#80E89E)", "Электро-розовый (#EFB8C8)"}, 0);
        ctx.addSettingsSlider("Размер шрифта статуса", "Размер текста заголовка виджета (sp)", "status_size", 13, 20, 15);

        ctx.showToast("⚡ Кибер-плагин LihonB активирован!");
    }

    @Override
    public void onStop() {
        getContext().log("CyberHomePlugin stopped!");
        // Remove widget if present
        Activity a = getContext().getActivity();
        if (a != null) {
            View widget = a.findViewById(a.getResources().getIdentifier("cyber_home_widget", "id", a.getPackageName()));
            if (widget == null) {
                // Find by tag
                View root = a.findViewById(android.R.id.content);
                if (root != null) {
                    widget = root.findViewWithTag("cyber_home_widget");
                }
            }
            if (widget != null && widget.getParent() instanceof ViewGroup) {
                ((ViewGroup) widget.getParent()).removeView(widget);
            }
        }
    }

    @Override
    public void onSettingsChanged(String key) {
        getContext().log("Setting changed: " + key);
        Activity a = getContext().getActivity();
        if (a != null) {
            View startPage = a.findViewById(a.getResources().getIdentifier("start_page", "id", a.getPackageName()));
            if (startPage != null) {
                onHomeScreenCreated(a, startPage);
            }
        }
    }

    @Override
    public void onHomeScreenCreated(Activity activity, View homeScreenRoot) {
        if (activity == null || homeScreenRoot == null) return;

        boolean showWidget = getContext().prefs().getBoolean("show_widget", true);

        // Remove old widget if already added
        View oldWidget = homeScreenRoot.findViewWithTag("cyber_home_widget");
        if (oldWidget != null && oldWidget.getParent() instanceof ViewGroup) {
            ((ViewGroup) oldWidget.getParent()).removeView(oldWidget);
        }

        if (!showWidget) return;

        int choice = getContext().prefs().getInt("banner_color", 0);
        String accentColor = "#D0BCFF";
        int strokeColor = Color.parseColor("#D0BCFF");
        if (choice == 1) {
            accentColor = "#80E89E";
            strokeColor = Color.parseColor("#80E89E");
        } else if (choice == 2) {
            accentColor = "#EFB8C8";
            strokeColor = Color.parseColor("#EFB8C8");
        }

        int fontSize = getContext().prefs().getInt("status_size", 15);

        // Build Custom Neon Card
        LinearLayout cyberCard = new LinearLayout(activity);
        cyberCard.setTag("cyber_home_widget");
        cyberCard.setOrientation(LinearLayout.VERTICAL);
        cyberCard.setPadding(32, 28, 32, 28);

        // Shape with glowing neon border
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(Color.parseColor("#211F26"));
        gd.setCornerRadius(24 * activity.getResources().getDisplayMetrics().density);
        gd.setStroke((int) (2 * activity.getResources().getDisplayMetrics().density), strokeColor);
        cyberCard.setBackground(gd);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        clp.setMargins(0, 16, 0, 16);
        cyberCard.setLayoutParams(clp);

        // Title Row
        TextView title = new TextView(activity);
        title.setText("⚡ CYBER LIHON MOD ENGINE");
        title.setTextColor(Color.parseColor(accentColor));
        title.setTextSize(fontSize);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        cyberCard.addView(title);

        TextView sub = new TextView(activity);
        sub.setText("Плагин кастомизировал домашний экран!\nТапните здесь для перехода на официальный сайт lihont.space 🚀");
        sub.setTextColor(Color.parseColor("#E6E0E9"));
        sub.setTextSize(12);
        sub.setPadding(0, 8, 0, 12);
        cyberCard.addView(sub);

        // Interactive Click
        cyberCard.setOnClickListener(v -> {
            getContext().openUrl("https://lihont.space");
            getContext().showToast("🚀 Переход на lihont.space через плагин!");
        });

        // Insert into start_page_content
        ViewGroup target = null;
        int contentId = activity.getResources().getIdentifier("start_page_content", "id", activity.getPackageName());
        if (contentId != 0) {
            View v = homeScreenRoot.findViewById(contentId);
            if (v instanceof ViewGroup) target = (ViewGroup) v;
        }
        if (target == null && homeScreenRoot instanceof ViewGroup) {
            target = (ViewGroup) homeScreenRoot;
        }
        if (target != null) {
            // Insert after search bar / incognito button (index 4 or child count)
            int insertIndex = Math.min(4, target.getChildCount());
            target.addView(cyberCard, insertIndex);
        }
    }
}
