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
    private boolean receivedError = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstStart = mPreferences.getBoolean(Constants.firstStartKey, true);
        if (firstStart) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        } else {

            //Initialisiere das Refresh Layout
            final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe);

            //Hole Anmeldedaten aus den SharedPrefs
            final String url = mPreferences.getString(Constants.urlKey, getString(R.string.pref_url_default));
            username = mPreferences.getString(Constants.usernameKey, getString(R.string.pref_username_default));
            password = mPreferences.getString(Constants.passwordKey, getString(R.string.pref_password_default));
            boolean stretchScreen = mPreferences.getBoolean(Constants.stretchScreenKey, false);
            boolean useCache = mPreferences.getBoolean(Constants.cacheKey, false);

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
                    receivedError = true;

                    webSettings.setTextZoom(80);
                    String htmlString = "<html><body>" +
                            "<h3><strong>Webseite nicht verfügbar</strong></h3>\n" +
                            "<p>Die Website unter <strong>" + failingUrl + "</strong> konnte nicht geladen werden, weil:</p>\n" +
                            "<p>" + description + "</p></body></html>";

                    webView.loadUrl("about:blank");
                    webView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null);
                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    if (!receivedError) swipeRefreshLayout.setRefreshing(true);
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    swipeRefreshLayout.setRefreshing(false);
                    super.onPageFinished(view, url);
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
                    webSettings.setTextZoom(10);
                    webView.loadUrl(url);
                }
            });
        }
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
        finishAffinity();
        System.exit(0);
    }
}