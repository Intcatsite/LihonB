package space.lihont.browser;

import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LihonWebChromeClient extends WebChromeClient {

    private final ProgressBar progressBar;
    private final TextView titleView;

    public LihonWebChromeClient(ProgressBar progressBar, TextView titleView) {
        this.progressBar = progressBar;
        this.titleView = titleView;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if (newProgress < 100) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(newProgress);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        if (title != null && !title.isEmpty() && titleView != null) {
            titleView.setText(title);
        }
    }
}
