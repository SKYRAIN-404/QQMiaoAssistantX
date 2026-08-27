/*
 * =====================================================================
 *  安全与版权声明（SECURITY & COPYRIGHT NOTICE）
 *  ------------------------------------------------------------
 *  本文件属于「喵喵助手·夕兮二改版」，任何形式的分发与再分发，
 *  严禁在本项目（含全部源码、资源、二进制的 native 库及最终 APK）中
 *  植入病毒、木马、后门、勒索软件、挖矿脚本或任何形式的恶意代码。
 *
 *  任何对原「喵喵助手」项目源代码进行恶意篡改并二次分发的行为，
 *  与原作者及「夕兮/SKYRAIN」无关，均由分发者承担全部法律责任。
 *  唯一官方获取渠道：QQ 792413814（请自行核对签名后构建安装）。
 * =====================================================================
 */
/*
 * 喵喵助手魔改版（MiaoAssistantX） - 本文件是「夕兮/SKYRAIN」基于
 * 「喵喵助手」重构与扩展的部分实现。
 *
 * Copyright (C) 2026 夕兮 / SKYRAIN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.example.u7e5f3218e9;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class QQAccessibilityService extends AccessibilityService {
    private static final String PKG_QQ = "com.tencent.mobileqq";
    private static final String PKG_QQI = "com.tencent.mobileqqi";
    private static final String TAG = "QQCatSvc";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private CatConfig cachedConfig;
    private String userOriginal = "";
    private String lastSet = "";
    private boolean processing = false;
    private long lastWriteTime = 0;
    private long silentUntil = 0;   // 写入回显后的静默期，避免改写结果再次触发循环
    private java.util.Set<String> enabledApps = new java.util.HashSet<String>();
    private String lastText = null;  // 上次观测到的输入框全文，用于区分「输入文字」与「仅移动光标」

    /** 当前事件归属的应用包名是否启用本服务 */
    private boolean isEnabledApp(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        if (enabledApps.isEmpty()) {
            // 兜底默认 QQ
            boolean b = PKG_QQ.equals(pkg) || PKG_QQI.equals(pkg);
            if (!b) Log.d(TAG, "[isEnabledApp] enabledApps为空回退QQ，pkg=" + pkg);
            return b;
        }
        boolean b = enabledApps.contains(pkg);
        if (!b) Log.d(TAG, "[isEnabledApp] pkg=" + pkg + " 不在启用列表(" + enabledApps + ")");
        return b;
    }

    /** 刷新启用应用列表（服务连接时/检测到变化时） */
    private void reloadEnabledApps() {
        try { this.enabledApps = AIConfig.load(this).enabledApps; } catch (Exception e) { this.enabledApps = new java.util.HashSet<String>(); }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        String pkg = e.getPackageName() != null ? e.getPackageName().toString() : "";
        // 每次事件轻量刷新（SharedPreferences 有进程内缓存，开销极低），
        // 保证用户改勾选后立即响应，无需重启服务。
        reloadEnabledApps();
        if (!isEnabledApp(pkg)) return;
        int type = e.getEventType();
        if (type == 32) {
            this.processing = false;
            this.userOriginal = "";
            this.lastSet = "";
            this.lastWriteTime = 0L;
            this.lastText = null;
            this.cachedConfig = CatConfig.load(this);
            return;
        }
        if (type == 1) {
            AccessibilityNodeInfo src = e.getSource();
            if (src != null) {
                // 只有点击到「发送类」控件时才兜底处理（防止点击输入框移动光标也误触发）
                try {
                    boolean isSend = false;
                    CharSequence t = src.getText();
                    CharSequence cd = src.getContentDescription();
                    String rid = src.getViewIdResourceName();
                    String s = (t != null ? t.toString() : "") + (cd != null ? cd.toString() : "");
                    if (s.contains("发送") || s.contains("Send") || s.contains("send")
                            || (rid != null && rid.toLowerCase().contains("send"))) {
                        isSend = true;
                    }
                    if (!isSend) { src.recycle(); return; }
                    doProcess(true);
                } catch (Exception ex) { /* ignore */ }
                src.recycle();
                return;
            }
            return;
        }
        if (type == 16) {
            long nowT = System.currentTimeMillis();
            // 静默期：这是 AI/规则写入引发的回显事件，忽略以防无限循环
            if (nowT < this.silentUntil) return;
            CatConfig cfg = this.cachedConfig;
            if (cfg == null) {
                cfg = CatConfig.load(this);
                this.cachedConfig = cfg;
            }
            String mode = cfg.processingMode != null ? cfg.processingMode : CatConfig.MODE_PUNCTUATION;
            if (CatConfig.MODE_REALTIME.equals(mode)) {
                doProcess(false);
                return;
            }
            // —— 标点模式：仅当用户真正【输入了文字】且末尾是标点、光标在末尾时唤醒 ——
            // 输出一次后锁定，直到再次输入新标点才触发；在框内修改/删除/改写
            // 标点之前的内容都不再触发，避免 AI 反复自动填充干扰用户编辑。
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) { Log.d(TAG, "[标点触发] root为null"); return; }
            AccessibilityNodeInfo inp = findInput(root);
            if (inp == null) { Log.d(TAG, "[标点触发] findInput 找不到输入框"); root.recycle(); return; }
            CharSequence cs = inp.getText();
            int selStart = inp.getTextSelectionStart();
            int selEnd = inp.getTextSelectionEnd();
            inp.recycle();
            root.recycle();
            if (cs == null || cs.length() == 0) { Log.d(TAG, "[标点触发] 输入框文本为空"); lastText = ""; return; }
            String fullStr = cs.toString();
            String raw = fullStr.trim();
            // 关键：区分「输入文字」与「仅移动光标/选中」。
            // 若全文内容与上一次观测完全相同，说明用户只是在框内点了位置（移动光标、
            // 框选、没有真正追加/改动字符），此时末尾若恰是前文的标点也不应触发。
            if (lastText != null && lastText.equals(fullStr)) {
                Log.d(TAG, "[标点触发] 内容未变(仅光标移动/选中)，不触发");
                return;
            }
            lastText = fullStr;   // 无论是否触发都更新基线
            if (raw.isEmpty() || !isPunctuationEnding(raw)) { Log.d(TAG, "[标点触发] 末尾非标点 raw=" + raw); return; } // 末尾不是标点 → 不触发
            // 光标在文本中间（例如用户把光标移到前文某个符号前一位做编辑），视为编辑，不触发。
            if (selStart >= 0 && selEnd >= 0 && selStart == selEnd
                    && selStart < fullStr.length()) {
                Log.d(TAG, "[标点触发] 光标在文本中间(" + selStart + "/" + fullStr.length() + ")，视为编辑，不触发");
                return;
            }
            if (raw.equals(this.lastSet)) return;   // 刚刚写回的结果
            Log.d(TAG, "标点(新增)触发: " + raw);
            doProcess(false);
        }
    }

    private boolean isPunctuationEnding(String s) {
        if (s == null || s.isEmpty()) return false;
        char last = s.charAt(s.length() - 1);
        return last == 12290 || last == 65281 || last == '!' || last == 65311 || last == '?' || last == ' ';
    }

    private void doProcess(final boolean isSendClick) {
        if (this.processing) return;
        this.processing = true;
        final AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { this.processing = false; return; }
        AccessibilityNodeInfo inp = findInput(root);
        if (inp == null) { root.recycle(); this.processing = false; return; }
        CharSequence cs = inp.getText();
        if (cs == null || cs.length() == 0) {
            inp.recycle(); root.recycle(); this.processing = false;
            this.userOriginal = ""; this.lastSet = ""; return;
        }
        final String raw = cs.toString().trim();
        inp.recycle();
        if (raw.isEmpty()) {
            root.recycle(); this.processing = false;
            this.userOriginal = ""; this.lastSet = ""; return;
        }

        final CatConfig cfg = (this.cachedConfig != null) ? this.cachedConfig : CatConfig.load(this);
        long now = System.currentTimeMillis();
        if (this.lastWriteTime > 0 && now - this.lastWriteTime < 600 && raw.equals(this.lastSet)) {
            Log.d(TAG, "写入回显跳过");
            this.lastWriteTime = 0L;
            root.recycle(); this.processing = false; return;
        }

        boolean isRealtime = CatConfig.MODE_REALTIME.equals(cfg.processingMode);
        if (this.lastSet.isEmpty() || !raw.startsWith(this.lastSet)) {
            this.userOriginal = stripAll(raw, cfg);
        } else {
            String added = raw.substring(this.lastSet.length());
            this.userOriginal += added;
        }
        if (this.userOriginal.isEmpty()) {
            root.recycle(); this.processing = false; return;
        }

        // 判断是否启用本地大模型
        final AIConfig aiCfg = AIConfig.load(this);
        final boolean useAI = aiCfg.enabled;

        if (useAI) {
            // 异步调用本地大模型
            final long finalLastWrite = this.lastWriteTime;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String result = new AIModifier(null).rewrite(QQAccessibilityService.this, userOriginal);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result == null || result.trim().isEmpty() || result.equals(userOriginal)) {
                                // AI 无返回则回退到规则处理
                                String target = TextProcessor.process(userOriginal, cfg);
                                writeBack(target, raw);
                            } else {
                                writeBack(result, raw);
                            }
                            root.recycle();
                            processing = false;
                        }
                    });
                }
            }).start();
            return;
        }

        String target = TextProcessor.process(this.userOriginal, cfg);
        writeBack(target, raw);
        root.recycle();
        this.processing = false;
    }

    private void writeBack(final String target, final String raw) {
        if (target == null || target.isEmpty()) return;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            AccessibilityNodeInfo inp = findInput(root);
            root.recycle();
            if (inp == null) return;
            boolean ok = setText(inp, target);
            Log.d(TAG, "AI/规则写入: " + target + " (ok=" + ok + ")");
            if (ok) {
                this.lastSet = target;
                this.lastWriteTime = System.currentTimeMillis();
                this.silentUntil = System.currentTimeMillis() + 2000;  // 2 秒静默，防止回显再次触发
            }
            inp.recycle();
        } catch (Exception e) {
            Log.e(TAG, "写入失败", e);
        }
    }

    private String stripAll(String text, CatConfig cfg) {
        if (text == null || text.isEmpty()) return "";
        String result = text;
        String[] emotes = cfg.getActiveEmoticons();
        if (emotes.length == 0) emotes = CatConfig.BUILTIN_EMOTICONS;
        java.util.Arrays.sort(emotes, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) { return b.length() - a.length(); }
        });
        for (String em : emotes) {
            if (em == null || em.isEmpty()) continue;
            int idx;
            while ((idx = result.indexOf(em)) >= 0) {
                int st = (idx > 0 && result.charAt(idx - 1) == ' ') ? idx - 1 : idx;
                result = result.substring(0, st) + result.substring(idx + em.length());
            }
        }
        return result.replaceAll("\\s*[\\p{S}\\p{So}\\p{Sm}\\p{Sk}\\p{P}]{3,}\\s*", " ").trim();
    }

    private AccessibilityNodeInfo findNodeById(AccessibilityNodeInfo n, String id) {
        if (n == null || id == null) return null;
        if (id.equals(n.getViewIdResourceName())) return AccessibilityNodeInfo.obtain(n);
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null) {
                AccessibilityNodeInfo r = findNodeById(c, id);
                c.recycle();
                if (r != null) return r;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo n) {
        if (n == null) return null;
        if (n.isEditable()) return AccessibilityNodeInfo.obtain(n);
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null) {
                AccessibilityNodeInfo r = findEditable(c);
                c.recycle();
                if (r != null) return r;
            }
        }
        return null;
    }

    /**
     * 通用地定位"输入框"节点。
     * 优先通过"可编辑"属性（跨应用通用），若需要可在此按包名做精确 ID 增强。
     */
    private AccessibilityNodeInfo findInput(AccessibilityNodeInfo root) {
        if (root == null) return null;
        // 通用方案：找第一个可编辑节点（绝大多数聊天输入框都标为 editable）
        AccessibilityNodeInfo e = findEditable(root);
        if (e != null) return e;
        // 兜底：找 className 含 EditText 的节点
        return findEditTextClass(root);
    }

    private AccessibilityNodeInfo findEditTextClass(AccessibilityNodeInfo n) {
        if (n == null) return null;
        CharSequence cls = n.getClassName();
        if (cls != null && cls.toString().contains("EditText")) return AccessibilityNodeInfo.obtain(n);
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null) {
                AccessibilityNodeInfo r = findEditTextClass(c);
                c.recycle();
                if (r != null) return r;
            }
        }
        return null;
    }

    private boolean setText(AccessibilityNodeInfo n, String t) {
        if (n == null) return false;
        try {
            Bundle b = new Bundle();
            b.putCharSequence("ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE", t);
            boolean ok = n.performAction(2097152, b);
            if (ok) {
                Bundle a = new Bundle();
                a.putInt("ACTION_ARGUMENT_SELECTION_START_INT", t.length());
                a.putInt("ACTION_ARGUMENT_SELECTION_END_INT", t.length());
                n.performAction(131072, a);
            }
            return ok;
        } catch (Exception e) { return false; }
    }

    @Override
    public void onInterrupt() { this.processing = false; }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        reloadEnabledApps();
        AccessibilityServiceInfo i = new AccessibilityServiceInfo();
        i.eventTypes = 49;
        i.feedbackType = 16;
        i.flags = 81;
        i.notificationTimeout = 50L;
        // 监听所有包，具体过滤在 onAccessibilityEvent 内依据用户勾选的 enabledApps 判断，
        // 这样修改勾选后无需重启服务即可生效。
        i.packageNames = null;
        setServiceInfo(i);
        this.cachedConfig = CatConfig.load(this);
        Log.d(TAG, "服务已连接，启用应用: " + enabledApps);
        // 启动保活前台服务
        try {
            Intent it = new Intent(this, KeepAliveService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(it);
            } else {
                startService(it);
            }
            Log.d(TAG, "已请求启动保活服务");
        } catch (Exception e) {
            Log.e(TAG, "启动保活服务失败", e);
        }
    }
}