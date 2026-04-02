package com.sumver.kalinba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


import java.lang.reflect.Field;

public class hackvip implements IXposedHookLoadPackage {

    // 目标包名
    private static final String TARGET_PACKAGE = "pet.morning.linkey";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. 过滤目标应用
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }

        XposedBridge.log("=== 灵犀智控破解模块已加载 ===");

        // 2. 核心策略：Hook KeyService 的 onCreate
        hookKeyService(lpparam);
        
        // 3. 辅助策略：监控日志以辅助调试（可选）
        hookLogcat(lpparam);
    }

    private void hookKeyService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // 这里的类名必须完全匹配
            XposedHelpers.findAndHookMethod(
                "pet.morning.linkey.KeyService", 
                lpparam.classLoader,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("检测到 KeyService 启动，正在尝试移除校验...");
                        
                        Object serviceInstance = param.thisObject;
                        
                        // 尝试清空 f2350c 字段 (协程作用域)
                        try {
                            Field scopeField = serviceInstance.getClass().getDeclaredField("f2350c");
                            scopeField.setAccessible(true);
                            scopeField.set(serviceInstance, null); // 设为 null 强制中断协程
                            XposedBridge.log("成功禁用 f2350c，后台校验可能已停止。");
                        } catch (NoSuchFieldException e) {
                            XposedBridge.log("字段 f2350c 未找到，请检查混淆映射。");
                        }
                    }
                }
            );
        } catch (Exception e) {
            XposedBridge.log("Hook KeyService 失败: " + e.getMessage());
        }
    }

    private void hookLogcat(XC_LoadPackage.LoadPackageParam lpparam) {
        // 这是一个调试手段，用于确认校验逻辑是否真的被触发了
        try {
            XposedHelpers.findAndHookMethod(
                "android.util.Log",
                lpparam.classLoader,
                "d",
                String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String tag = (String) param.args[0];
                        String msg = (String) param.args[1];
                        
                        if ("SmartKeyService".equals(tag) && msg != null && msg.contains("VIP")) {
                            XposedBridge.log("⚠️ 警告：检测到 VIP 校验日志输出，说明校验逻辑仍在运行！");
                        }
                    }
                }
            );
        } catch (Exception ignored) {}
    }
}
