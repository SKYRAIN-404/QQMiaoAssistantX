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
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class CatConfig {
    public static final String[] BUILTIN_EMOTICONS = {"^⌯𖥦⌯^ ੭ ^", "⌯'ㅅ'⌯", "=^𖥦^=", "⌯•ㅅ•⌯", "ฅ•̀∀•́ฅ", "ฅ ̳͒•ˑ̫• ̳͒ฅ♡", "ฅ(̳•·̫•̳ฅ)♡", "ฅ^••^ฅ", "=^•ω•^=", "₍^ >ヮ<^₎", "/ᐠ - ˕ -マ Ⳋ", "ฅ^•ﻌ•^ฅ", "ฅ՞•ﻌ•՞ฅ", "(ฅ´ω`ฅ)", "ฅ(*`ω´*)ฅ", "ฅ꒰ ⸝˶• •˶⸝꒱ฅ", "₍˄·͈༝·͈˄*₎◞ ̑̑", "!!^⌯𖥦⌯^ ੭!!", "₍^⸝⸝> ·̫ <⸝⸝ ^₎", "ฅ^._.^ฅ", "₍🎀˄•͈༝•͈˄₎ฅ˒˒", "^•͈༝•^ฅ", "꒰ఎ(^ . ֑ .^)໒꒱", "ฅ●ω●ฅ", "₍⸍⸌·͈༝·͈⸍⸌₎◞", "(>^ω^<)", "ฅ^-﹃-^ฅ", "^ ̳ට ̫ ට ̳^", "୧₍˄·͈༝·͈˄₎୨", "^ ̳ᴗ  ̫ ᴗ ̳^", "˓˓ก(⸍⸌̣ʷ̣̫⸍̣⸌₎ค˒˒", "ヽ(ฅ≧へ≦)ฅ", "(`･ω･´)ฅ", "(=^･ᴥ･^=)", "(^ω^ฅ)", "ฅ(≧▽≦)ฅ", "ฅ(=´▽`=)ฅ", "ヾ((๑˘ㅂ˘๑)ฅ", "(ฅ◑ω◑ฅ)", "(๑•̀ω•́ฅ)", "(ฅ>ω<*ฅ)", "(=^.^=)", "(=´ᴥ`)", "(=ↀωↀ=)", "(=^-ω-^=)", "ฅ(*°ω°*ฅ)", "ヽ(=^･ω･^=)丿", "(^•ᴥ•^)", "( Φ ω Φ )", "(=^x^=)", "ฅ( ̳• ◡ • ̳)ฅ", "o( =•ω•= )m", "~o( =∩ω∩= )m", "≡ω≡"};

    public static final String KEY_RULES = "rules";
    public static final String KEY_ENABLE_APPEND = "enable_append";
    public static final String KEY_APPEND_TEXT = "append_text";
    public static final String KEY_ENABLE_EMOTICON = "enable_emoticon";
    public static final String KEY_CUSTOM_EMOTICONS = "custom_emoticons";
    public static final String KEY_PROCESSING_MODE = "processing_mode";
    public static final String MODE_PUNCTUATION = "punctuation";
    public static final String MODE_REALTIME = "realtime";
    private static final String PREFS_NAME = "cat_config";

    public static class Rule {
        public final String from;
        public final String to;

        public Rule(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return from + "=" + to;
        }
    }

    public boolean enableAppend = true;
    public String appendText = "喵";
    public boolean enableRandomEmoticon = true;
    public String processingMode = MODE_PUNCTUATION;
    public String[] customEmoticons = new String[0];
    public List<Rule> rules = new ArrayList<>();

    public static Rule parseRule(String line) {
        if (line == null) {
            return null;
        }
        String s = line.trim();
        if (s.isEmpty()) {
            return null;
        }
        String separators = "=＝→";
        int idx = -1;
        for (int i = 0; i < separators.length(); i++) {
            int p = s.indexOf(separators.charAt(i));
            if (p >= 0 && (idx < 0 || p < idx)) {
                idx = p;
            }
        }
        if (idx <= 0) {
            return null;
        }
        String from = s.substring(0, idx).trim();
        String to = s.substring(idx + 1).trim();
        if (from.isEmpty()) {
            return null;
        }
        return new Rule(from, to);
    }

    public static String rulesToString(List<Rule> rules) {
        StringBuilder sb = new StringBuilder();
        if (rules != null) {
            for (Rule r : rules) {
                if (r == null || r.from.isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(r.from).append('=').append(r.to);
            }
        }
        return sb.toString();
    }

    public static CatConfig load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, 0);
        CatConfig cfg = new CatConfig();
        cfg.enableAppend = sp.getBoolean(KEY_ENABLE_APPEND, true);
        cfg.appendText = sp.getString(KEY_APPEND_TEXT, "喵");
        cfg.enableRandomEmoticon = sp.getBoolean(KEY_ENABLE_EMOTICON, true);
        cfg.processingMode = sp.getString(KEY_PROCESSING_MODE, MODE_PUNCTUATION);

        String rulesStr = sp.getString(KEY_RULES, "");
        if (rulesStr != null && !rulesStr.trim().isEmpty()) {
            List<Rule> list = new ArrayList<>();
            for (String line : rulesStr.split("\n")) {
                Rule r = parseRule(line);
                if (r != null) {
                    list.add(r);
                }
            }
            cfg.rules = list;
        }

        String custom = sp.getString(KEY_CUSTOM_EMOTICONS, "");
        if (custom != null && !custom.trim().isEmpty()) {
            List<String> list = new ArrayList<>();
            for (String s : custom.split("\n")) {
                String t = s.trim();
                if (!t.isEmpty()) {
                    list.add(t);
                }
            }
            cfg.customEmoticons = list.toArray(new String[0]);
        } else {
            cfg.customEmoticons = new String[0];
        }
        return cfg;
    }

    public void save(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(KEY_ENABLE_APPEND, this.enableAppend);
        ed.putString(KEY_APPEND_TEXT, this.appendText == null ? "" : this.appendText);
        ed.putBoolean(KEY_ENABLE_EMOTICON, this.enableRandomEmoticon);
        ed.putString(KEY_PROCESSING_MODE, this.processingMode == null ? MODE_PUNCTUATION : this.processingMode);
        ed.putString(KEY_RULES, rulesToString(this.rules));
        ed.putString(KEY_CUSTOM_EMOTICONS, join(this.customEmoticons, "\n"));
        ed.apply();
    }

    public String[] getActiveEmoticons() {
        if (this.customEmoticons != null && this.customEmoticons.length > 0) {
            return this.customEmoticons;
        }
        return BUILTIN_EMOTICONS;
    }

    private static String join(String[] arr, String delim) {
        StringBuilder sb = new StringBuilder();
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                String s = arr[i];
                if (s == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(delim);
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }
}