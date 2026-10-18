package space.lihont.browser.plugin;

public class SettingItem {
    public enum Type {
        SECTION, TOGGLE, SLIDER, CHOICE
    }

    public Type type;
    public String title;
    public String description;
    public String key;
    public boolean defaultBool;
    public int min;
    public int max;
    public int defaultInt;
    public String[] choices;
    public int defaultChoice;

    public static SettingItem section(String title) {
        SettingItem i = new SettingItem();
        i.type = Type.SECTION;
        i.title = title;
        return i;
    }

    public static SettingItem toggle(String title, String desc, String key, boolean defaultVal) {
        SettingItem i = new SettingItem();
        i.type = Type.TOGGLE;
        i.title = title;
        i.description = desc;
        i.key = key;
        i.defaultBool = defaultVal;
        return i;
    }

    public static SettingItem slider(String title, String desc, String key, int min, int max, int defaultVal) {
        SettingItem i = new SettingItem();
        i.type = Type.SLIDER;
        i.title = title;
        i.description = desc;
        i.key = key;
        i.min = min;
        i.max = max;
        i.defaultInt = defaultVal;
        return i;
    }

    public static SettingItem choice(String title, String desc, String key, String[] choices, int defaultIndex) {
        SettingItem i = new SettingItem();
        i.type = Type.CHOICE;
        i.title = title;
        i.description = desc;
        i.key = key;
        i.choices = choices;
        i.defaultChoice = defaultIndex;
        return i;
    }
}
