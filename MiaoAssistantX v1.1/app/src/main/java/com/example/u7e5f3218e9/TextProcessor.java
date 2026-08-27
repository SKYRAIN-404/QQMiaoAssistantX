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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextProcessor {
    private static final Random RANDOM = new Random();
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("([，,。！!？?\\s]+)");

    public static String process(String original, CatConfig config) {
        if (original == null || original.trim().isEmpty()) {
            return original;
        }
        String text = original.trim();

        if (config.rules != null) {
            for (CatConfig.Rule rule : config.rules) {
                if (rule == null || rule.from.isEmpty()) {
                    continue;
                }
                text = text.replace(rule.from, rule.to);
            }
        }

        if (config.enableAppend) {
            text = appendPerSentence(text, config.appendText);
        }

        if (config.enableRandomEmoticon) {
            String emoticon = getRandomEmoticon(config);
            if (emoticon != null && !emoticon.isEmpty()) {
                text = text + " " + emoticon;
            }
        }
        return text;
    }

    private static String appendPerSentence(String text, String suffix) {
        String s = (suffix == null) ? "" : suffix;
        List<String> parts = new ArrayList<>();
        List<String> separators = new ArrayList<>();
        Matcher matcher = SENTENCE_SPLIT_PATTERN.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            parts.add(text.substring(lastEnd, matcher.start()));
            separators.add(matcher.group(1));
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            parts.add(text.substring(lastEnd));
        } else if (!parts.isEmpty() && lastEnd == text.length()) {
            parts.add("");
        }
        if (parts.isEmpty()) {
            parts.add(text);
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i).trim();
            if (!part.isEmpty()) {
                result.append(part);
                result.append(s);
            }
            if (i < separators.size()) {
                result.append(separators.get(i));
            }
        }
        String resultStr = result.toString().trim();
        if (resultStr.isEmpty()) {
            return text + s;
        }
        return resultStr;
    }

    private static String getRandomEmoticon(CatConfig config) {
        String[] emoticons = config.getActiveEmoticons();
        if (emoticons == null || emoticons.length == 0) {
            emoticons = CatConfig.BUILTIN_EMOTICONS;
        }
        return emoticons.length == 0 ? "" : emoticons[RANDOM.nextInt(emoticons.length)];
    }

    public static String process(String original) {
        CatConfig defaults = new CatConfig();
        defaults.enableAppend = true;
        defaults.appendText = "喵";
        defaults.enableRandomEmoticon = true;
        defaults.customEmoticons = new String[0];
        defaults.rules = new ArrayList<>();
        return process(original, defaults);
    }
}