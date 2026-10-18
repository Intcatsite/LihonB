package space.lihont.plugin.boykisser;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import space.lihont.browser.plugin.LihonPlugin;
import space.lihont.browser.plugin.PluginContext;

public class BoykisserPlugin extends LihonPlugin {

    private static final String GIF_URL = "https://i.redd.it/7xkox81t79zg1.gif";
    private static final String PET_URL = "https://media3.giphy.com/media/v1.Y2lkPTZjMDliOTUycDQ5dzQybzgwajJzZXFiODlwNnU0c2g4eDN5dGNhc3Jkb2pwaW81MCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/FywOYzrcOVjtY1ORy4/200.gif";
    private static final String SND_URL = "https://www.myinstants.com/media/sounds/boykisser-meow.mp3";

    private static final long ANIM_MS = 1250L;
    private static final long PET_ANIM_MS = 2200L;
    private static final int DEFAULT_SIZE_DP = 140;

    private ImageView view;
    private ImageView petView;
    private AnimatedImageDrawable drawable;
    private AnimatedImageDrawable petDrawable;
    private MediaPlayer player;

    private File gifFile;
    private File petFile;
    private File soundFile;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean animating = false;
    private volatile int petSession = 0;
    private Activity hostActivity;

    @Override
    public void onStart(PluginContext ctx) {
        initSettings(ctx);

        Activity activity = ctx.getActivity();
        this.hostActivity = activity;
        File cacheDir = new File(activity.getFilesDir(), "boykisser_cache");
        cacheDir.mkdirs();
        gifFile = new File(cacheDir, "boykisser.gif");
        petFile = new File(cacheDir, "pet.gif");
        soundFile = new File(cacheDir, "meow.mp3");

        Executors.newSingleThreadExecutor().execute(() -> {
            downloadIfMissing(GIF_URL, gifFile);
            downloadIfMissing(PET_URL, petFile);
            downloadIfMissing(SND_URL, soundFile);
            ui.post(() -> attach(ctx));
        });
    }

    private void initSettings(PluginContext ctx) {
        ctx.addSettingsSection("Внешний вид Boykisser");
        ctx.addSettingsSlider("Размер персонажа", "Размер персонажа на экране", "pref_size", 40, 300, DEFAULT_SIZE_DP);
        ctx.addSettingsSlider("Прозрачность", "Видимость в процентах", "pref_alpha", 10, 100, 100);
        ctx.addSettingsSection("Интерактивность");
        ctx.addSettingsToggle("Звук мяуканья", "Воспроизводить мяуканье при нажатии", "pref_sound", true);
        ctx.addSettingsToggle("Перетаскивание", "Разрешить двигать по всему экрану", "pref_drag", true);
        ctx.addSettingsToggle("Поглаживание головы", "Анимация руки при поглаживании головы", "pref_pet", true);
    }

    private void downloadIfMissing(String urlStr, File dest) {
        if (dest.exists() && dest.length() > 0) return;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 LihonB/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
        } catch (Exception ignored) {}
    }

    private void attach(PluginContext ctx) {
        Activity activity = ctx.getActivity();
        if (activity == null || activity.isFinishing()) return;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        if (decor == null) return;

        detach();

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        float density = dm.density;

        SharedPreferences prefs = ctx.prefs();
        int sizeDp = prefs.getInt("pref_size", DEFAULT_SIZE_DP);
        int alphaPercent = prefs.getInt("pref_alpha", 100);
        int sizePx = (int) (sizeDp * density);

        int savedRightDp = prefs.getInt("pos_right_dp", 16);
        int savedBottomDp = prefs.getInt("pos_bottom_dp", 80);
        int rightPx = (int) (savedRightDp * density);
        int bottomPx = (int) (savedBottomDp * density);

        // Main Boykisser View
        view = new ImageView(activity);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setAlpha(alphaPercent / 100.0f);
        view.setClickable(true);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.rightMargin = rightPx;
        lp.bottomMargin = bottomPx;
        view.setLayoutParams(lp);

        // Load static first frame (DO NOT start animation on load!)
        resetToFirstFrame();

        // Pet Overlay View (hand scratching head)
        petView = new ImageView(activity);
        petView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int petOffsetPx = (int) (28 * density);
        FrameLayout.LayoutParams petLp = new FrameLayout.LayoutParams(sizePx, sizePx);
        petLp.gravity = Gravity.BOTTOM | Gravity.END;
        petLp.rightMargin = rightPx;
        petLp.bottomMargin = bottomPx + petOffsetPx;
        petView.setLayoutParams(petLp);
        petView.setAlpha(alphaPercent / 100.0f);
        petView.setVisibility(View.GONE);
        petView.setClickable(false);

        TouchListener touchListener = new TouchListener(ctx, view, petView, lp, density, petOffsetPx);
        view.setOnTouchListener(touchListener);
        petView.setOnTouchListener(touchListener);

        decor.addView(view);
        decor.addView(petView);
    }

    private void resetToFirstFrame() {
        if (gifFile == null || !gifFile.exists() || view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source src = ImageDecoder.createSource(gifFile);
                Drawable d = ImageDecoder.decodeDrawable(src);
                if (d instanceof AnimatedImageDrawable) {
                    drawable = (AnimatedImageDrawable) d;
                    // Note: Do NOT call drawable.start()! It remains on frame 0.
                    view.setImageDrawable(drawable);
                }
            } catch (Exception ignored) {}
        }
    }

    private void playAnimation() {
        if (drawable == null) return;
        try {
            drawable.start();
            ui.postDelayed(() -> {
                try {
                    drawable.stop();
                } catch (Throwable ignored) {}
                resetToFirstFrame();
            }, ANIM_MS);
        } catch (Throwable ignored) {}
    }

    private long playMeow() {
        if (getContext() == null) return 0;
        boolean soundEnabled = getContext().prefs().getBoolean("pref_sound", true);
        if (!soundEnabled || soundFile == null || !soundFile.exists()) {
            return 0;
        }

        try {
            if (player != null) {
                try {
                    if (player.isPlaying()) player.stop();
                    player.release();
                } catch (Throwable ignored) {}
                player = null;
            }

            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(soundFile.getAbsolutePath());
            mp.prepare();
            int dur = mp.getDuration();
            int playMs = 1250;
            int seekMs = 0;

            if (dur > 0) {
                int half = dur / 2;
                if (Math.random() < 0.5) {
                    seekMs = half;
                    playMs = Math.max(dur - half, 250);
                } else {
                    seekMs = 0;
                    playMs = Math.max(half - 100, 250);
                }
            }

            mp.seekTo(seekMs);
            mp.start();
            player = mp;

            final int stopDelay = playMs;
            ui.postDelayed(() -> {
                try {
                    if (player == mp) {
                        if (mp.isPlaying()) {
                            mp.stop();
                        }
                        mp.release();
                        player = null;
                    }
                } catch (Throwable ignored) {}
            }, stopDelay);

            return stopDelay;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void onTap() {
        // Debounce: ignore repeated taps while previous animation/sound is still playing
        if (animating) return;
        animating = true;

        long soundDuration = playMeow();
        playAnimation();

        // Bounce effect
        if (view != null) {
            view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(120).withEndAction(() -> {
                if (view != null) {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
            }).start();
        }

        long lockDuration = Math.max(ANIM_MS, soundDuration);
        ui.postDelayed(() -> animating = false, lockDuration);
    }

    private void onPet() {
        showPetOverlay();
        onTap();
    }

    private void showPetOverlay() {
        if (petView == null || petFile == null || !petFile.exists()) return;
        petSession++;
        final int currentSession = petSession;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source src = ImageDecoder.createSource(petFile);
                Drawable d = ImageDecoder.decodeDrawable(src);
                if (d instanceof AnimatedImageDrawable) {
                    petDrawable = (AnimatedImageDrawable) d;
                    petView.setImageDrawable(petDrawable);
                    petView.setVisibility(View.VISIBLE);
                    petDrawable.start();

                    ui.postDelayed(() -> hidePet(currentSession), PET_ANIM_MS);
                }
            } catch (Exception ignored) {}
        }
    }

    private void hidePet(int session) {
        if (session != petSession) return;
        if (petView != null) {
            petView.setVisibility(View.GONE);
        }
        if (petDrawable != null) {
            try {
                petDrawable.stop();
            } catch (Throwable ignored) {}
            petDrawable = null;
        }
    }

    private void hidePetNow() {
        petSession++;
        hidePet(petSession);
    }

    @Override
    public void onSettingsChanged(String key) {
        if (view == null || getContext() == null) return;
        SharedPreferences prefs = getContext().prefs();
        Activity activity = getContext().getActivity();
        if (activity == null) return;

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();

        if ("pref_size".equals(key)) {
            int sizeDp = prefs.getInt("pref_size", DEFAULT_SIZE_DP);
            int sizePx = (int) (sizeDp * dm.density);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                lp.width = sizePx;
                lp.height = sizePx;
                view.setLayoutParams(lp);
            }
            if (petView != null) {
                ViewGroup.LayoutParams petLp = petView.getLayoutParams();
                if (petLp != null) {
                    petLp.width = sizePx;
                    petLp.height = sizePx;
                    petView.setLayoutParams(petLp);
                }
            }
        } else if ("pref_alpha".equals(key)) {
            int alphaPercent = prefs.getInt("pref_alpha", 100);
            view.setAlpha(alphaPercent / 100.0f);
            if (petView != null) {
                petView.setAlpha(alphaPercent / 100.0f);
            }
        }
    }

    private void detach() {
        try {
            if (view != null && view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            if (petView != null && petView.getParent() instanceof ViewGroup) {
                ((ViewGroup) petView.getParent()).removeView(petView);
            }
        } catch (Throwable ignored) {}
        view = null;
        petView = null;
        drawable = null;
        petDrawable = null;
    }

    @Override
    public void onStop() {
        detach();
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
                player.release();
            } catch (Throwable ignored) {}
            player = null;
        }
    }

    private class TouchListener implements View.OnTouchListener {
        private final PluginContext ctx;
        private final ImageView hostView;
        private final ImageView pet;
        private final FrameLayout.LayoutParams lp;
        private final float density;
        private final int petOffsetPx;

        private boolean dragging = false;
        private boolean topZone = false;
        private boolean petTriggered = false;
        private int petDirChanges = 0;
        private int petLastDir = 0;
        private float petLastX = 0f;

        private float startX, startY;
        private int startRight, startBottom;

        TouchListener(PluginContext ctx, ImageView hostView, ImageView pet,
                      FrameLayout.LayoutParams lp, float density, int petOffsetPx) {
            this.ctx = ctx;
            this.hostView = hostView;
            this.pet = pet;
            this.lp = lp;
            this.density = density;
            this.petOffsetPx = petOffsetPx;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                startX = event.getRawX();
                startY = event.getRawY();
                startRight = lp.rightMargin;
                startBottom = lp.bottomMargin;

                dragging = false;
                petTriggered = false;
                petDirChanges = 0;
                petLastDir = 0;
                petLastX = event.getRawX();

                // Top 42% is the head/pet zone
                topZone = event.getY() < (hostView.getHeight() * 0.42f);
                return true;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                boolean pettingEnabled = ctx.prefs().getBoolean("pref_pet", true);
                boolean dragEnabled = ctx.prefs().getBoolean("pref_drag", true);

                if (topZone && pettingEnabled) {
                    float verticalDrift = Math.abs(event.getRawY() - startY);
                    if (verticalDrift <= 40f * density) {
                        float dx = event.getRawX() - petLastX;
                        if (Math.abs(dx) >= 8f * density) {
                            int dir = dx > 0 ? 1 : -1;
                            if (petLastDir != 0 && dir != petLastDir) {
                                petDirChanges++;
                            }
                            petLastDir = dir;
                            petLastX = event.getRawX();

                            if (petDirChanges >= 3) {
                                petTriggered = true;
                                petDirChanges = 0;
                                onPet();
                            }
                        }
                        return true;
                    }
                }

                if (dragEnabled) {
                    float diffX = event.getRawX() - startX;
                    float diffY = event.getRawY() - startY;

                    if (!dragging && (Math.abs(diffX) > 10f * density || Math.abs(diffY) > 10f * density)) {
                        dragging = true;
                    }

                    if (dragging) {
                        int newRight = Math.max(0, startRight - (int) diffX);
                        int newBottom = Math.max(0, startBottom - (int) diffY);
                        lp.rightMargin = newRight;
                        lp.bottomMargin = newBottom;
                        hostView.setLayoutParams(lp);

                        if (pet != null) {
                            FrameLayout.LayoutParams petLp = (FrameLayout.LayoutParams) pet.getLayoutParams();
                            if (petLp != null) {
                                petLp.rightMargin = newRight;
                                petLp.bottomMargin = newBottom + petOffsetPx;
                                pet.setLayoutParams(petLp);
                            }
                        }
                    }
                }
                return true;
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (topZone) {
                    if (petTriggered) {
                        hidePetNow();
                    } else {
                        onTap();
                    }
                } else {
                    if (dragging) {
                        // Persist position
                        ctx.prefs().edit()
                            .putInt("pos_right_dp", (int) (lp.rightMargin / density))
                            .putInt("pos_bottom_dp", (int) (lp.bottomMargin / density))
                            .apply();
                    } else {
                        onTap();
                    }
                }
                return true;
            }

            return false;
        }
    }
}
