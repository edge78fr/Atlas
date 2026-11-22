package fr.edge78.atlas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import org.mozilla.geckoview.*;

// L'Atlas dans une appli, avec le moteur de Firefox (GeckoView) : pas de lignes entre les tuiles.
public class MainActivity extends Activity {
  static final String URL = "https://edge78fr.github.io/Atlas/";
  static GeckoRuntime rt;
  GeckoSession s; GeckoView v; boolean retour = false;
  GeckoSession.PermissionDelegate.Callback permCb;
  GeckoResult<GeckoSession.PromptDelegate.PromptResponse> fichRes; GeckoSession.PromptDelegate.FilePrompt fichPrompt;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    v = new GeckoView(this); setContentView(v);
    if (rt == null) rt = GeckoRuntime.create(this);
    s = new GeckoSession(new GeckoSessionSettings.Builder().usePrivateMode(false).build());
    s.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
      @Override public void onCanGoBack(GeckoSession se, boolean c) { retour = c; }
      @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession se, LoadRequest r) {
        Uri u = Uri.parse(r.uri); String h = u.getHost(); String sc = u.getScheme();
        if (sc != null && (sc.equals("blob") || sc.equals("data") || sc.equals("about"))) return GeckoResult.allow();
        if (h != null && h.equals("edge78fr.github.io")) return GeckoResult.allow();
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) {}
        return GeckoResult.deny();
      }
    });
    s.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
      @Override public void onAndroidPermissionsRequest(GeckoSession se, String[] perms, Callback cb) {
        if (perms == null || perms.length == 0) { cb.grant(); return; }
        permCb = cb; requestPermissions(perms, 1);
      }
      @Override public GeckoResult<Integer> onContentPermissionRequest(GeckoSession se, ContentPermission p) {
        return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
      }
    });
    s.setPromptDelegate(new GeckoSession.PromptDelegate() {
      @Override public GeckoResult<PromptResponse> onAlertPrompt(GeckoSession se, AlertPrompt p) {
        GeckoResult<PromptResponse> r = new GeckoResult<>();
        new AlertDialog.Builder(MainActivity.this).setMessage(p.message).setCancelable(false)
          .setPositiveButton("OK", (d, w) -> r.complete(p.dismiss())).show();
        return r;
      }
      @Override public GeckoResult<PromptResponse> onButtonPrompt(GeckoSession se, ButtonPrompt p) {
        GeckoResult<PromptResponse> r = new GeckoResult<>();
        new AlertDialog.Builder(MainActivity.this).setMessage(p.message).setCancelable(false)
          .setPositiveButton("OK", (d, w) -> r.complete(p.confirm(ButtonPrompt.Type.POSITIVE)))
          .setNegativeButton("Annuler", (d, w) -> r.complete(p.confirm(ButtonPrompt.Type.NEGATIVE))).show();
        return r;
      }
      @Override public GeckoResult<PromptResponse> onTextPrompt(GeckoSession se, TextPrompt p) {
        GeckoResult<PromptResponse> r = new GeckoResult<>();
        EditText t = new EditText(MainActivity.this); if (p.defaultValue != null) t.setText(p.defaultValue);
        new AlertDialog.Builder(MainActivity.this).setMessage(p.message).setView(t).setCancelable(false)
          .setPositiveButton("OK", (d, w) -> r.complete(p.confirm(t.getText().toString())))
          .setNegativeButton("Annuler", (d, w) -> r.complete(p.dismiss())).show();
        return r;
      }
      @Override public GeckoResult<PromptResponse> onFilePrompt(GeckoSession se, FilePrompt p) {
        fichRes = new GeckoResult<>(); fichPrompt = p;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        try { startActivityForResult(Intent.createChooser(i, "Choisir un plan"), 2); } catch (Exception e) { fichRes.complete(p.dismiss()); }
        return fichRes;
      }
    });
    s.open(rt); v.setSession(s);
    s.loadUri(URL);
  }
  @Override public void onRequestPermissionsResult(int rc, String[] p, int[] r) {
    if (rc == 1 && permCb != null) {
      boolean ok = r.length > 0; for (int x : r) if (x != PackageManager.PERMISSION_GRANTED) ok = false;
      if (ok) permCb.grant(); else permCb.reject(); permCb = null;
    }
  }
  @Override protected void onActivityResult(int rc, int res, Intent d) {
    if (rc == 2 && fichRes != null) {
      if (res == RESULT_OK && d != null && d.getData() != null) fichRes.complete(fichPrompt.confirm(this, d.getData()));
      else fichRes.complete(fichPrompt.dismiss());
      fichRes = null;
    }
  }
  @Override public void onBackPressed() { if (retour) s.goBack(); else super.onBackPressed(); }
}
