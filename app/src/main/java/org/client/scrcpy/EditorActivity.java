package org.client.scrcpy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.*;

public class EditorActivity extends Activity {
    private OverlayLayer ol;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#222222"));

        // placeholder game landscape (16:9) di tengah
        View game = new View(this);
        game.setBackgroundColor(Color.parseColor("#555555"));
        int w = getResources().getDisplayMetrics().widthPixels;
        root.addView(game, new FrameLayout.LayoutParams(w, w * 9 / 16, Gravity.CENTER));
        TextView lbl = new TextView(this);
        lbl.setText("AREA GAME");
        lbl.setTextColor(Color.LTGRAY);
        root.addView(lbl, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        ol = new OverlayLayer(this);
        ol.setEditMode(true);
        root.addView(ol, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        Button add = new Button(this); add.setText("+");
        Button del = new Button(this); del.setText("Hapus");
        Button save = new Button(this); save.setText("Simpan");
        bar.addView(add); bar.addView(del); bar.addView(save);
        root.addView(bar, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END));
        setContentView(root);

        add.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setItems(new String[]{"Teks", "Gambar/GIF"}, (dd, which) -> {
                if (which == 1) {
                    Intent p = new Intent(Intent.ACTION_GET_CONTENT);
                    p.setType("image/*");
                    startActivityForResult(p, 77);
                } else {
                    final EditText et = new EditText(this);
                    new AlertDialog.Builder(this).setTitle("Teks").setView(et)
                        .setPositiveButton("OK", (d, x) -> ol.addText(et.getText().toString()))
                        .setNegativeButton("Batal", null).show();
                }
            }).show());
        del.setOnClickListener(v -> ol.removeSelected());
        save.setOnClickListener(v -> {
            Toast.makeText(this, "Tersimpan (simpan data menyusul)", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 77 || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            File f = new File(getFilesDir(), "ov_" + System.currentTimeMillis());
            InputStream in = getContentResolver().openInputStream(data.getData());
            OutputStream out = new FileOutputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            out.close();
            ol.addImage(f);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal muat gambar: " + e, Toast.LENGTH_LONG).show();
        }
    }
}
