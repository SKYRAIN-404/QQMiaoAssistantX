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

import android.content.Context;
import android.content.SharedPreferences;

/**
 * AI 改写配置（双通道）。
 * mode = "online"  -> 调用在线 OpenAI 兼容 API（如 DeepSeek / OpenAI）
 * mode = "local"   -> 直接用 llama.cpp 加载手机里的 .gguf 模型推理
 */
public class AIConfig {
    // ---- 在线 API 默认值（OpenAI 兼容）----
    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    public static final String DEFAULT_MODEL = "deepseek-chat";

    public static final String MODE_ONLINE = "online";
    public static final String MODE_LOCAL = "local";

    public static final String DEFAULT_PERSONA =
            "你是一位温柔可爱的猫娘助手（喵喵），正在帮用户把一条日常聊天输入改写得更可爱、更有猫味。" +
            "规则：\n" +
            "1. 把第一人称\"我\"改为\"本喵\"，把\"你\"改为\"主人\"。\n" +
            "2. 在句末或断句处自然地加上\"喵\"、\"~\"等语气词。\n" +
            "3. 可以适当添加可爱的颜文字，但不要改变原意。\n" +
            "4. 不要丢失任何信息，不要编造。\n" +
            "只输出改写后的文本，不要解释。";

    private static final String PREFS = "ai_config";
    private static final String KEY_ENABLED = "ai_enabled";
    private static final String KEY_MODE = "ai_mode";
    private static final String KEY_BASE_URL = "ai_base_url";
    private static final String KEY_MODEL = "ai_model";
    private static final String KEY_API_KEY = "ai_api_key";
    private static final String KEY_PERSONA = "ai_persona";
    private static final String KEY_TEMPERATURE = "ai_temperature";
    private static final String KEY_MAX_TOKENS = "ai_max_tokens";
    private static final String KEY_TIMEOUT = "ai_timeout";
    private static final String KEY_GGUF_PATH = "ai_gguf_path";
    private static final String KEY_N_THREADS = "ai_n_threads";
    private static final String KEY_ENABLED_APPS = "ai_enabled_apps";

    public boolean enabled = false;
    public String mode = MODE_ONLINE;          // online / local
    public String baseUrl = DEFAULT_BASE_URL;
    public String model = DEFAULT_MODEL;
    public String apiKey = "";
    public String persona = DEFAULT_PERSONA;
    public double temperature = 0.8;
    public int maxTokens = 256;
    public int timeoutSec = 60;
    public String ggufPath = "";               // 本地模型文件路径
    public int nThreads = 4;                   // 本地推理线程数
    public java.util.Set<String> enabledApps = new java.util.LinkedHashSet<>(); // 启用本服务的应用包名集合

    public static AIConfig load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AIConfig cfg = new AIConfig();
        cfg.enabled = sp.getBoolean(KEY_ENABLED, false);
        cfg.mode = sp.getString(KEY_MODE, MODE_ONLINE);
        cfg.baseUrl = sp.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        cfg.model = sp.getString(KEY_MODEL, DEFAULT_MODEL);
        cfg.apiKey = sp.getString(KEY_API_KEY, "");
        cfg.persona = sp.getString(KEY_PERSONA, DEFAULT_PERSONA);
        cfg.temperature = sp.getFloat(KEY_TEMPERATURE, 0.8f);
        cfg.maxTokens = sp.getInt(KEY_MAX_TOKENS, 256);
        cfg.timeoutSec = sp.getInt(KEY_TIMEOUT, 60);
        cfg.ggufPath = sp.getString(KEY_GGUF_PATH, "");
        cfg.nThreads = sp.getInt(KEY_N_THREADS, 4);
        // 读取启用应用包名集合（逗号分隔存储），默认含 QQ、QQ国际版
        cfg.enabledApps = new java.util.LinkedHashSet<String>();
        String apps = sp.getString(KEY_ENABLED_APPS, null);
        if (apps != null && !apps.trim().isEmpty()) {
            for (String s : apps.split(",")) { String t = s.trim(); if (!t.isEmpty()) cfg.enabledApps.add(t); }
        } else {
            cfg.enabledApps.add("com.tencent.mobileqq");
        }
        return cfg;
    }

    public void save(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = sp.edit();
            ed.putBoolean(KEY_ENABLED, this.enabled);
            ed.putString(KEY_MODE, this.mode == null ? MODE_ONLINE : this.mode);
            ed.putString(KEY_BASE_URL, this.baseUrl == null ? DEFAULT_BASE_URL : this.baseUrl);
            ed.putString(KEY_MODEL, this.model == null ? DEFAULT_MODEL : this.model);
            ed.putString(KEY_API_KEY, this.apiKey == null ? "" : this.apiKey);
            ed.putString(KEY_PERSONA, this.persona == null ? DEFAULT_PERSONA : this.persona);
            ed.putFloat(KEY_TEMPERATURE, (float) this.temperature);
            ed.putInt(KEY_MAX_TOKENS, this.maxTokens);
            ed.putInt(KEY_TIMEOUT, this.timeoutSec);
            ed.putString(KEY_GGUF_PATH, this.ggufPath == null ? "" : this.ggufPath);
            ed.putInt(KEY_N_THREADS, this.nThreads);
            // 启用应用包名集合 → 逗号分隔串
            StringBuilder sb = new StringBuilder();
            if (this.enabledApps != null) { for (String s : this.enabledApps) { if (s != null && !s.trim().isEmpty()) { if (sb.length() > 0) sb.append(","); sb.append(s.trim()); } } }
            if (sb.length() == 0) sb.append("com.tencent.mobileqq");
            ed.putString(KEY_ENABLED_APPS, sb.toString());
            // 用 commit() 同步写盘，避免 apply() 异步时进程被杀导致配置丢失（“设置消失”bug 根因）。
            ed.commit();
            android.util.Log.i("AIConfig", "AI 配置已同步保存到 " + PREFS + " (enabled=" + this.enabled + ", mode=" + this.mode + ")");
        } catch (Exception e) {
            android.util.Log.e("AIConfig", "AI 配置保存失败: " + e.getMessage(), e);
        }
    }

    /** 判断当前是否使用本地模型 */
    public boolean isLocal() {
        return MODE_LOCAL.equals(mode);
    }
}