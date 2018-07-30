package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {

    private String url, username, password;
    private SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialisiere das Refresh Layout
        swipeRefreshLayout = findViewById(R.id.swipe);

        //Hole Anmeldedaten aus den SharedPrefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        url = Methods.getSharedPrefsURL(getApplicationContext());
        username = Methods.getSharedPrefsUsername(getApplicationContext());
        password = Methods.getSharedPrefsPassword(getApplicationContext());
        boolean stretchScreen = sharedPreferences.getBoolean("pref_stretch", false);

        webView = findViewById(R.id.webView);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(stretchScreen); //True, um die Seite auf den gesamten Bildschirm zu strecken
        webSettings.setTextZoom(10);
        webSettings.setMinimumFontSize(10);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        webView.setWebViewClient(new WebViewClient() {

            public void onReceivedHttpAuthRequest(WebView view,
                                                  HttpAuthHandler handler, String host, String realm) {

                handler.proceed(username, password);
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(url);
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (isDeviceOffline()) {
                    webSettings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                    webView.loadUrl(url);
                    swipeRefreshLayout.setRefreshing(false);
                } else {
                    webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                    webView.loadUrl(url);
                    swipeRefreshLayout.setRefreshing(false);
                }
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

    private boolean isDeviceOffline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }

        return networkInfo == null || !networkInfo.isConnected();
    }
}

