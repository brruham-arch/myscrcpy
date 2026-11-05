package org.client.scrcpy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class OverlayLayer extends FrameLayout {
    private boolean editMode = false;
    private View selected;
    private float lastX, lastY;
    private final ScaleGestureDetector scaleDetector;
    private final Paint border = new Paint();

    public OverlayLayer(Context c) { this(c, null); }

    public OverlayLayer(Context c, AttributeSet a) {
        super(c, a);
        setWillNotDraw(false);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(Color.YELLOW);
        border.setStrokeWidth(4f);
        scaleDetector = new ScaleGestureDetector(c,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                if (selected != null) {
                    float s = selected.getScaleX() * d.getScaleFactor();
                    s = Math.max(0.2f, Math.min(s, 8f));
                    selected.setScaleX(s);
                    selected.setScaleY(s);
                    invalidate();
                }
                return true;
            }
        });
    }

    public void setEditMode(boolean on) {
        editMode = on;
        if (!on) selected = null;
        invalidate();
    }

    public boolean isEditMode() { return editMode; }

    public void addText(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(24f);
        tv.setShadowLayer(4f, 0f, 0f, Color.BLACK);
        addView(tv, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        selected = tv;
        invalidate();
    }

    // src: Uri, File, atau path; Glide otomatis menganimasikan GIF
    public void addImage(Object src) {
        ImageView iv = new ImageView(getContext());
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(getContext()).load(src).into(iv);
        iv.setTag(String.valueOf(src));
        addView(iv, new LayoutParams(400, 400, Gravity.CENTER));
        selected = iv;
        invalidate();
    }

    public void removeSelected() {
        if (selected != null) {
            removeView(selected);
            selected = null;
            invalidate();
        }
    }

    private View hit(float x, float y) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View v = getChildAt(i);
            float cx = v.getX() + v.getWidth() / 2f;
            float cy = v.getY() + v.getHeight() / 2f;
            float hw = v.getWidth() * v.getScaleX() / 2f;
            float hh = v.getHeight() * v.getScaleY() / 2f;
            if (Math.abs(x - cx) <= hw && Math.abs(y - cy) <= hh) return v;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!editMode) return false; // teruskan sentuhan ke game
        scaleDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                selected = hit(e.getX(), e.getY());
                if (selected == null) { invalidate(); return false; }
                lastX = e.getX();
                lastY = e.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (selected != null && e.getPointerCount() == 1
                        && !scaleDetector.isInProgress()) {
                    selected.setTranslationX(selected.getTranslationX() + e.getX() - lastX);
                    selected.setTranslationY(selected.getTranslationY() + e.getY() - lastY);
                    invalidate();
                }
                lastX = e.getX();
                lastY = e.getY();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int idx = e.getActionIndex() == 0 ? 1 : 0;
                lastX = e.getX(idx);
                lastY = e.getY(idx);
                break;
        }
        return true;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (editMode && selected != null) {
            float cx = selected.getX() + selected.getWidth() / 2f;
            float cy = selected.getY() + selected.getHeight() / 2f;
            float hw = selected.getWidth() * selected.getScaleX() / 2f;
            float hh = selected.getHeight() * selected.getScaleY() / 2f;
            canvas.drawRect(cx - hw, cy - hh, cx + hw, cy + hh, border);
        }
    }

    public void save(String key) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            float w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                org.json.JSONObject o = new org.json.JSONObject();
                if (v instanceof TextView) {
                    o.put("t", "text");
                    o.put("v", ((TextView) v).getText().toString());
                } else if (v.getTag() != null) {
                    o.put("t", "img");
                    o.put("v", v.getTag().toString());
                } else continue;
                o.put("x", v.getTranslationX() / w);
                o.put("y", v.getTranslationY() / h);
                o.put("s", v.getScaleX());
                arr.put(o);
            }
            getContext().getSharedPreferences("overlay", 0)
                    .edit().putString(key, arr.toString()).apply();
        } catch (Exception e) { }
    }

    public void load(String key) {
        final String json = getContext().getSharedPreferences("overlay", 0)
                .getString(key, null);
        if (json == null) return;
        post(() -> {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                float w = getWidth(), h = getHeight();
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject o = arr.getJSONObject(i);
                    if ("text".equals(o.getString("t"))) addText(o.getString("v"));
                    else addImage(new java.io.File(o.getString("v")));
                    View v = getChildAt(getChildCount() - 1);
                    v.setTranslationX((float) o.getDouble("x") * w);
                    v.setTranslationY((float) o.getDouble("y") * h);
                    float sc = (float) o.getDouble("s");
                    v.setScaleX(sc);
                    v.setScaleY(sc);
                }
                selected = null;
                invalidate();
            } catch (Exception e) { }
        });
    }
}
