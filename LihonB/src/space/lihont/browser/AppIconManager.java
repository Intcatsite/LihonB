package space.lihont.browser;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Handles dynamic app launcher icon switching in Android via activity-aliases.
 */
public class AppIconManager {

    public static final String ALIAS_ORIGINAL = "space.lihont.browser.AliasOriginal";
    public static final String ALIAS_PINK = "space.lihont.browser.AliasPink";
    public static final String ALIAS_PASTEL = "space.lihont.browser.AliasPastel";
    public static final String ALIAS_FLAT = "space.lihont.browser.AliasFlat";
    public static final String ALIAS_LB = "space.lihont.browser.AliasLB";

    public static final String[] ALL_ALIASES = {
        ALIAS_ORIGINAL,
        ALIAS_PINK,
        ALIAS_PASTEL,
        ALIAS_FLAT,
        ALIAS_LB
    };

    public static void setAppIcon(Context context, String targetAlias) {
        PackageManager pm = context.getPackageManager();
        for (String alias : ALL_ALIASES) {
            ComponentName cn = new ComponentName(context, alias);
            int newState = alias.equals(targetAlias)
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            try {
                pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP);
            } catch (Exception ignored) {}
        }
    }

    public static String getActiveAppIcon(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String alias : ALL_ALIASES) {
            ComponentName cn = new ComponentName(context, alias);
            try {
                int state = pm.getComponentEnabledSetting(cn);
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return alias;
                }
            } catch (Exception ignored) {}
        }
        return ALIAS_ORIGINAL;
    }
}
