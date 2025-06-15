package com.litesuggar.fgateclient.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 文本工具类 - 支持颜色代码解析
 * 使用Adventure API内置的解析器
 */
public class TextUtil {

    // MiniMessage解析器 - 支持现代格式如 <red>文本</red>
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    // 传统颜色代码解析器 - 支持 & 代码
    private static final LegacyComponentSerializer ampersandSerializer = LegacyComponentSerializer.legacyAmpersand();

    // 传统颜色代码解析器 - 支持 § 代码
    private static final LegacyComponentSerializer sectionSerializer = LegacyComponentSerializer.legacySection();

    // 检测MiniMessage标签的模式
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * 解析包含颜色代码的文本为Adventure Component
     * 支持多种格式：
     * 1. MiniMessage格式：<red>文本</red>, <#ff0000>文本</#ff0000>
     * 2. 传统§代码：§c文本, §4文本
     * 3. 传统&代码：&c文本, &4文本
     * 4. 十六进制格式：&#FF0000文本
     *
     * 注意：换行时颜色会自动重置
     *
     * @param text 包含颜色代码的文本
     * @return 解析后的Component
     */
    public static Component parseText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 处理换行，每行单独解析以确保颜色重置
        if (text.contains("\n")) {
            return parseMultilineText(text);
        }

        // 如果是单行，直接解析
        return parseSingleLineText(text);
    }

    /**
     * 处理多行文本，每行单独解析以确保换行时颜色重置
     */
    private static Component parseMultilineText(String text) {
        String[] lines = text.split("\n", -1); // -1 保留尾部空行

        if (lines.length == 0) {
            return Component.empty();
        }

        if (lines.length == 1) {
            return parseSingleLineText(lines[0]);
        }

        Component result = Component.empty();

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result = result.append(Component.newline());
            }

            String line = lines[i];
            if (!line.isEmpty()) {
                result = result.append(parseSingleLineText(line));
            }
        }

        return result;
    }

    /**
     * 解析单行文本（不包含换行符）
     */
    private static Component parseSingleLineText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 首先处理十六进制颜色格式 &#RRGGBB
        text = convertHexColors(text);

        // 尝试MiniMessage格式
        if (MINI_MESSAGE_PATTERN.matcher(text).find()) {
            try {
                return miniMessage.deserialize(text);
            } catch (Exception e) {
                // 如果MiniMessage解析失败，降级到传统方法
            }
        }

        // 尝试& 代码
        if (text.contains("&")) {
            try {
                return ampersandSerializer.deserialize(text);
            } catch (Exception e) {
                // 继续尝试其他格式
            }
        }

        // 尝试§ 代码
        if (text.contains("§")) {
            try {
                return sectionSerializer.deserialize(text);
            } catch (Exception e) {
                // 继续尝试其他格式
            }
        }

        // 如果都不包含特殊代码，返回纯文本
        return Component.text(text);
    }

    /**
     * 将十六进制颜色格式 &#RRGGBB 转换为 MiniMessage 格式 <#RRGGBB>
     */
    private static String convertHexColors(String text) {
        Pattern hexPattern = Pattern.compile("(?i)&#([A-F0-9]{6})");
        Matcher matcher = hexPattern.matcher(text);

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            text = text.replace(matcher.group(0), "<#" + hexColor + ">");
        }

        return text;
    }

    /**
     * 快速创建带颜色的文本组件
     *
     * @param text  文本内容
     * @param color 颜色
     * @return Component
     */
    public static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    /**
     * 快速创建带十六进制颜色的文本组件
     *
     * @param text     文本内容
     * @param hexColor 十六进制颜色（如 "ff0000"）
     * @return Component
     */
    public static Component colored(String text, String hexColor) {
        try {
            TextColor color = TextColor.fromHexString("#" + hexColor);
            return Component.text(text, color);
        } catch (Exception e) {
            return Component.text(text);
        }
    }
}
