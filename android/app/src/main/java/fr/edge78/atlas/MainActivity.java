package fr.edge78.atlas;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;

// L'Atlas dans une appli : la page en ligne (chiffrée comme sur le site), avec GPS et choix de fichiers.
public class MainActivity extends Activity {
  static final String URL = "https://edge78fr.github.io/Atlas/";
  WebView w; ValueCallback<Uri[]> fc; GeolocationPermissions.Callback gcb; String gor;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    w = new WebView(this); setContentView(w);
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
    s.setGeolocationEnabled(true); s.setAllowFileAccess(false); s.setMediaPlaybackRequiresUserGesture(false);
    w.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        Uri u = r.getUrl(); String h = u.getHost();
        if (h != null && h.equals("edge78fr.github.io")) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) {}
        return true;
      }
    });
    w.setWebChromeClient(new WebChromeClient() {
      @Override public void onGeolocationPermissionsShowPrompt(String o, GeolocationPermissions.Callback c) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) c.invoke(o, true, false);
        else { gcb = c; gor = o; requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1); }
      }
      @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
        if (fc != null) fc.onReceiveValue(null);
        fc = cb;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        try { startActivityForResult(Intent.createChooser(i, "Choisir un plan"), 2); } catch (Exception e) { fc = null; return false; }
        return true;
      }
    });
    if (b != null) w.restoreState(b); else w.loadUrl(URL);
  }
  @Override public void onRequestPermissionsResult(int rc, String[] p, int[] r) {
    if (rc == 1 && gcb != null) { gcb.invoke(gor, r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED, false); gcb = null; }
  }
  @Override protected void onActivityResult(int rc, int res, Intent d) {
    if (rc == 2 && fc != null) { fc.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, d)); fc = null; }
  }
  @Override public void onBackPressed() { if (w.canGoBack()) w.goBack(); else super.onBackPressed(); }
  @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); w.saveState(o); }
}
