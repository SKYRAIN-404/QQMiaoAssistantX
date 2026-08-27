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

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * AI 改写路由（双通道）。
 *  - 本地模式(mode=local)：用 llama.cpp JNI 加载 .gguf 直接推理，不走端口。
 *  - 在线模式(mode=online)：调用任意 OpenAI 兼容在线 API（如 DeepSeek）。
 */
public class AIModifier {
    private static final String TAG = "AIModifier";
    private final AIConfig config;
    private LLamaEngine localEngine;

    public AIModifier(AIConfig config) {
        this.config = config;
    }

    public AIModifier() { this.config = null; }

    /** 全局唯一的本地引擎（懒加载），用于避免反复加载模型 */
    private synchronized LLamaEngine engine(Context ctx) {
        if (localEngine == null) {
            localEngine = new LLamaEngine();
        }
        if (!localEngine.isAvailable()) {
            Log.e(TAG, "本地引擎不可用（libllm_jni.so 缺失或加载失败）");
        }
        return localEngine;
    }

    public String rewrite(Context ctx, String original) {
        if (original == null || original.trim().isEmpty()) {
            return original;
        }
        AIConfig cfg = (config != null) ? config : AIConfig.load(ctx);

        if (!cfg.enabled) return original;

        try {
            if (cfg.isLocal()) {
                return rewriteLocal(ctx, cfg, original);
            } else {
                return rewriteOnline(cfg, original);
            }
        } catch (Exception e) {
            Log.e(TAG, "AI 改写失败", e);
            return original;
        }
    }

    // ==================== 本地模型（llama.cpp） ====================
    private String rewriteLocal(Context ctx, AIConfig cfg, String original) {
        if (cfg.ggufPath == null || cfg.ggufPath.isEmpty()) {
            Log.e(TAG, "本地模式未指定模型文件");
            return null;
        }
        LLamaEngine e = engine(ctx);
        if (!e.isAvailable()) {
            Log.e(TAG, "本地引擎 so 不可用");
            return null;
        }
        // 若进程重启后模型未加载（句柄为空），则自动按配置路径重新加载，
        // 避免“主界面加载过一次、但 QQ 服务实例没模型”导致标点触发无反应。
        if (!e.isModelLoaded() && cfg.ggufPath != null && !cfg.ggufPath.isEmpty()) {
            Log.i(TAG, "本地模型未加载，按配置路径自动加载: " + cfg.ggufPath);
            e.loadModel(cfg.ggufPath);
        }
        String system = (cfg.persona == null || cfg.persona.isEmpty())
                ? AIConfig.DEFAULT_PERSONA : cfg.persona;
        String out = e.generate(original, system, cfg.maxTokens, cfg.temperature);
        if (out == null || out.trim().isEmpty()) {
            Log.e(TAG, "本地模型无有效输出");
            return null;
        }
        String trimmed = out.trim();
        // 去掉常见包装（模型可能带引号或前后缀）
        return trimmed;
    }

    /** 供外部加载本地模型（在 MainActivity 选择模型后调用），返回是否成功 */
    public boolean loadLocalModel(Context ctx, String ggufPath) {
        AIConfig cfg = (config != null) ? config : AIConfig.load(ctx);
        cfg.ggufPath = ggufPath;
        cfg.mode = AIConfig.MODE_LOCAL;
        cfg.enabled = true;
        boolean ok = engine(ctx).loadModel(ggufPath);
        if (ok) cfg.save(ctx);
        return ok;
    }

    // ==================== 在线 API（OpenAI 兼容） ====================
    private String rewriteOnline(AIConfig cfg, String original) {
        String baseUrl = cfg.baseUrl;
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = AIConfig.DEFAULT_BASE_URL;
        }
        baseUrl = baseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String endpoint = baseUrl + "/chat/completions";
        String body = openaiRequestBody(cfg, original);
        String result = postJson(endpoint, body, cfg);
        if (result != null && !result.trim().isEmpty()) {
            return result.trim();
        }
        return null;
    }

    private String openaiRequestBody(AIConfig cfg, String text) {
        String persona = (cfg.persona == null || cfg.persona.isEmpty())
                ? AIConfig.DEFAULT_PERSONA : cfg.persona;
        return "{\"model\":\"" + esc(cfg.model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + esc(persona) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + esc(text) + "\"}"
                + "],\"temperature\":" + cfg.temperature + ","
                + "\"max_tokens\":" + cfg.maxTokens + "}";
    }

    private String postJson(String urlStr, String body, AIConfig cfg) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(cfg.timeoutSec * 1000);
            conn.setReadTimeout(cfg.timeoutSec * 1000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (cfg.apiKey != null && !cfg.apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + cfg.apiKey);
            }
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(data);
            os.flush();
            os.close();
            int code = conn.getResponseCode();
            InputStream is;
            if (code >= 200 && code < 300) {
                is = conn.getInputStream();
            } else {
                Log.e(TAG, "在线 API HTTP " + code + ": " + urlStr);
                return null;
            }
            String resp = readStream(is);
            return extractContent(resp);
        } catch (Exception e) {
            Log.e(TAG, "在线 API 请求异常", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 从 OpenAI 兼容响应中提取 content 字段 */
    private String extractContent(String json) {
        if (json == null) return null;
        Log.d(TAG, "在线响应: " + trimToLen(json, 500));
        try {
            int chIdx = json.indexOf("\"choices\"");
            if (chIdx >= 0) {
                int cIdx = json.indexOf("\"content\"", chIdx);
                if (cIdx >= 0) return unescape(extractStringValue(json, cIdx));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractStringValue(String json, int keyIdx) {
        int colon = json.indexOf(':', keyIdx);
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return "";
        char c = json.charAt(start);
        if (c == '"') {
            StringBuilder sb = new StringBuilder();
            boolean inEscape = false;
            for (int i = start + 1; i < json.length(); i++) {
                char ch = json.charAt(i);
                if (inEscape) { sb.append('\\').append(ch); inEscape = false; }
                else if (ch == '\\') { inEscape = true; }
                else if (ch == '"') break;
                else sb.append(ch);
            }
            return sb.toString();
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') end++;
        return json.substring(start, end).trim();
    }

    private String unescape(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").replace("\\/", "/").replace("\\t", "\t");
    }

    private String readStream(InputStream is) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String trimToLen(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}