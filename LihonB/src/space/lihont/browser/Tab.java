package space.lihont.browser;

import android.webkit.WebView;

public class Tab {
    private final int id;
    private String title;
    private String url;
    private final boolean incognito;
    private final WebView webView;

    public Tab(int id, String title, String url, boolean incognito, WebView webView) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.incognito = incognito;
        this.webView = webView;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title != null && !title.isEmpty() ? title : (url != null ? url : "Новая вкладка");
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isIncognito() {
        return incognito;
    }

    public WebView getWebView() {
        return webView;
    }
}
