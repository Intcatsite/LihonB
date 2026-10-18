package space.lihont.browser.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import space.lihont.browser.R;

/**
 * Material Design 3 Dialog for managing LihonB plugins (.lbp)
 * and auto-rendering declared plugin settings.
 */
public class PluginsDialog {

    public interface OnInstallRequest {
        void onInstallRequested();
    }

    public static void show(Activity activity, OnInstallRequest installCallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ScrollView scroll = new ScrollView(activity);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_card);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        builder.setView(scroll);
        AlertDialog dialog = builder.create();

        // Top Header
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(activity);
        icon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        icon.setImageResource(R.drawable.ic_extension);
        header.addView(icon);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tclp.setMargins(20, 0, 16, 0);
        titleCol.setLayoutParams(tclp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Плагины LihonB");
        tvTitle.setTextColor(Color.parseColor("#E6E0E9"));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("Модули расширения (.lbp)");
        tvSub.setTextColor(Color.parseColor("#CAC4D0"));
        tvSub.setTextSize(12);
        titleCol.addView(tvSub);

        header.addView(titleCol);

        // Install Button
        Button btnInstall = new Button(activity);
        btnInstall.setText("+ Установить");
        btnInstall.setTextSize(12);
        btnInstall.setTextColor(Color.parseColor("#EADDFF"));
        btnInstall.setBackgroundResource(R.drawable.bg_pill);
        btnInstall.setOnClickListener(v -> {
            dialog.dismiss();
            if (installCallback != null) {
                installCallback.onInstallRequested();
            }
        });
        header.addView(btnInstall);

        root.addView(header);

        // Plugins List
        List<LoadedPlugin> plugins = PluginManager.getInstance().getLoadedPlugins();

        TextView tvSection = new TextView(activity);
        tvSection.setText("Установлено: " + plugins.size());
        tvSection.setTextColor(Color.parseColor("#D0BCFF"));
        tvSection.setTextSize(13);
        tvSection.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams sslp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sslp.setMargins(0, 24, 0, 12);
        tvSection.setLayoutParams(sslp);
        root.addView(tvSection);

        if (plugins.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("Пока нет установленных плагинов.\nНажмите «+ Установить», чтобы выбрать .lbp файл.");
            empty.setTextColor(Color.parseColor("#CAC4D0"));
            empty.setTextSize(13);
            empty.setPadding(0, 16, 0, 16);
            root.addView(empty);
        } else {
            for (LoadedPlugin p : plugins) {
                root.addView(createPluginCard(activity, p, dialog, installCallback));
            }
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private static View createPluginCard(Activity activity, LoadedPlugin p, AlertDialog parentDialog, OnInstallRequest callback) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(p.enabled ? R.drawable.bg_card_adblock_on : R.drawable.bg_card_adblock_off);
        card.setPadding(24, 20, 24, 20);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        clp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(clp);

        // Row 1: Title, Version, Switch
        LinearLayout row1 = new LinearLayout(activity);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout infoCol = new LinearLayout(activity);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(activity);
        title.setText(p.manifest.getDisplayName());
        title.setTextColor(Color.parseColor("#E6E0E9"));
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        infoCol.addView(title);

        TextView authorVer = new TextView(activity);
        authorVer.setText("v" + p.manifest.version + " • " + p.manifest.author);
        authorVer.setTextColor(Color.parseColor("#CAC4D0"));
        authorVer.setTextSize(11);
        infoCol.addView(authorVer);

        row1.addView(infoCol);

        // Switch
        Switch toggle = new Switch(activity);
        toggle.setChecked(p.enabled);
        toggle.setOnCheckedChangeListener((btn, isChecked) -> {
            PluginManager.getInstance().setPluginEnabled(p.manifest.id, isChecked);
            card.setBackgroundResource(isChecked ? R.drawable.bg_card_adblock_on : R.drawable.bg_card_adblock_off);
            Toast.makeText(activity, p.manifest.getDisplayName() + (isChecked ? " включён" : " выключен"), Toast.LENGTH_SHORT).show();
        });
        row1.addView(toggle);

        card.addView(row1);

        // Description
        if (!p.manifest.getDisplayDescription().isEmpty()) {
            TextView desc = new TextView(activity);
            desc.setText(p.manifest.getDisplayDescription());
            desc.setTextColor(Color.parseColor("#938F99"));
            desc.setTextSize(12);
            desc.setPadding(0, 8, 0, 8);
            card.addView(desc);
        }

        // Action Row: Settings ⚙️ and Delete 🗑️
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.setPadding(0, 6, 0, 0);

        if (p.context.getSettings().size() > 0) {
            Button btnSettings = new Button(activity);
            btnSettings.setText("⚙ Настройки");
            btnSettings.setTextSize(11);
            btnSettings.setTextColor(Color.parseColor("#EADDFF"));
            btnSettings.setBackgroundResource(R.drawable.bg_pill);
            LinearLayout.LayoutParams bslp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 32 * (int) activity.getResources().getDisplayMetrics().density
            );
            bslp.setMargins(0, 0, 12, 0);
            btnSettings.setLayoutParams(bslp);
            btnSettings.setOnClickListener(v -> showPluginSettings(activity, p));
            actions.addView(btnSettings);
        }

        ImageView btnDelete = new ImageView(activity);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(54, 54));
        btnDelete.setPadding(8, 8, 8, 8);
        btnDelete.setImageResource(R.drawable.ic_trash);
        btnDelete.setOnClickListener(v -> {
            PluginManager.getInstance().deletePlugin(p.manifest.id);
            parentDialog.dismiss();
            show(activity, callback);
            Toast.makeText(activity, "Плагин удалён", Toast.LENGTH_SHORT).show();
        });
        actions.addView(btnDelete);

        card.addView(actions);

        return card;
    }

    // =========================================================================
    // AUTO-GENERATED MD3 SETTINGS (LIKE MARGYT)
    // =========================================================================
    public static void showPluginSettings(Activity activity, LoadedPlugin p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_card);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        builder.setView(scroll);
        AlertDialog dialog = builder.create();

        // Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Настройки: " + p.manifest.getDisplayName());
        tvTitle.setTextColor(Color.parseColor("#E6E0E9"));
        tvTitle.setTextSize(17);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        List<SettingItem> items = p.context.getSettings();
        for (SettingItem item : items) {
            switch (item.type) {
                case SECTION: {
                    TextView sec = new TextView(activity);
                    sec.setText(item.title);
                    sec.setTextColor(Color.parseColor("#D0BCFF"));
                    sec.setTextSize(13);
                    sec.setTypeface(null, android.graphics.Typeface.BOLD);
                    sec.setPadding(0, 20, 0, 8);
                    root.addView(sec);
                    break;
                }
                case TOGGLE: {
                    LinearLayout row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, 12, 0, 12);

                    LinearLayout textCol = new LinearLayout(activity);
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                    TextView t = new TextView(activity);
                    t.setText(item.title);
                    t.setTextColor(Color.parseColor("#E6E0E9"));
                    t.setTextSize(14);
                    textCol.addView(t);

                    if (item.description != null && !item.description.isEmpty()) {
                        TextView d = new TextView(activity);
                        d.setText(item.description);
                        d.setTextColor(Color.parseColor("#CAC4D0"));
                        d.setTextSize(11);
                        textCol.addView(d);
                    }
                    row.addView(textCol);

                    Switch s = new Switch(activity);
                    boolean curVal = p.context.prefs().getBoolean(item.key, item.defaultBool);
                    s.setChecked(curVal);
                    s.setOnCheckedChangeListener((btn, isChecked) -> {
                        p.context.prefs().edit().putBoolean(item.key, isChecked).apply();
                        p.instance.onSettingsChanged(item.key);
                    });
                    row.addView(s);

                    root.addView(row);
                    break;
                }
                case SLIDER: {
                    LinearLayout col = new LinearLayout(activity);
                    col.setOrientation(LinearLayout.VERTICAL);
                    col.setPadding(0, 12, 0, 12);

                    int curVal = p.context.prefs().getInt(item.key, item.defaultInt);

                    TextView t = new TextView(activity);
                    t.setText(item.title + ": " + curVal);
                    t.setTextColor(Color.parseColor("#E6E0E9"));
                    t.setTextSize(14);
                    col.addView(t);

                    if (item.description != null) {
                        TextView d = new TextView(activity);
                        d.setText(item.description);
                        d.setTextColor(Color.parseColor("#CAC4D0"));
                        d.setTextSize(11);
                        col.addView(d);
                    }

                    SeekBar bar = new SeekBar(activity);
                    bar.setMax(item.max - item.min);
                    bar.setProgress(curVal - item.min);
                    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            int actual = item.min + progress;
                            t.setText(item.title + ": " + actual);
                            if (fromUser) {
                                p.context.prefs().edit().putInt(item.key, actual).apply();
                                p.instance.onSettingsChanged(item.key);
                            }
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    });
                    col.addView(bar);

                    root.addView(col);
                    break;
                }
                case CHOICE: {
                    LinearLayout row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(0, 12, 0, 12);

                    TextView t = new TextView(activity);
                    t.setText(item.title);
                    t.setTextColor(Color.parseColor("#E6E0E9"));
                    t.setTextSize(14);
                    row.addView(t);

                    int selected = p.context.prefs().getInt(item.key, item.defaultChoice);
                    String curLabel = (selected >= 0 && selected < item.choices.length) ? item.choices[selected] : "Выбрать";

                    Button btnChoice = new Button(activity);
                    btnChoice.setText(curLabel + " ▼");
                    btnChoice.setTextColor(Color.parseColor("#D0BCFF"));
                    btnChoice.setBackgroundResource(R.drawable.bg_omnibox);
                    btnChoice.setOnClickListener(v -> {
                        new AlertDialog.Builder(activity)
                            .setTitle(item.title)
                            .setSingleChoiceItems(item.choices, p.context.prefs().getInt(item.key, item.defaultChoice), (chDialog, which) -> {
                                p.context.prefs().edit().putInt(item.key, which).apply();
                                btnChoice.setText(item.choices[which] + " ▼");
                                p.instance.onSettingsChanged(item.key);
                                chDialog.dismiss();
                            })
                            .show();
                    });
                    row.addView(btnChoice);

                    root.addView(row);
                    break;
                }
            }
        }

        // Close Button
        Button btnClose = new Button(activity);
        btnClose.setText("Готово");
        btnClose.setTextColor(Color.parseColor("#381E72"));
        btnClose.setBackgroundResource(R.drawable.bg_pill);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 42 * (int) activity.getResources().getDisplayMetrics().density
        );
        clp.setMargins(0, 20, 0, 0);
        btnClose.setLayoutParams(clp);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
