package space.lihont.browser.plugin;

import org.json.JSONObject;

public class PluginManifest {
    public String id;
    public String name;
    public String nameRu;
    public String version;
    public String author;
    public String description;
    public String descriptionRu;
    public String entry;
    public int minApi;

    public static PluginManifest fromJson(String jsonStr) throws Exception {
        JSONObject obj = new JSONObject(jsonStr);
        PluginManifest m = new PluginManifest();
        m.id = obj.getString("id");
        m.name = obj.optString("name", m.id);
        m.nameRu = obj.optString("name_ru", m.name);
        m.version = obj.optString("version", "1.0");
        m.author = obj.optString("author", "Lihon Developer");
        m.description = obj.optString("description", "");
        m.descriptionRu = obj.optString("description_ru", m.description);
        m.entry = obj.getString("entry");
        m.minApi = obj.optInt("min_api", 1);
        return m;
    }

    public String getDisplayName() {
        return (nameRu != null && !nameRu.trim().isEmpty()) ? nameRu : name;
    }

    public String getDisplayDescription() {
        return (descriptionRu != null && !descriptionRu.trim().isEmpty()) ? descriptionRu : description;
    }
}
