package com.example.douyinaweme;

import android.app.Application;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DouYinInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) {
        if (!lpp.packageName.equals("com.ss.android.ugc.aweme")) {
            return;
        }

        XposedBridge.log("[DouyinAweme] Module injected into Douyin");

        XposedHelpers.findAndHookMethod(
                Application.class,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Application hostApp = (Application) param.thisObject;
                        ClassLoader cl = hostApp.getClassLoader();

                        XposedBridge.log("[DouyinAweme] Application ready, starting hooks...");

                        DumpReceiver.register(hostApp);
                        HookManager.hookGetAweme(cl);
                        HookManager.hookLongPress(cl);
                    }
                }
        );
    }
}
