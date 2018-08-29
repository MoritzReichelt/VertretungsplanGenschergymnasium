package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {

    private String username;
    private String password;
    private WebView webView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialisiere das Refresh Layout
        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe);

        //Hole Anmeldedaten aus den SharedPrefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String url = Methods.getSharedPrefsURL(getApplicationContext());
        username = Methods.getSharedPrefsUsername(getApplicationContext());
        password = Methods.getSharedPrefsPassword(getApplicationContext());
        boolean stretchScreen = sharedPreferences.getBoolean("pref_stretch", false);
        boolean useCache = sharedPreferences.getBoolean("pref_cache", false);

        webView = findViewById(R.id.webView);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(stretchScreen); //True, um die Seite auf den gesamten Bildschirm zu strecken
        webSettings.setTextZoom(10);
        webSettings.setMinimumFontSize(8);

        if (useCache) {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        }

        webView.setWebViewClient(new WebViewClient() {

            public void onReceivedHttpAuthRequest(WebView view,
                                                  HttpAuthHandler handler, String host, String realm) {

                handler.proceed(username, password);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);

                webSettings.setTextZoom(80);
                String htmlString = "<html><body>" +
                        "<h3><strong>Webseite nicht verfügbar</strong></h3>\n" +
                        "<p>Die Website unter <strong>" + failingUrl + "</strong> konnte nicht geladen werden, weil:</p>\n" +
                        "<p>" + description + "</p></body></html>";

                webView.loadUrl("about:blank");
                webView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        if (savedInstanceState == null) {
            webSettings.setTextZoom(10);
            webView.loadUrl(url);
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.button_about:
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                break;
            case R.id.button_preferences:
                startActivity(new Intent(MainActivity.this, PrefsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}