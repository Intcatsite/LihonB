package space.lihont.plugin.clock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.Calendar;

import space.lihont.browser.plugin.LihonPlugin;
import space.lihont.browser.plugin.PluginContext;

public class Md3ClockPlugin extends LihonPlugin {

    private static final String KEY_COLOR = "accent_color";
    private static final String KEY_SIZE  = "clock_diameter";
    private static final String KEY_DARK_DIAL = "dark_dial";

    private static final int[] ACCENTS = {
        Color.parseColor("#D0BCFF"), // MD3 Monet Lavender
        Color.parseColor("#80D8D8"), // MD3 Cyan / Teal
        Color.parseColor("#EFB8C8"), // MD3 Pastel Pink
        Color.parseColor("#81C784"), // MD3 Pastel Green
        Color.parseColor("#FFB74D"), // MD3 Pastel Amber
        Color.parseColor("#FF8A80")  // MD3 Pastel Coral Red
    };

    private static final String[] ACCENT_NAMES = {
        "Лаванда (Monet)", "Бирюзовый", "Розовый", "Зелёный", "Янтарный", "Коралловый"
    };

    private Md3ClockView clockView;
    private FrameLayout wrapper;

    @Override
    public void onStart(PluginContext ctx) {
        ctx.log("MD3 Analog Clock плагин активирован");
        ctx.addSettingsSection("Внешний вид часов");
        ctx.addSettingsChoice("Цвет акцента", "Цвет секундной стрелки и индикатора",
                KEY_COLOR, ACCENT_NAMES, 0);
        ctx.addSettingsSlider("Диаметр часов", "Размер циферблата в dp",
                KEY_SIZE, 120, 260, 160);
        ctx.addSettingsToggle("Тёмный циферблат", "Использовать глубокую тёмную тему Monet для циферблата",
                KEY_DARK_DIAL, true);
    }

    @Override
    public void onStop() {
        if (clockView != null) {
            clockView.stopTicker();
        }
        Activity a = getContext() != null ? getContext().getActivity() : null;
        if (a != null) {
            View root = a.findViewById(android.R.id.content);
            if (root != null) {
                View old = root.findViewWithTag("md3_clock_widget");
                if (old != null && old.getParent() instanceof ViewGroup) {
                    ((ViewGroup) old.getParent()).removeView(old);
                }
            }
            int slotId = a.getResources().getIdentifier("plugin_slot_header", "id", a.getPackageName());
            if (slotId != 0) {
                View slotView = a.findViewById(slotId);
                if (slotView instanceof ViewGroup) {
                    ((ViewGroup) slotView).removeAllViews();
                }
            }
        }
        if (wrapper != null && wrapper.getParent() instanceof ViewGroup) {
            ((ViewGroup) wrapper.getParent()).removeView(wrapper);
        }
        wrapper = null;
        clockView = null;
    }

    @Override
    public void onSettingsChanged(String key) {
        PluginContext ctx = getContext();
        if (ctx == null) return;
        Activity a = ctx.getActivity();
        if (clockView == null && a != null) {
            int startPageId = a.getResources().getIdentifier("start_page", "id", a.getPackageName());
            View startPage = startPageId != 0 ? a.findViewById(startPageId) : null;
            if (startPage != null) {
                onHomeScreenCreated(a, startPage);
            }
        }
        if (clockView == null) return;
        Context c = ctx.getActivity();
        if (KEY_COLOR.equals(key)) {
            int idx = ctx.prefs().getInt(KEY_COLOR, 0);
            int color = ACCENTS[Math.max(0, Math.min(idx, ACCENTS.length - 1))];
            clockView.setAccentColor(color);
        } else if (KEY_SIZE.equals(key)) {
            int sizeDp = ctx.prefs().getInt(KEY_SIZE, 160);
            ViewGroup.LayoutParams lp = clockView.getLayoutParams();
            if (lp != null) {
                lp.width = dp(c, sizeDp);
                lp.height = dp(c, sizeDp);
                clockView.setLayoutParams(lp);
            }
            clockView.setClockScale(sizeDp / 160f);
        } else if (KEY_DARK_DIAL.equals(key)) {
            boolean dark = ctx.prefs().getBoolean(KEY_DARK_DIAL, true);
            clockView.setDialTheme(dark);
        }
    }

    @Override
    public void onHomeScreenCreated(Activity activity, View homeScreenRoot) {
        if (homeScreenRoot == null) return;

        // Note: In LihonB, homeScreenRoot is a ScrollView.
        // ScrollView can host only ONE direct child. So we must insert into its child LinearLayout!
        ViewGroup container;
        if (homeScreenRoot instanceof ScrollView) {
            ScrollView sv = (ScrollView) homeScreenRoot;
            if (sv.getChildCount() > 0 && sv.getChildAt(0) instanceof ViewGroup) {
                container = (ViewGroup) sv.getChildAt(0);
            } else {
                return;
            }
        } else if (homeScreenRoot instanceof ViewGroup) {
            container = (ViewGroup) homeScreenRoot;
        } else {
            return;
        }

        // Clean up previous instance by tag or reference
        View old = homeScreenRoot.findViewWithTag("md3_clock_widget");
        if (old != null && old.getParent() instanceof ViewGroup) {
            ((ViewGroup) old.getParent()).removeView(old);
        }
        if (wrapper != null && wrapper.getParent() instanceof ViewGroup) {
            ((ViewGroup) wrapper.getParent()).removeView(wrapper);
        }

        wrapper = new FrameLayout(activity);
        wrapper.setTag("md3_clock_widget");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity, 14);
        lp.bottomMargin = dp(activity, 10);
        wrapper.setLayoutParams(lp);

        int sizeDp = 160;
        int colorIdx = 0;
        boolean darkDial = true;
        if (getContext() != null) {
            sizeDp = getContext().prefs().getInt(KEY_SIZE, 160);
            colorIdx = getContext().prefs().getInt(KEY_COLOR, 0);
            darkDial = getContext().prefs().getBoolean(KEY_DARK_DIAL, true);
        }

        clockView = new Md3ClockView(activity);
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(
                dp(activity, sizeDp), dp(activity, sizeDp));
        clockLp.gravity = Gravity.CENTER_HORIZONTAL;
        clockView.setLayoutParams(clockLp);

        clockView.setDialTheme(darkDial);
        clockView.setAccentColor(ACCENTS[Math.max(0, Math.min(colorIdx, ACCENTS.length - 1))]);
        clockView.setClockScale(sizeDp / 160f);

        wrapper.addView(clockView);

        // Prefer dedicated plugin_slot_header, or insert before big_search_bar
        int slotId = activity.getResources().getIdentifier("plugin_slot_header", "id", activity.getPackageName());
        View slotView = slotId != 0 ? homeScreenRoot.findViewById(slotId) : null;
        if (slotView instanceof ViewGroup) {
            ((ViewGroup) slotView).removeAllViews();
            ((ViewGroup) slotView).addView(wrapper);
        } else {
            int searchId = activity.getResources().getIdentifier("big_search_bar", "id", activity.getPackageName());
            int insertIndex = -1;
            for (int i = 0; i < container.getChildCount(); i++) {
                if (container.getChildAt(i).getId() == searchId) {
                    insertIndex = i;
                    break;
                }
            }
            if (insertIndex < 0) insertIndex = Math.min(2, container.getChildCount());
            container.addView(wrapper, insertIndex);
        }
    }

    private int dp(Context context, int value) {
        float density = 2.0f;
        if (context != null) {
            density = context.getResources().getDisplayMetrics().density;
        } else if (getContext() != null && getContext().getActivity() != null) {
            density = getContext().getActivity().getResources().getDisplayMetrics().density;
        }
        return Math.round(value * density);
    }

    // =======================================================
    // MD3 CLOCK VIEW WITH SMOOTH TICKING
    // =======================================================
    public static class Md3ClockView extends View {
        private int colorFace           = Color.parseColor("#211F26");
        private int colorRim            = Color.parseColor("#49454F");
        private int colorTicks          = Color.parseColor("#E6E0E9");
        private int colorHourMinute     = Color.parseColor("#E6E0E9");
        private int colorPrimary        = Color.parseColor("#D0BCFF");

        private final Paint paintFace   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintRim    = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintTick   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintHour   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintMinute = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintSecond = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable ticker = new Runnable() {
            @Override
            public void run() {
                invalidate();
                handler.postDelayed(this, 1000);
            }
        };

        public Md3ClockView(Context context) {
            super(context);
            initPaints();
        }

        private void initPaints() {
            paintFace.setStyle(Paint.Style.FILL);
            paintFace.setColor(colorFace);

            paintRim.setStyle(Paint.Style.STROKE);
            paintRim.setColor(colorRim);
            paintRim.setStrokeWidth(2.5f);

            paintTick.setStyle(Paint.Style.STROKE);
            paintTick.setColor(colorTicks);
            paintTick.setStrokeCap(Paint.Cap.ROUND);

            paintHour.setStyle(Paint.Style.STROKE);
            paintHour.setColor(colorHourMinute);
            paintHour.setStrokeCap(Paint.Cap.ROUND);
            paintHour.setStrokeWidth(7f);

            paintMinute.setStyle(Paint.Style.STROKE);
            paintMinute.setColor(colorHourMinute);
            paintMinute.setStrokeCap(Paint.Cap.ROUND);
            paintMinute.setStrokeWidth(4.5f);

            paintSecond.setStyle(Paint.Style.STROKE);
            paintSecond.setColor(colorPrimary);
            paintSecond.setStrokeCap(Paint.Cap.ROUND);
            paintSecond.setStrokeWidth(2f);

            paintCenter.setStyle(Paint.Style.FILL);
            paintCenter.setColor(colorPrimary);
        }

        public void setDialTheme(boolean dark) {
            if (dark) {
                colorFace = Color.parseColor("#211F26");
                colorRim = Color.parseColor("#49454F");
                colorTicks = Color.parseColor("#E6E0E9");
                colorHourMinute = Color.parseColor("#E6E0E9");
            } else {
                colorFace = Color.parseColor("#E7E0EC");
                colorRim = Color.parseColor("#79747E");
                colorTicks = Color.parseColor("#1C1B1F");
                colorHourMinute = Color.parseColor("#1C1B1F");
            }
            paintFace.setColor(colorFace);
            paintRim.setColor(colorRim);
            paintTick.setColor(colorTicks);
            paintHour.setColor(colorHourMinute);
            paintMinute.setColor(colorHourMinute);
            invalidate();
        }

        public void setAccentColor(int color) {
            this.colorPrimary = color;
            paintSecond.setColor(colorPrimary);
            paintCenter.setColor(colorPrimary);
            invalidate();
        }

        public void setClockScale(float scale) {
            paintHour.setStrokeWidth(7f * scale);
            paintMinute.setStrokeWidth(4.5f * scale);
            paintSecond.setStrokeWidth(2f * scale);
            invalidate();
        }

        public void stopTicker() {
            handler.removeCallbacks(ticker);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            handler.removeCallbacks(ticker);
            handler.post(ticker);
        }

        @Override
        protected void onDetachedFromWindow() {
            stopTicker();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.min(cx, cy) * 0.90f;

            // Draw clock base & subtle rim
            canvas.drawCircle(cx, cy, radius, paintFace);
            canvas.drawCircle(cx, cy, radius, paintRim);

            // Draw hour & minute ticks
            for (int i = 0; i < 60; i++) {
                float angle = (float) Math.toRadians(i * 6 - 90);
                boolean isHour = (i % 5 == 0);
                float inner = isHour ? radius * 0.76f : radius * 0.84f;
                float outer = radius * 0.88f;

                float x1 = cx + (float) Math.cos(angle) * inner;
                float y1 = cy + (float) Math.sin(angle) * inner;
                float x2 = cx + (float) Math.cos(angle) * outer;
                float y2 = cy + (float) Math.sin(angle) * outer;

                paintTick.setAlpha(isHour ? 220 : 70);
                paintTick.setStrokeWidth(isHour ? 3.5f : 1.5f);
                canvas.drawLine(x1, y1, x2, y2, paintTick);
            }

            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);

            // Draw Hands with smooth positioning
            drawHand(canvas, cx, cy, radius * 0.50f, (hour + minute / 60f) * 30f - 90f, paintHour);
            drawHand(canvas, cx, cy, radius * 0.72f, (minute + second / 60f) * 6f - 90f, paintMinute);
            drawHand(canvas, cx, cy, radius * 0.82f, second * 6f - 90f, paintSecond);

            // Center Pin
            canvas.drawCircle(cx, cy, 6f, paintCenter);
            Paint innerPin = new Paint(Paint.ANTI_ALIAS_FLAG);
            innerPin.setColor(colorFace);
            canvas.drawCircle(cx, cy, 2.5f, innerPin);
        }

        private void drawHand(Canvas canvas, float cx, float cy, float length, float angleDeg, Paint paint) {
            double rad = Math.toRadians(angleDeg);
            float x = cx + (float) Math.cos(rad) * length;
            float y = cy + (float) Math.sin(rad) * length;
            canvas.drawLine(cx, cy, x, y, paint);
        }
    }
}
