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
 *  唯一官方获取渠道：QQ 792413184（请自行核对签名后构建安装）。
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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private CheckBox cbAppend;
    private CheckBox cbEmoticon;
    private CatConfig config;
    private AIConfig aiConfig;
    private EditText etAppendText;
    private EditText etCustomEmoticons;
    private EditText etRules;
    private CheckBox rbPunctuation;
    private CheckBox rbRealtime;
    private CheckBox cbAI;
    private EditText etAIUrl, etAIModel, etAIKey, etAIPersona, etAITemp, etAITimeout;
    private EditText etGgufPath;
    private CompoundButton rbOnline, rbLocal;
    private TextView tvOnline, tvLocal;
    private Button btnLoad;
    private static final int REQ_PICK_GGUF = 9911;
    private TextView statusText;
    private TextView aiStatus;
    private Button toggleButton;

    private static final int START = 0xFF6C5CE7;
    private static final int END = 0xFFFD6E9C;
    private static final int BG = 0xFF14152A;
    private static final int CARD = 0xFF1F2140;
    private static final int CARD_ALT = 0xFF272A52;
    private static final int TXT = 0xFFEDEDF5;
    private static final int TXT2 = 0xFFA0A2C0;
    private static final int ACC = 0xFF7C6CF6;
    private static final int GOOD = 0xFF4CD6A8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try { this.config = CatConfig.load(this); } catch (Exception e) { this.config = new CatConfig(); }
        try { this.aiConfig = AIConfig.load(this); } catch (Exception e) { this.aiConfig = new AIConfig(); }
        // 确保 AI 配置的 SharedPreferences 文件存在（首次进入即建立 ai_config.xml 基线，
        // 防止 saveConfig 因 prefs 不存在/时序问题导致 AI 设置“消失”）。
        try { this.aiConfig.save(this); } catch (Exception e) { }

        // 临时诊断：启动即触发本地引擎加载，便于抓取真实错误
        new Thread(new Runnable() { public void run() {
            boolean ok = LLamaEngine.isAvailable();
            android.util.Log.i("LLamaEngine", "启动诊断 isAvailable=" + ok);
        } }).start();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(root, new ViewGroup.LayoutParams(-1, -2));

        // ---- 顶部渐变标题 ----
        TextView header = new TextView(this);
        header.setText("\uD83D\uDC31 喵喵改写助手");
        header.setTextSize(26f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Color.WHITE);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 56, 0, 8);
        Shader sh = new LinearGradient(0, 0, 900, 0, new int[]{START, END}, null, Shader.TileMode.CLAMP);
        header.getPaint().setShader(sh);
        root.addView(header);
        TextView sub = new TextView(this);
        sub.setText("基于无障碍的 QQ 智能改写 · 支持本地大模型");
        sub.setTextSize(13f);
        sub.setTextColor(TXT2);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 8, 0, 28);
        root.addView(sub);

        // ---- 服务状态卡片 ----
        LinearLayout statusCard = card();
        statusText = new TextView(this);
        statusText.setTextSize(16f);
        statusText.setTextColor(TXT);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 4, 0, 10);
        statusCard.addView(statusText);
        toggleButton = new Button(this);
        toggleButton.setTextSize(15f);
        toggleButton.setTextColor(Color.WHITE);
        toggleButton.setBackgroundColor(ACC);
        toggleButton.setAllCaps(false);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, dp(52));
        btnLp.setMargins(0, 8, 0, 0);
        toggleButton.setLayoutParams(btnLp);
        toggleButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { openAccessibilitySettings(); } });
        statusCard.addView(toggleButton);
        aiStatus = new TextView(this);
        aiStatus.setTextSize(4f);
        aiStatus.setTextSize(12f);
        aiStatus.setPadding(0, 12, 0, 0);
        aiStatus.setTextColor(TXT2);
        aiStatus.setGravity(Gravity.CENTER);
        statusCard.addView(aiStatus);
        root.addView(cardWrap(statusCard));

        // ---- AI 大模型区块 ----
        root.addView(sectionTitle("\u2728 AI 智能改写"));
        root.addView(cardWrap(aiPanel()));

        // ---- 处理模式 ----
        root.addView(sectionTitle("\u2699\uFE0F 处理模式"));
        LinearLayout modeCard = card();
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        rbPunctuation = check(" 标点触发 (推荐)", CatConfig.MODE_PUNCTUATION.equals(config.processingMode));
        rbPunctuation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton b, boolean c) { if (c) rbRealtime.setChecked(false); } });
        rbRealtime = check(" 实时处理", CatConfig.MODE_REALTIME.equals(config.processingMode));
        rbRealtime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton b, boolean c) { if (c) rbPunctuation.setChecked(false); } });
        modeRow.addView(rbPunctuation);
        modeRow.addView(rbRealtime);
        modeCard.addView(modeRow);
        TextView modeHint = hint("标点触发：在句号/叹号等标点后自动改写\n实时处理：每输入即实时改写");
        modeCard.addView(modeHint);
        root.addView(cardWrap(modeCard));

        // ---- 断句追加 ----
        root.addView(sectionTitle("\uD83C\uDF3B 断句与颜文字"));
        LinearLayout funCard = card();
        cbAppend = check(" 断句追加", config.enableAppend);
        funCard.addView(cbAppend);
        etAppendText = new EditText(this);
        styleInput(etAppendText, "追加内容（默认：喵）", config.appendText != null ? config.appendText : "喵");
        funCard.addView(etAppendText);
        cbEmoticon = check(" 句末随机颜文字", config.enableRandomEmoticon);
        funCard.addView(cbEmoticon);
        funCard.addView(space(6));
        TextView emojiTitle = new TextView(this);
        emojiTitle.setText("自定义颜文字（每行一个，留空用内置）");
        emojiTitle.setTextSize(13f);
        emojiTitle.setTextColor(TXT2);
        funCard.addView(emojiTitle);
        etCustomEmoticons = new EditText(this);
        styleArea(etCustomEmoticons, "例如: (=^w^=) 等", joinLines(config.customEmoticons), 3);
        funCard.addView(etCustomEmoticons);
        root.addView(cardWrap(funCard));

        // ---- 文本替换规则 ----
        root.addView(sectionTitle("\uD83D\uDD01 替换规则"));
        LinearLayout ruleCard = card();
        TextView ruleHint = hint("每行一条，按顺序应用：原词=替换词\n（支持 ＝ 全角等号 / →）\n例：我=本喵 / 你=主人");
        ruleCard.addView(ruleHint);
        etRules = new EditText(this);
        styleArea(etRules, "我=本喵\n你=主人", CatConfig.rulesToString(config.rules), 5);
        ruleCard.addView(etRules);
        root.addView(cardWrap(ruleCard));

        // ---- 启用应用（不局限于 QQ） ----
        root.addView(sectionTitle("\uD83D\uDCF1 启用应用"));
        LinearLayout appCard = card();
        appCard.addView(hint("选择需要本服务生效的聊天应用（可多选），不再局限于 QQ。\n保存后重新打开目标应用并输入标点即可生效。"));
        Button pickBtn = new Button(this);
        pickBtn.setText("\uD83D\uDCC5 选择应用（当前 " + (aiConfig.enabledApps != null ? aiConfig.enabledApps.size() : 0) + " 个）");
        pickBtn.setTextSize(15f); pickBtn.setTextColor(Color.WHITE);
        pickBtn.setBackgroundColor(0xFF2E9E7E); pickBtn.setAllCaps(false);
        pickBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
        pickBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showAppPicker(); } });
        appCard.addView(pickBtn);
        root.addView(cardWrap(appCard));

        // ---- 保存 + 测试 ----
        root.addView(space(10));
        Button saveBtn = new Button(this);
        saveBtn.setText("\uD83D\uDCBE 保存全部设置");
        saveBtn.setTextSize(16f);
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setBackgroundColor(ACC);
        saveBtn.setAllCaps(false);
        saveBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        saveBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { saveConfig(); } });
        root.addView(cardWrap(saveBtn));
        Button testBtn = new Button(this);
        testBtn.setText("\uD83D\uDC0C 测试当前配置");
        testBtn.setTextSize(15f);
        testBtn.setTextColor(ACC);
        testBtn.setBackgroundColor(CARD_ALT);
        testBtn.setAllCaps(false);
        testBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50)));
        testBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showTestDialog(); } });
        root.addView(cardWrap(testBtn));

        TextView footer = new TextView(this);
        footer.setText("喵喵助手 · 本喵永远守护你的小尾巴 \uD83D\uDC49\uD83D\uDC46");
        footer.setTextSize(12f);
        footer.setTextColor(TXT2);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 28, 0, 48);
        root.addView(footer);

        setContentView(scrollView);

        // 界面就绪后，延迟弹出首次启动免责声明（避免在 onCreate 早期弹窗被覆盖/闪烁）
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() { showDisclaimerIfNeeded(); }
        }, 600);
    }

    private LinearLayout aiPanel() {
        LinearLayout card = card();
        cbAI = check(" 启用 AI 智能改写", aiConfig.enabled);
        card.addView(cbAI);
        TextView tip = hint("AI 改写支持两种通道：\n· 在线API：调用 OpenAI 兼容在线接口（如 DeepSeek / OpenAI）\n· 本地模型：直接加载手机里的 .gguf 小模型文件，离线推理不走端口。");
        card.addView(tip);

        // ---- 模式选择 ----
        card.addView(fieldLabel("改写通道"));
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        rbOnline = new com.example.u7e5f3218e9.RadioButtonCompat(this);
        rbOnline.setText("在线 API"); rbOnline.setTextSize(15f); rbOnline.setTextColor(TXT);
        rbLocal = new com.example.u7e5f3218e9.RadioButtonCompat(this);
        rbLocal.setText("本地模型"); rbLocal.setTextSize(15f); rbLocal.setTextColor(TXT);
        rbOnline.setChecked(!aiConfig.isLocal());
        rbLocal.setChecked(aiConfig.isLocal());
        rbOnline.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            rbLocal.setChecked(false); applyModeUI(); } });
        rbLocal.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            rbOnline.setChecked(false); applyModeUI(); } });
        modeRow.addView(rbOnline, new LinearLayout.LayoutParams(0, -2, 1));
        modeRow.addView(rbLocal, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(modeRow);

        // ---- 在线 API 字段 ----
        tvOnline = hint("在线 API：填写 OpenAI 兼容接口地址、模型和密钥。");
        card.addView(tvOnline);
        card.addView(fieldLabel("接口地址"));
        etAIUrl = new EditText(this);
        styleInput(etAIUrl, "例如 https://api.deepseek.com/v1", aiConfig.baseUrl);
        card.addView(etAIUrl);
        card.addView(fieldLabel("模型名称"));
        etAIModel = new EditText(this);
        styleInput(etAIModel, "例如 deepseek-chat", aiConfig.model);
        card.addView(etAIModel);
        card.addView(fieldLabel("API Key"));
        etAIKey = new EditText(this);
        styleInput(etAIKey, "在线 API 的密钥", aiConfig.apiKey);
        card.addView(etAIKey);

        // ---- 本地模型字段 ----
        tvLocal = hint("本地模型：选择手机里的 .gguf 模型文件，离线推理。");
        card.addView(tvLocal);
        card.addView(fieldLabel("模型文件路径"));
        etGgufPath = new EditText(this);
        styleInput(etGgufPath, "例如 /sdcard/Download/qwen1.5.gguf", aiConfig.ggufPath);
        card.addView(etGgufPath);
        Button chooseBtn = new Button(this);
        chooseBtn.setText("\uD83D\uDCC2 选择本地模型文件 (.gguf)");
        chooseBtn.setTextSize(15f); chooseBtn.setTextColor(Color.WHITE);
        chooseBtn.setBackgroundColor(0xFF2E9E7E); chooseBtn.setAllCaps(false);
        chooseBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
        chooseBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pickModelFile(); } });
        card.addView(chooseBtn);
        btnLoad = new Button(this);
        btnLoad.setText("\uD83D\uDCBE 加载本地模型");
        btnLoad.setTextSize(15f); btnLoad.setTextColor(Color.WHITE);
        btnLoad.setBackgroundColor(ACC); btnLoad.setAllCaps(false);
        btnLoad.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
        btnLoad.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { loadSelectedModel(); } });
        card.addView(btnLoad);

        // ---- 共用：人设 / 温度 / 超时 ----
        card.addView(fieldLabel("人设提示词（决定改写风格）"));
        etAIPersona = new EditText(this);
        styleArea(etAIPersona, "例：你是可爱的猫娘，把「我」改成「本喵」...", aiConfig.persona, 5);
        card.addView(etAIPersona);
        card.addView(fieldLabel("温度（0~2，越大越有创意）"));
        etAITemp = new EditText(this);
        styleInput(etAITemp, "0.8", String.valueOf(aiConfig.temperature));
        card.addView(etAITemp);
        card.addView(fieldLabel("超时（秒）"));
        etAITimeout = new EditText(this);
        styleInput(etAITimeout, "60", String.valueOf(aiConfig.timeoutSec));
        card.addView(etAITimeout);

        applyModeUI();
        return card;
    }

    /** 根据所选通道显示/隐藏对应字段 */
    private void applyModeUI() {
        boolean local = (rbLocal != null && rbLocal.isChecked());
        int on = View.VISIBLE, off = View.GONE;
        if (tvOnline != null) tvOnline.setVisibility(local ? off : on);
        if (etAIUrl != null) etAIUrl.setVisibility(local ? off : on);
        if (etAIModel != null) etAIModel.setVisibility(local ? off : on);
        if (etAIKey != null) etAIKey.setVisibility(local ? off : on);
        if (tvLocal != null) tvLocal.setVisibility(local ? on : off);
        if (etGgufPath != null) etGgufPath.setVisibility(local ? on : off);
    }

    /** 打开系统文件选择器挑选 .gguf 模型 */
    private void pickModelFile() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_PICK_GGUF);
        } catch (Exception e) { toast("无法打开文件选择器"); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK_GGUF && res == RESULT_OK && data != null) {
            try {
                android.net.Uri uri = data.getData();
                String path = uriPath(uri);
                if (path != null && etGgufPath != null) {
                    etGgufPath.setText(path);
                    aiConfig.ggufPath = path;
                    aiConfig.mode = AIConfig.MODE_LOCAL;
                    rbLocal.setChecked(true);
                    applyModeUI();
                    saveConfig();
                    toast("\uD83D\uDCC2 已选定模型: " + path);
                } else {
                    toast("\u2600\uFE0F 无法获取该文件的直读路径，请手动输入路径");
                }
            } catch (Exception e) { toast("选择失败: " + e.getMessage()); }
        }
    }

    /** 从 URI 尽量解析出文件真实路径 */
    private String uriPath(android.net.Uri uri) {
        String path = uri.getPath();
        try {
            android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) { if (c.moveToFirst()) { int in = c.getColumnIndex("_data"); if (in >= 0 && c.getString(in) != null) path = c.getString(in); } c.close(); }
        } catch (Exception ignored) {}
        // 解析 SAF document URI：content://.../document/primary:Download/xx.gguf → /storage/emulated/0/Download/xx.gguf
        if (path != null && path.startsWith("/document/primary:")) {
            try {
                String rel = java.net.URLDecoder.decode(path.substring("/document/primary:".length()), "UTF-8");
                if (rel.startsWith("/")) rel = rel.substring(1);
                path = "/storage/emulated/0/" + rel;
            } catch (Exception ignored2) {}
        }
        if (path != null && path.contains("emulated")) {
            int i = path.indexOf("emulated/");
            if (i >= 0) path = "/sdcard/" + path.substring(i + 9);
        }
        return path;
    }

    /** 加载选定的本地模型 */
    private void loadSelectedModel() {
        String p = etGgufPath == null ? "" : etGgufPath.getText().toString().trim();
        if (p.isEmpty()) { toast("\u26A0\uFE0F 请先选择或填写模型文件路径"); return; }
        if (!LLamaEngine.isAvailable()) { toast("\u26A0\uFE0F 本地引擎不可用（缺少 libllm_jni.so）"); return; }
        // 若模型在外部存储路径，需先确认有"所有文件访问"权限（targetSdk 30+ 要求）
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()
                && (p.startsWith("/sdcard/") || p.startsWith("/storage/"))) {
            new AlertDialog.Builder(this)
                    .setTitle("\uD83D\uDD12 需要文件访问权限")
                    .setMessage("加载本地模型需要读取存储中的 GGUF 文件。\n请在下个页面点击「允许管理所有文件」，然后返回重试。")
                    .setPositiveButton("\u2705 去授权", new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int w) {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e2) {
                                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                            }
                        }
                    })
                    .setNegativeButton("\u61A8\u540E\u518D\u8BF4", null)
                    .show();
            return;
        }
        final String modelPath = p;
        final android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(this)
                .setTitle("\u23F3 加载本地模型").setMessage("正在加载模型，首次可能较慢...").setCancelable(false).show();
        new Thread(new Runnable() {
            @Override public void run() {
                final boolean ok = new AIModifier().loadLocalModel(MainActivity.this, modelPath);
                runOnUiThread(new Runnable() { @Override public void run() {
                    try { dlg.dismiss(); } catch (Exception ignored) {}
                    if (ok) { toast("\u2705 本地模型加载成功"); if (cbAI != null) cbAI.setChecked(true); saveConfig(); }
                    else { toast("\u274C 模型加载失败，请确认文件为受支持的 GGUF 格式"); }
                }});
            }
        }).start();
    }

    private TextView fieldLabel(String t) { return label(t, 13f, TXT2); }
    private TextView sectionTitle(String t) {
        TextView tv = label(t, 18f, TXT);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(20, 22, 20, 8);
        tv.setLayoutParams(lp);
        return tv;
    }
    private TextView label(String t, float size, int color) {
        TextView tv = new TextView(this);
        tv.setText(t); tv.setTextSize(size); tv.setTextColor(color);
        tv.setPadding(4, 14, 4, 6);
        return tv;
    }
    private TextView hint(String t) {
        TextView tv = new TextView(this);
        tv.setText(t); tv.setTextSize(12f); tv.setTextColor(TXT2);
        tv.setPadding(4, 6, 4, 10);
        return tv;
    }
    private CheckBox check(String t, boolean checked) {
        CheckBox cb = new CheckBox(this);
        cb.setText(t); cb.setTextSize(15f); cb.setTextColor(TXT); cb.setChecked(checked);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ACC));
        return cb;
    }
    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(CARD);
        l.setPadding(dp(16), dp(14), dp(16), dp(16));
        return l;
    }
    private LinearLayout cardWrap(View v) {
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(16), 0, dp(16), 0);
        w.setLayoutParams(lp);
        w.addView(v);
        return w;
    }
    private View space(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(h))); return v; }
    private void styleInput(EditText et, String hint, String text) {
        et.setSingleLine(true);
        et.setHint(hint); et.setText(text);
        et.setTextColor(TXT); et.setHintTextColor(TXT2);
        et.setBackgroundColor(CARD_ALT);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 6, 0, 8);
        et.setLayoutParams(lp);
    }
    private void styleArea(EditText et, String hint, String text, int lines) {
        et.setSingleLine(false);
        et.setGravity(Gravity.TOP);
        et.setHint(hint); et.setText(text);
        et.setTextColor(TXT); et.setHintTextColor(TXT2);
        et.setBackgroundColor(CARD_ALT);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        // 编辑框高度适中：不过度撑满，让外层 ScrollView 有空间滚动到其它设置
        et.setMinLines(lines);
        et.setMaxLines(lines + 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 6, 0, 8);
        et.setLayoutParams(lp);
    }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    protected void onResume() { super.onResume(); updateStatus(); }

    private void updateStatus() {
        if (statusText == null || toggleButton == null) return;
        boolean enabled = isAccessibilityOn();
        if (enabled) {
            statusText.setText("\uD83D\uDD35 服务运行中 · 无障碍已开启");
            statusText.setTextColor(GOOD);
            toggleButton.setText("无障碍服务已开启");
            toggleButton.setEnabled(false);
            toggleButton.setBackgroundColor(0xFF2E3A5E);
        } else {
            statusText.setText("\u26AA\uFE0F 服务未开启");
            statusText.setTextColor(0xFFFF6B6B);
            toggleButton.setText("前往开启无障碍服务");
            toggleButton.setEnabled(true);
            toggleButton.setBackgroundColor(ACC);
        }
        if (aiStatus != null) {
            boolean ai = AIConfig.load(this).enabled;
            AIConfig ac = AIConfig.load(this);
            String modeTxt = ac.isLocal() ? "本地模型" : "在线 API";
            aiStatus.setText(ai ? ("\uD83E\uDD16 AI 已启用（" + modeTxt + "）· 标点触发改写") : "\uD83E\uDD16 AI 未启用（默认规则改写）");
        }
    }

    private boolean isAccessibilityOn() {
        try {
            AccessibilityManager am = (AccessibilityManager) getSystemService("accessibility");
            if (am == null) return false;
            List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(-1);
            for (AccessibilityServiceInfo info : list) {
                if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null
                        && getPackageName().equals(info.getResolveInfo().serviceInfo.packageName)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) { toast("无法打开设置"); }
    }

    private void saveConfig() {
        try {
            config.enableAppend = cbAppend.isChecked();
            String append = etAppendText.getText().toString().trim();
            config.appendText = append.isEmpty() ? "\u55B5" : append;
            config.enableRandomEmoticon = cbEmoticon.isChecked();
            config.processingMode = rbRealtime.isChecked() ? CatConfig.MODE_REALTIME : CatConfig.MODE_PUNCTUATION;
            ArrayList<CatConfig.Rule> rules = new ArrayList<>();
            String rt = etRules.getText() == null ? "" : etRules.getText().toString();
            for (String line : rt.split("\n")) { CatConfig.Rule r = CatConfig.parseRule(line); if (r != null) rules.add(r); }
            config.rules = rules;
            ArrayList<String> list = new ArrayList<>();
            String ct = etCustomEmoticons.getText() == null ? "" : etCustomEmoticons.getText().toString().trim();
            for (String raw : ct.split("\n")) { String t = raw.trim(); if (!t.isEmpty()) list.add(t); }
            config.customEmoticons = list.toArray(new String[0]);
            config.save(this);

            aiConfig.enabled = cbAI.isChecked();
            aiConfig.mode = (rbLocal != null && rbLocal.isChecked()) ? AIConfig.MODE_LOCAL : AIConfig.MODE_ONLINE;
            aiConfig.baseUrl = str(etAIUrl, AIConfig.DEFAULT_BASE_URL);
            aiConfig.model = str(etAIModel, AIConfig.DEFAULT_MODEL);
            aiConfig.apiKey = str(etAIKey, "");
            if (etGgufPath != null) aiConfig.ggufPath = etGgufPath.getText().toString().trim();
            aiConfig.persona = str(etAIPersona, AIConfig.DEFAULT_PERSONA);
            try { aiConfig.temperature = Double.parseDouble(str(etAITemp, "0.8")); } catch (Exception ignored) {}
            try { aiConfig.timeoutSec = Integer.parseInt(str(etAITimeout, "60")); } catch (Exception ignored) {}
            aiConfig.save(this);
            toast("\u2705 设置已保存");
        } catch (Exception e) { toast("保存失败: " + e.getMessage()); }
    }

    /**
     * 弹出应用多选对话框：列出已安装的第三方应用，用户勾选生效。
     */
    private void showAppPicker() {
        try {
            final ArrayList<String> pkgs = new ArrayList<String>();
            final ArrayList<String> labels = new ArrayList<String>();
            final java.util.Set<String> current = new java.util.LinkedHashSet<String>(aiConfig.enabledApps);
            PackageManager pm = getPackageManager();
            // 读取「有桌面图标」的所有应用（日常可用的软件，含被 Hail 冻结/隐藏的用 MATCH_UNINSTALLED_PACKAGES 补全）
            // 不过滤系统标志，避免把"已更新的系统应用"(如微信/QQ) 误判为系统包而漏掉。
            int flags = android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;
            Intent launcher = new Intent(Intent.ACTION_MAIN);
            launcher.addCategory(Intent.CATEGORY_LAUNCHER);
            List<android.content.pm.ResolveInfo> list;
            try { list = pm.queryIntentActivities(launcher, flags); }
            catch (Exception e1) { try { list = pm.queryIntentActivities(launcher, 0); } catch (Exception e2) { list = null; } }
            if (list == null) list = new ArrayList<android.content.pm.ResolveInfo>();
            java.util.Map<String, android.content.pm.ResolveInfo> map = new java.util.LinkedHashMap<String, android.content.pm.ResolveInfo>();
            for (android.content.pm.ResolveInfo ri : list) {
                if (ri == null || ri.activityInfo == null || ri.activityInfo.packageName == null) continue;
                if (!map.containsKey(ri.activityInfo.packageName)) map.put(ri.activityInfo.packageName, ri);
            }
            java.util.List<android.content.pm.ResolveInfo> sortedApps = new ArrayList<android.content.pm.ResolveInfo>(map.values());
            java.util.Collections.sort(sortedApps, new java.util.Comparator<android.content.pm.ResolveInfo>() {
                public int compare(android.content.pm.ResolveInfo a, android.content.pm.ResolveInfo b) {
                    String la = labelOf(a), lb = labelOf(b);
                    return la.compareToIgnoreCase(lb);
                }
            });
            for (android.content.pm.ResolveInfo ri : sortedApps) {
                try {
                    String pkg = ri.activityInfo.packageName;
                    String label = labelOf(ri);
                    pkgs.add(pkg); labels.add(label);
                } catch (Exception ignored) {}
            }
            if (pkgs.isEmpty()) { toast("未读取到可用第三方应用"); return; }
            final boolean[] checked = new boolean[pkgs.size()];
            for (int i = 0; i < pkgs.size(); i++) checked[i] = current.contains(pkgs.get(i));
            new AlertDialog.Builder(this)
                    .setTitle("选择生效应用（多选）")
                    .setMultiChoiceItems(labels.toArray(new String[0]), checked,
                            new android.content.DialogInterface.OnMultiChoiceClickListener() {
                                public void onClick(android.content.DialogInterface d, int which, boolean isChecked) {
                                    checked[which] = isChecked;
                                }
                            })
                    .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int w) {
                            java.util.Set<String> sel = new java.util.LinkedHashSet<String>();
                            for (int i = 0; i < pkgs.size(); i++) if (checked[i]) sel.add(pkgs.get(i));
                            if (sel.isEmpty()) sel.add("com.tencent.mobileqq");
                            aiConfig.enabledApps = sel;
                            aiConfig.save(MainActivity.this);
                            toast("已保存 " + sel.size() + " 个启用应用，请重新打开目标应用生效");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) { toast("读取应用列表失败: " + e.getMessage()); }
    }

    /** 获取应用显示名；拿不到则回退为包名 */
    private String labelOf(android.content.pm.ResolveInfo ri) {
        try {
            if (ri == null || ri.activityInfo == null) return "";
            String lbl = getPackageManager().getApplicationLabel(ri.activityInfo.applicationInfo).toString();
            if (lbl != null && !lbl.trim().isEmpty()) return lbl;
        } catch (Exception ignored) {}
        return ri.activityInfo.packageName;
    }

    private void showTestDialog() {
        try {
            saveConfig();
            CatConfig tc = CatConfig.load(this);
            AIConfig ac = AIConfig.load(this);
            String sample = "今天我很好，你准备好了吗？我们去公园玩吧";
            String out = TextProcessor.process(sample, tc);
            String msg = "规则改写：\n" + out
                    + "\n\nAI 改写：" + (ac.enabled
                        ? (ac.isLocal()
                            ? ("本地模型 " + (ac.ggufPath.isEmpty() ? "（未选模型）" : shortName(ac.ggufPath)))
                            : ("在线 API " + ac.model))
                        : "未启用")
                    + "\nAI 人设：" + (ac.persona == null || ac.persona.isEmpty() ? "无" : ac.persona.substring(0, Math.min(20, ac.persona.length())) + "...");
            new AlertDialog.Builder(this).setTitle("\uD83D\uDC0C 预览").setMessage(msg).setPositiveButton("好的", null).show();
        } catch (Exception e) { toast("测试失败: " + e.getMessage()); }
    }

    private String str(EditText et, String def) { String s = et.getText() == null ? "" : et.getText().toString().trim(); return s.isEmpty() ? def : s; }
    private String shortName(String path) {
        if (path == null || path.isEmpty()) return "";
        int i = path.lastIndexOf('/');
        return i >= 0 ? path.substring(i + 1) : path;
    }
    private String joinLines(String[] arr) { StringBuilder sb = new StringBuilder(); if (arr != null) for (String s : arr) { if (s != null && !s.trim().isEmpty()) { if (sb.length() > 0) sb.append("\n"); sb.append(s.trim()); } } return sb.toString(); }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    /**
     * 首次启动展示免责声明弹窗。
     * 用户点击「我已知晓并同意」后不再重复弹出。
     */
    private void showDisclaimerIfNeeded() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("disclaimer", MODE_PRIVATE);
            if (prefs.getBoolean("agreed_v1", false)) return;   // 已同意过，不再重复
            String msg = "【官方渠道与安全声明】\n\n"
                    + "一、项目来源与版权\n"
                    + "本应用「喵喵助手·夕兮二改版」是基于开源项目「喵喵助手」"
                    + "（遵循 AGPLv3 协议）的二次开发与重构版本。"
                    + "原始「喵喵助手」由原作者 QiCaiJie 开发并开源，"
                    + "其官方源码仓库为：\n"
                    + "   https://github.com/QiCaiJie114514/QQMiaoAssistant\n"
                    + "原作者保留其对原始代码的著作权；"
                    + "本项目全部新增及重构代码由「夕兮 / SKYRAIN」创作，按 AGPLv3 协议发布。\n\n"
                    + "二、唯一官方获取渠道\n"
                    + "本应用唯一官方获取渠道为 QQ：792413184。"
                    + "仅在通过该渠道获取，并由您自行核对应用签名后构建/安装的版本，"
                    + "方可视为正常、可信版本。\n\n"
                    + "三、非官方渠道风险提示\n"
                    + "除上述官方渠道外，任何经由二进制安装包、网盘链接、"
                    + "或其他第三方 fork 渠道所获得的版本，均非本人（夕兮 / SKYRAIN）发布。"
                    + "此类渠道中的安装包存在被篡改、植入病毒、木马或后门的风险，"
                    + "由此产生的一切损失由相应分发者承担，与原作者及本人无关。\n\n"
                    + "四、安全使用提醒\n"
                    + "请勿安装来源不明的 APK；安装前请务必核对渠道及应用数字签名，"
                    + "并警惕要求额外权限或索取个人信息的行为。\n\n"
                    + "© 夕兮 / SKYRAIN    渠道 QQ：792413184\n"
                    + "本源：https://github.com/QiCaiJie114514/QQMiaoAssistant";
            new AlertDialog.Builder(this)
                    .setTitle("官方渠道与安全声明")
                    .setMessage(msg)
                    .setCancelable(false)
                    .setPositiveButton("我已知晓并同意", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) {
                            prefs.edit().putBoolean("agreed_v1", true).apply();
                        }
                    })
                    .setNegativeButton("退出应用", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) {
                            finish();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .show();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "免责弹窗失败: " + e.getMessage());
        }
    }
}