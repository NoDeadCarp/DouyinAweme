package com.example.douyinaweme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import de.robv.android.xposed.XposedBridge;

public class DumpReceiver {

    private static final String ACTION_DUMP_AWEME = "com.knownniu.douyinaweme.DUMP_AWEME";

    public static void register(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (ACTION_DUMP_AWEME.equals(intent.getAction())) {
                    Object aweme = HookManager.getCurrentAweme();
                    if (aweme != null) {
                        XposedBridge.log("[DouyinAweme] Broadcast -> dump");
                        AwemeDumper.dump(aweme, "/sdcard/Download/DouyinAweme/aweme_dump.json");
                    } else {
                        XposedBridge.log("[DouyinAweme] No Aweme cached, scroll a video first");
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_DUMP_AWEME);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
        XposedBridge.log("[DouyinAweme] Dump receiver registered");
    }
}
