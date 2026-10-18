package space.lihont.browser;

import android.net.Uri;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lihon AdBlocker: Zero-overhead DNS and domain-level ad & tracker filter.
 */
public final class AdBlocker {

    private static final Set<String> AD_HOSTS = new HashSet<>();
    private static final AtomicInteger BLOCKED_COUNT = new AtomicInteger(0);
    private static boolean enabled = true;

    static {
        // High-volume advertising networks, tracking beacons & popup scripts
        String[] hosts = {
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "adservice.google.com", "pagead2.googlesyndication.com",
            "an.yandex.ru", "mc.yandex.ru", "adfox.yandex.ru",
            "connect.facebook.net", "ads.twitter.com", "c.amazon-adsystem.com",
            "adnxs.com", "criteo.com", "pubmatic.com", "rubiconproject.com",
            "taboola.com", "outbrain.com", "scorecardresearch.com",
            "adroll.com", "quantserve.com", "advertising.com",
            "popcash.net", "popads.net", "propellerads.com",
            "tracking.yandex.ru", "appmetrica.yandex.ru",
            "analytics.google.com", "google-analytics.com",
            "hotjar.com", "clarity.ms", "statcounter.com"
        };
        for (String h : hosts) {
            AD_HOSTS.add(h);
        }
    }

    private AdBlocker() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static int getBlockedCount() {
        return BLOCKED_COUNT.get();
    }

    public static void resetCount() {
        BLOCKED_COUNT.set(0);
    }

    public static boolean isAd(Uri uri) {
        if (!enabled || uri == null) return false;
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase();

        for (String adHost : AD_HOSTS) {
            if (host.equals(adHost) || host.endsWith("." + adHost)) {
                BLOCKED_COUNT.incrementAndGet();
                return true;
            }
        }
        return false;
    }

    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
    }
}
