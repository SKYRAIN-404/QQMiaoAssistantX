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

import android.util.Log;

/**
 * 本地 GGUF 模型推理引擎（基于 llama.cpp JNI）。
 * 通过 llm_jni.so 直接加载手机里的 .gguf 模型文件，
 * 完全不依赖任何 TCP 端口 / HTTP 服务。
 *
 * native 方法都由 libllm_jni.so 提供。
 */
public class LLamaEngine {
    private static final String TAG = "LLamaEngine";
    private static boolean loaded = false;
    // 静态句柄：让"主界面加载模型"与"无障碍服务推理"共享同一份模型，
    // 否则各处 new 出的 LLamaEngine 各持有独立 handle，导致推理实例没加载过模型。
    private static long handle = 0;    // native guard_llm_t*（全局共享）
    private volatile boolean running = false;

    static {
        try {
            System.loadLibrary("llm_jni");
            loaded = true;
            Log.i(TAG, "libllm_jni.so 加载成功");
        } catch (UnsatisfiedLinkError e) {
            loaded = false;
            Log.e(TAG, "libllm_jni.so 加载失败：本地模型不可用", e);
        }
    }

    /** 判断本机是否具备本地推理能力 */
    public static boolean isAvailable() {
        return loaded;
    }

    /** 判断当前是否已加载了一个可用的模型 */
    public synchronized boolean isModelLoaded() {
        return loaded && handle != 0;
    }

    // ---- native 接口 ----
    private native long nativeCreate();
    private native int nativeLoad(long h, String ggufPath);
    private native String nativeGenerate(long h, String user, String system, int maxTokens, double temperature);
    private native int nativeRelease(long h);

    /** 加载模型文件，返回 true 表示成功 */
    public synchronized boolean loadModel(String ggufPath) {
        if (!loaded) return false;
        if (ggufPath == null || ggufPath.isEmpty()) return false;
        release();
        try {
            handle = nativeCreate();
            if (handle == 0) return false;
            int rc = nativeLoad(handle, ggufPath);
            if (rc != 0) {
                Log.e(TAG, "模型加载失败 rc=" + rc + " path=" + ggufPath);
                nativeRelease(handle);
                handle = 0;
                return false;
            }
            Log.i(TAG, "模型加载成功: " + ggufPath);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "loadModel 异常", t);
            return false;
        }
    }

    /** 使用本地模型对输入进行改写/生成 */
    public String generate(String user, String system, int maxTokens, double temperature) {
        if (!loaded || handle == 0) return null;
        if (running) return null; // 防重入
        running = true;
        try {
            return nativeGenerate(handle, user, system == null ? "" : system, maxTokens, temperature);
        } catch (Throwable t) {
            Log.e(TAG, "generate 异常", t);
            return null;
        } finally {
            running = false;
        }
    }

    /** 释放模型资源 */
    public synchronized void release() {
        if (handle != 0) {
            try { nativeRelease(handle); } catch (Throwable ignored) {}
            handle = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // handle 为全局共享，不在此释放（避免 GC 误杀主界面加载的模型）
        super.finalize();
    }
}