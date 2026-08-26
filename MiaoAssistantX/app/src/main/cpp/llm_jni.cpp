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
 * 喵喵助手魔改版（MiaoAssistantX） - JNI 桥接层
 * 本文件是「夕兮/SKYRAIN」基于「喵喵助手」重构与扩展的实现的一部分，
 * 通过 llama.cpp 提供的接口，在本地加载 .gguf 模型进行推理。
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
 *
 * 说明：本桥接文件所调用的 llama.cpp / ggml 底层库为第三方组件，
 * 遵循 MIT 许可，其源码与许可见随附 NOTICE 文件。
 */
#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <android/log.h>
#include "llama.h"
#include "llama-cpp.h"
#include "ggml.h"
#define CLASS_NAME "com/example/u7e5f3218e9/LLamaEngine"
#define LOGTAG "LLamaNative"
static void llama_log_cb(enum ggml_log_level level, const char * text, void * user_data) {
    // 把 llama.cpp/ggml 的日志转发到 Android logcat，便于诊断模型加载问题
    (void)user_data;
    int prio = ANDROID_LOG_DEBUG;
    switch (level) {
        case GGML_LOG_LEVEL_ERROR:   prio = ANDROID_LOG_ERROR; break;
        case GGML_LOG_LEVEL_WARN:    prio = ANDROID_LOG_WARN;  break;
        case GGML_LOG_LEVEL_INFO:    prio = ANDROID_LOG_INFO;  break;
        default: prio = ANDROID_LOG_DEBUG; break;
    }
    if (text) __android_log_write(prio, LOGTAG, text);
}
struct guard_llm_t {
    llama_model   * model   = nullptr;
    llama_context * ctx     = nullptr;
    const llama_vocab * vocab = nullptr;
    llama_sampler * smpl    = nullptr;
    bool loaded = false;
};
static bool ensure_backend_done = false;
static guard_llm_t * toHandle(JNIEnv * env, jlong h) { return reinterpret_cast<guard_llm_t *>(h); }
static jlong JNICALL nativeCreate(JNIEnv * env, jclass clazz) {
    llama_log_set(llama_log_cb, nullptr);
    ggml_log_set(llama_log_cb, nullptr);
    if (!ensure_backend_done) { ggml_backend_load_all(); llama_backend_init(); ensure_backend_done = true; }
    guard_llm_t * g = new guard_llm_t();
    return reinterpret_cast<jlong>(g);
}
static jint JNICALL nativeLoad(JNIEnv * env, jclass clazz, jlong h, jstring jpath) {
    guard_llm_t * g = toHandle(env, h);
    if (!g) return -1;
    const char * cpath = env->GetStringUTFChars(jpath, nullptr);
    if (!cpath) return -1;
    __android_log_print(ANDROID_LOG_INFO, LOGTAG, "开始加载模型: %s", cpath);
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;
    g->model = llama_model_load_from_file(cpath, mparams);
    env->ReleaseStringUTFChars(jpath, cpath);
    if (!g->model) { __android_log_print(ANDROID_LOG_ERROR, LOGTAG, "模型加载失败(返回空): %s", cpath); return 1; }
    g->vocab = llama_model_get_vocab(g->model);
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = 4096; cparams.n_batch = 512;
    cparams.n_threads = 4; cparams.n_threads_batch = 4;
    g->ctx = llama_init_from_model(g->model, cparams);
    if (!g->ctx) { __android_log_print(ANDROID_LOG_ERROR, LOGTAG, "ctx 初始化失败"); llama_model_free(g->model); g->model = nullptr; return 2; }
    auto sparams = llama_sampler_chain_default_params();
    g->smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(g->smpl, llama_sampler_init_temp(0.8f));
    llama_sampler_chain_add(g->smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(g->smpl, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(g->smpl, llama_sampler_init_min_p(0.05f, 1));
    // 关键：链末尾必须有一个“最终采样器”（greedy 或 dist）来设置 cur_p.selected。
    // 否则 llama_sampler_sample 里 GGML_ASSERT(cur_p.selected >= 0 ...) 必然失败 → ggml_abort → 进程崩溃。
    // temp/top_k/top_p/min_p 都只改 logit/概率，不设置 selected，故必须追加 dist。
    llama_sampler_chain_add(g->smpl, llama_sampler_init_dist(0));   // 分布采样，seed=0
    g->loaded = true;
    __android_log_print(ANDROID_LOG_INFO, LOGTAG, "模型加载成功");
    return 0;
}
static jstring JNICALL nativeGenerate(JNIEnv * env, jclass clazz, jlong h, jstring juser, jstring jsystem, jint max_tokens, jdouble temperature) {
    guard_llm_t * g = toHandle(env, h);
    if (!g || !g->loaded) return nullptr;
    const char * cuser = env->GetStringUTFChars(juser, nullptr);
    const char * csystem = env->GetStringUTFChars(jsystem, nullptr);
    std::string system_p = csystem ? csystem : "";
    std::string prompt;
    if (!system_p.empty()) { prompt = "<|im_start|>system\n" + system_p + "<|im_end|>\n"; }
    prompt += "<|im_start|>user\n" + std::string(cuser ? cuser : "") + "<|im_end|>\n<|im_start|>assistant\n";
    env->ReleaseStringUTFChars(juser, cuser);
    env->ReleaseStringUTFChars(jsystem, csystem);
    // 注意：不要在这里动态 llama_sampler_chain_remove + add override temp。
    // 该 API 在部分 llama.cpp 版本下会损坏采样链状态，导致 llama_sampler_sample 内
    // GGML_ASSERT/ggml_abort → SIGABRT 进程崩溃。温度固定用加载时设定的 0.8f。
    int n_prompt = -llama_tokenize(g->vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0) return nullptr;
    std::vector<llama_token> prompt_tokens(n_prompt);
    if (llama_tokenize(g->vocab, prompt.c_str(), prompt.size(), prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) return nullptr;
    llama_batch batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
    if (llama_model_has_encoder(g->model)) {
        if (llama_encode(g->ctx, batch)) return nullptr;
        llama_token dec = llama_model_decoder_start_token(g->model);
        if (dec == LLAMA_TOKEN_NULL) dec = llama_vocab_bos(g->vocab);
        batch = llama_batch_get_one(&dec, 1);
    }
    const int n_ctx_max = llama_n_ctx(g->ctx);
    if (max_tokens < 1 || max_tokens > 512) max_tokens = 256;  // 限制生成长度，防止上下文溢出
    std::string out; int n_decode = 0;
    for (int n_pos = 0;
         n_pos + batch.n_tokens < (int)prompt_tokens.size() + max_tokens &&
         n_pos + batch.n_tokens < n_ctx_max; ) {   // 预留上下文，防止 KV 溢出
        if (llama_decode(g->ctx, batch)) break;
        n_pos += batch.n_tokens;
        llama_token new_id = llama_sampler_sample(g->smpl, g->ctx, -1);
        if (llama_vocab_is_eog(g->vocab, new_id)) break;
        char buf[512];
        int n = llama_token_to_piece(g->vocab, new_id, buf, sizeof(buf), 0, true);
        if (n < 0) break;
        out.append(buf, n);
        batch = llama_batch_get_one(&new_id, 1);
        n_decode++; if (n_decode >= max_tokens) break;
    }
    if (out.empty()) return nullptr;
    return env->NewStringUTF(out.c_str());
}
static jint JNICALL nativeRelease(JNIEnv * env, jclass clazz, jlong h) {
    guard_llm_t * g = toHandle(env, h);
    if (!g) return 0;
    if (g->smpl)  { llama_sampler_free(g->smpl); g->smpl = nullptr; }
    if (g->ctx)   { llama_free(g->ctx); g->ctx = nullptr; }
    if (g->model) { llama_model_free(g->model); g->model = nullptr; }
    g->loaded = false; delete g; return 0;
}
static const JNINativeMethod methods[] = {
    {"nativeCreate",     "()J",                                             (void*)nativeCreate},
    {"nativeLoad",       "(JLjava/lang/String;)I",                           (void*)nativeLoad},
    {"nativeGenerate",   "(JLjava/lang/String;Ljava/lang/String;ID)Ljava/lang/String;", (void*)nativeGenerate},
    {"nativeRelease",    "(J)I",                                             (void*)nativeRelease},
};
JNIEXPORT jint JNI_OnLoad(JavaVM * vm, void * reserved) {
    JNIEnv * env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass cls = env->FindClass(CLASS_NAME);
    if (!cls) return JNI_ERR;
    if (env->RegisterNatives(cls, methods, sizeof(methods)/sizeof(methods[0])) != JNI_OK) return JNI_ERR;
    return JNI_VERSION_1_6;
}