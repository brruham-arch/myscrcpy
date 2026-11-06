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
        TextView game = new TextView(this) { final android.graphics.Paint pp = new android.graphics.Paint(); { pp.setColor(Color.YELLOW); pp.setStrokeWidth(4f); } @Override protected void onDraw(android.graphics.Canvas c) { super.onDraw(c); float cx = getWidth() / 2f, cy = getHeight() / 2f; c.drawLine(cx - 40, cy, cx + 40, cy, pp); c.drawLine(cx, cy - 40, cx, cy + 40, pp); } }; game.setText("AREA GAME\n(tarik tepi kanan/bawah = skala)"); game.setTextColor(Color.LTGRAY); game.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        game.setBackgroundColor(Color.parseColor("#555555"));
        int w = getResources().getDisplayMetrics().widthPixels;
        root.addView(game, new FrameLayout.LayoutParams(w, w * 9 / 16, Gravity.CENTER));

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
        ol.load("main");
        android.content.SharedPreferences gp = getSharedPreferences("overlay", 0);
        game.setTranslationX(gp.getFloat("gx", 0f) * w);
        game.setTranslationY(gp.getFloat("gy", 0f) * w);
        game.setScaleX(gp.getFloat("gsx", 1f));
        game.setScaleY(gp.getFloat("gsy", 1f));
        final android.view.ScaleGestureDetector sd = new android.view.ScaleGestureDetector(this,
            new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(android.view.ScaleGestureDetector d) {
                    float f = d.getScaleFactor();
                    game.setScaleX(Math.max(0.3f, Math.min(game.getScaleX() * f, 4f))); game.setScaleY(Math.max(0.3f, Math.min(game.getScaleY() * f, 4f)));
                    return true;
                }
            });
        final float[] last = new float[2];
        final float[] rs = new float[5];
        game.setOnTouchListener((v, e) -> {
            int act0 = e.getActionMasked();
            if (act0 == android.view.MotionEvent.ACTION_DOWN) {
                float dn = getResources().getDisplayMetrics().density * 48f;
                int md = 0;
                if (e.getX() > v.getWidth() - dn / v.getScaleX()) md |= 1;
                if (e.getY() > v.getHeight() - dn / v.getScaleY()) md |= 2;
                rs[0] = md; rs[1] = e.getRawX(); rs[2] = e.getRawY();
                rs[3] = v.getScaleX(); rs[4] = v.getScaleY();
            }
            if (rs[0] > 0f) {
                if (act0 == android.view.MotionEvent.ACTION_MOVE && e.getPointerCount() == 1) {
                    int md2 = (int) rs[0];
                    if ((md2 & 1) != 0) v.setScaleX(Math.max(0.3f, Math.min(rs[3] + 2f * (e.getRawX() - rs[1]) / v.getWidth(), 4f)));
                    if ((md2 & 2) != 0) v.setScaleY(Math.max(0.3f, Math.min(rs[4] + 2f * (e.getRawY() - rs[2]) / v.getHeight(), 4f)));
                } else if (act0 == android.view.MotionEvent.ACTION_UP || act0 == android.view.MotionEvent.ACTION_CANCEL) {
                    rs[0] = 0f;
                }
                return true;
            }
            sd.onTouchEvent(e);
            int a = e.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) {
                last[0] = e.getRawX(); last[1] = e.getRawY();
            } else if (a == android.view.MotionEvent.ACTION_MOVE) {
                if (e.getPointerCount() == 1 && !sd.isInProgress()) {
                    game.setTranslationX(game.getTranslationX() + e.getRawX() - last[0]);
                    game.setTranslationY(game.getTranslationY() + e.getRawY() - last[1]);
                }
                last[0] = e.getRawX(); last[1] = e.getRawY();
            } else if (a == android.view.MotionEvent.ACTION_POINTER_UP) {
                int i = e.getActionIndex() == 0 ? 1 : 0;
                last[0] = e.getRawX() + (e.getX(i) - e.getX(0));
                last[1] = e.getRawY() + (e.getY(i) - e.getY(0));
            }
            return true;
        });

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
            ol.save("main");
            getSharedPreferences("overlay", 0).edit().putFloat("gx", game.getTranslationX() / w).putFloat("gy", game.getTranslationY() / w).putFloat("gsx", game.getScaleX()).putFloat("gsy", game.getScaleY()).apply();
            Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show();
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
