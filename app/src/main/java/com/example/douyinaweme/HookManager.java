package com.example.douyinaweme;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Space;
import android.widget.TextView;

import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HookManager {

    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final String VIDEO_HOLDER_CLASS = "com.ss.android.ugc.aweme.feed.adapter.VideoViewHolder";
    private static final String LONG_PRESS_CLASS = "com.ss.android.ugc.aweme.feed.ui.LongPressLayout";

    private static Object currentAweme;

    private static final WeakHashMap<View, Runnable> longPressTasks = new WeakHashMap<>();

    public static Object getCurrentAweme() {
        return currentAweme;
    }

    public static void hookGetAweme(ClassLoader cl) {
        try {
            Class<?> vhClass = cl.loadClass(VIDEO_HOLDER_CLASS);
            for (java.lang.reflect.Method m : vhClass.getDeclaredMethods()) {
                if (m.getName().equals("getAweme")) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            Object aweme = p.getResult();
                            if (aweme != null) currentAweme = aweme;
                        }
                    });
                    XposedBridge.log("[DouyinAweme] Hooked: getAweme()");
                }
            }
        } catch (ClassNotFoundException e) {
            XposedBridge.log("[DouyinAweme] VideoViewHolder not found: " + e.getMessage());
        }
    }

    public static void hookLongPress(ClassLoader cl) {
        try {
            Class<?> lpClass = cl.loadClass(LONG_PRESS_CLASS);
            XposedHelpers.findAndHookMethod(lpClass, "onTouchEvent", MotionEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        View view = (View) p.thisObject;
                        MotionEvent event = (MotionEvent) p.args[0];
                        int action = event.getAction();

                        if (action == MotionEvent.ACTION_DOWN) {
                            Runnable task = longPressTasks.get(view);
                            if (task != null) view.removeCallbacks(task);
                            task = () -> showMenu(view);
                            longPressTasks.put(view, task);
                            view.postDelayed(task, ViewConfiguration.getLongPressTimeout());
                        } else if (action == MotionEvent.ACTION_UP
                                || action == MotionEvent.ACTION_CANCEL) {
                            Runnable task = longPressTasks.remove(view);
                            if (task != null) view.removeCallbacks(task);
                        }
                    }
                });
            XposedBridge.log("[DouyinAweme] Hooked: LongPressLayout.onTouchEvent");
        } catch (ClassNotFoundException e) {
            XposedBridge.log("[DouyinAweme] LongPressLayout not found: " + e.getMessage());
        }
    }

    private static Activity getActivity(Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) return (Activity) ctx;
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    /** 主菜单 */
    private static void showMenu(View view) {
        try {
            Activity activity = getActivity(view.getContext());
            if (activity == null) return;

            PopupWindow[] menuRef = new PopupWindow[1];
            PopupWindow menu = buildPopup(activity, menuContent(activity,
                    () -> { menuRef[0].dismiss(); showDeveloperPanel(activity); },
                    () -> menuRef[0].dismiss()));
            menuRef[0] = menu;
            showWithOverlay(menu, activity);
            XposedBridge.log("[DouyinAweme] Menu shown");
        } catch (Exception e) {
            XposedBridge.log("[DouyinAweme] Failed to show menu: " + e.getMessage());
        }
    }

    /** 开发者面板 */
    private static void showDeveloperPanel(Activity activity) {
        boolean d = dark(activity);
        int bg = d ? 0xFF242424 : 0xFFFFFFFF;

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(32, 32, 32, 32);
        content.setBackgroundColor(bg);

        TextView title = new TextView(activity);
        title.setText("Developer Options");
        title.setTextColor(d ? 0xFFEEEEEE : 0xFF111111);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        content.addView(title);

        Space space = new Space(activity);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 24));
        content.addView(space);

        content.addView(menuBtn(activity, "Dump Aweme Structure", () -> {
            Intent intent = new Intent("com.knownniu.douyinaweme.DUMP_AWEME");
            intent.setPackage("com.ss.android.ugc.aweme");
            activity.sendBroadcast(intent);
            TextView toast = new TextView(activity);
            toast.setText("Dump sent.");
            toast.setTextColor(0xFF4CAF50);
            toast.setGravity(Gravity.CENTER);
            toast.setPadding(0, 16, 0, 0);
            content.addView(toast);
        }));

        PopupWindow panel = buildPopup(activity, content);
        showWithOverlay(panel, activity);
    }

    private static boolean dark(Activity a) {
        return (a.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static GradientDrawable roundedBg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(20f);
        return d;
    }

    private static PopupWindow buildPopup(Activity a, View content) {
        PopupWindow pw = new PopupWindow(a);
        pw.setContentView(content);
        int w = (int) (a.getResources().getDisplayMetrics().widthPixels * 0.80);
        pw.setWidth(w);
        pw.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        pw.setBackgroundDrawable(roundedBg(dark(a) ? 0xFF242424 : 0xFFFFFFFF));
        pw.setElevation(24);
        pw.setFocusable(true);
        pw.setAnimationStyle(android.R.style.Animation_Dialog);
        return pw;
    }

    private static void showWithOverlay(PopupWindow pw, Activity a) {
        View overlay = new View(a);
        overlay.setBackgroundColor(0x88000000);
        overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setOnClickListener(v -> pw.dismiss());
        ViewGroup root = a.findViewById(android.R.id.content);
        root.addView(overlay);
        pw.setOnDismissListener(() -> root.removeView(overlay));
        pw.showAtLocation(root, Gravity.CENTER, 0, 0);
    }

    private static LinearLayout menuContent(Activity a, Runnable onDev, Runnable onCancel) {
        boolean d = dark(a);
        int tc = d ? 0xFFEEEEEE : 0xFF111111;
        int sub = d ? 0xFF888888 : 0xFF999999;
        int line = d ? 0xFF333333 : 0xFFE8E8E8;

        LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);

        // 标题
        TextView title = new TextView(a);
        title.setText("DouyinAweme");
        title.setTextColor(tc);
        title.setTextSize(17);
        title.setGravity(Gravity.CENTER);
        title.setPadding(32, 28, 32, 18);
        content.addView(title);

        // 菜单区
        LinearLayout menuArea = new LinearLayout(a);
        menuArea.setOrientation(LinearLayout.VERTICAL);
        menuArea.setPadding(24, 12, 24, 12);

        menuArea.addView(menuBtn(a, "Developer Options", onDev));
        menuArea.addView(menuBtn(a, "Cancel", onCancel));

        content.addView(menuArea);

        // 分割线
        View divider = new View(a);
        divider.setBackgroundColor(line);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        content.addView(divider);

        // 底部署名
        TextView footer = new TextView(a);
        footer.setText("Developer: Knownniu");
        footer.setTextColor(sub);
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(32, 14, 32, 18);
        content.addView(footer);

        return content;
    }

    private static TextView menuBtn(Activity a, String text, Runnable onClick) {
        boolean d = dark(a);
        int btnBg = d ? 0xFF3A3A3A : 0xFFF0F0F0;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(btnBg);
        bg.setCornerRadius(14f);

        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextColor(d ? 0xFFFFFFFF : 0xFF111111);
        tv.setTextSize(15);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(28, 36, 28, 36);
        tv.setBackground(bg);
        tv.setClickable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 4, 0, 4);
        tv.setLayoutParams(lp);

        if (onClick != null) tv.setOnClickListener(v -> onClick.run());
        return tv;
    }
}
