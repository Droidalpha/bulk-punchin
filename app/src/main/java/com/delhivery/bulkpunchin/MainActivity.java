package com.delhivery.bulkpunchin;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.KeyEvent;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView webView;
    private byte[] pendingCsv;
    private static final int REQUEST_SAVE_CSV = 1;
    private static final String START_URL = "https://adhoc-manpower.delhivery.com/bulk-punch-in";
    private static final String ALLOWED_HOST = "adhoc-manpower.delhivery.com";
    // The site builds its CSV in a blob, clicks a detached link and revokes the blob straight away,
    // so a DownloadListener never gets usable data. Catch the blob at the click instead.
    private static final String CSV_HOOK_JS =
        "(function(){" +
        "if(window.__csvHookInstalled)return;" +
        "window.__csvHookInstalled=true;" +
        "var blobs={};" +
        "var create=URL.createObjectURL;" +
        "URL.createObjectURL=function(o){var u=create.call(URL,o);if(o instanceof Blob)blobs[u]=o;return u;};" +
        "var click=HTMLAnchorElement.prototype.click;" +
        "HTMLAnchorElement.prototype.click=function(){" +
        "var b=blobs[this.href];" +
        "if(!b||!this.download)return click.call(this);" +
        "delete blobs[this.href];" +
        "var name=this.download;" +
        "var r=new FileReader();" +
        "r.onload=function(){DelhiveryDownloads.saveCsv(name,String(r.result).split(',')[1]||'');};" +
        "r.readAsDataURL(b);" +
        "};" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new CsvBridge(), "DelhiveryDownloads");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(CSV_HOOK_JS, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (isAllowed(uri)) {
                    return false;
                }
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (Exception e) { Toast.makeText(MainActivity.this, "Unable to open link", Toast.LENGTH_SHORT).show(); }
                    return true;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "Unsupported link", Toast.LENGTH_SHORT).show(); }
                return true;
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(START_URL);
        }
    }

    private static boolean isAllowed(Uri uri) {
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null &&
            (host.equals(ALLOWED_HOST) || host.endsWith(".delhivery.com"));
    }

    private final class CsvBridge {
        @JavascriptInterface
        public void saveCsv(final String fileName, final String base64) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    askWhereToSaveCsv(fileName, base64);
                }
            });
        }
    }

    private void askWhereToSaveCsv(String fileName, String base64) {
        String pageUrl = webView.getUrl();
        if (pageUrl == null || !isAllowed(Uri.parse(pageUrl))) return;
        try {
            pendingCsv = Base64.decode(base64, Base64.DEFAULT);
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(intent, REQUEST_SAVE_CSV);
        } catch (Exception e) {
            pendingCsv = null;
            Toast.makeText(this, "Unable to save file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SAVE_CSV) return;
        byte[] csv = pendingCsv;
        pendingCsv = null;
        if (resultCode != RESULT_OK || data == null || data.getData() == null || csv == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            out.write(csv);
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to save file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
