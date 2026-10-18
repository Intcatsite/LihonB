package space.lihont.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class RecentSitesManager {

    private static final String PREF_NAME = "lihonb_recents";
    private static final String KEY_SITES = "recent_urls";
    private static final int MAX_SITES = 4;

    public static class SiteItem {
        public final String title;
        public final String url;
        public final String host;

        public SiteItem(String title, String url, String host) {
            this.title = title;
            this.url = url;
            this.host = host;
        }
    }

    public static List<SiteItem> getRecentSites(Context context) {
        List<SiteItem> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SITES, null);

        if (raw != null && !raw.isEmpty()) {
            String[] entries = raw.split(";");
            for (String entry : entries) {
                String[] parts = entry.split("\\|");
                if (parts.length >= 2) {
                    list.add(new SiteItem(parts[0], parts[1], extractHost(parts[1])));
                }
            }
        }

        // Fill with defaults if needed
        if (list.size() < MAX_SITES) {
            list.add(new SiteItem("yako.ru", "https://yako.ru", "yako.ru"));
        }
        if (list.size() < MAX_SITES) {
            list.add(new SiteItem("lihont.space", "https://lihont.space", "lihont.space"));
        }
        if (list.size() < MAX_SITES) {
            list.add(new SiteItem("tiktok.com", "https://www.tiktok.com", "tiktok.com"));
        }
        if (list.size() < MAX_SITES) {
            list.add(new SiteItem("youtube.com", "https://m.youtube.com", "youtube.com"));
        }

        return list.subList(0, Math.min(MAX_SITES, list.size()));
    }

    public static void recordSite(Context context, String url, String title) {
        if (url == null || url.startsWith("about:") || url.startsWith("data:")) return;
        String host = extractHost(url);
        if (host == null || host.isEmpty()) return;

        List<SiteItem> current = getRecentSites(context);
        List<SiteItem> updated = new ArrayList<>();
        updated.add(new SiteItem(host, url, host));

        for (SiteItem item : current) {
            if (!item.host.equalsIgnoreCase(host) && updated.size() < MAX_SITES) {
                updated.add(item);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < updated.size(); i++) {
            SiteItem it = updated.get(i);
            sb.append(it.title).append("|").append(it.url);
            if (i < updated.size() - 1) {
                sb.append(";");
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SITES, sb.toString()).apply();
    }

    private static String extractHost(String url) {
        try {
            Uri uri = Uri.parse(url);
            String h = uri.getHost();
            if (h != null) {
                return h.startsWith("www.") ? h.substring(4) : h;
            }
        } catch (Exception ignored) {}
        return url;
    }
}
