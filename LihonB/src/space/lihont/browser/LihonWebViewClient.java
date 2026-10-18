package space.lihont.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class LihonWebViewClient extends WebViewClient {

    private final Context context;
    private final View startPageView;
    private final TextView urlDisplayView;
    private final TextView adblockStatsView;
    private final boolean incognito;

    public LihonWebViewClient(Context context, View startPageView, TextView urlDisplayView, TextView adblockStatsView, boolean incognito) {
        this.context = context;
        this.startPageView = startPageView;
        this.urlDisplayView = urlDisplayView;
        this.adblockStatsView = adblockStatsView;
        this.incognito = incognito;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request != null && request.getUrl() != null) {
            String urlStr = request.getUrl().toString();
            WebResourceResponse pluginResp = space.lihont.browser.plugin.PluginManager.getInstance().dispatchIntercept(urlStr);
            if (pluginResp != null) return pluginResp;

            if (AdBlocker.isAd(request.getUrl())) {
                // Drop ad request
                return AdBlocker.createEmptyResponse();
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (startPageView != null) {
            startPageView.setVisibility(View.GONE);
        }
        view.setVisibility(View.VISIBLE);
        if (urlDisplayView != null && url != null) {
            try {
                Uri parsed = Uri.parse(url);
                String host = parsed.getHost();
                urlDisplayView.setText(host != null ? host : url);
            } catch (Exception e) {
                urlDisplayView.setText(url);
            }
        }
        updateAdblockBadge();
        space.lihont.browser.plugin.PluginManager.getInstance().dispatchPageStarted(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        updateAdblockBadge();
        space.lihont.browser.plugin.PluginManager.getInstance().dispatchPageFinished(view, url);
    }

    private void updateAdblockBadge() {
        if (adblockStatsView != null) {
            int count = AdBlocker.getBlockedCount();
            if (count > 0) {
                adblockStatsView.setText("Заблокировано угроз и баннеров: " + count);
            } else {
                adblockStatsView.setText("Блокирует баннеры, трекеры и рекламу");
            }
        }
    }
}
