package com.litesuggar.fgateclient.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 文本处理工具类 - 支持多种颜色代码格式
 */
public class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    // 匹配 &#RRGGBB 格式的十六进制颜色代码
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * 解析包含颜色代码的文本为 Adventure Component
     * 支持以下格式：
     * - MiniMessage格式: <red>text</red>
     * - & 符号格式: &ctext
     * - § 符号格式: §ctext
     * - 十六进制格式: &#FF0000text
     *
     * @param text 包含颜色代码的文本
     * @return 解析后的Component
     */
    public static Component parseText(String text) {
        return parseText(text, true);
    }

    /**
     * 解析包含颜色代码的文本为 Adventure Component
     * 支持以下格式：
     * - MiniMessage格式: <red>text</red>
     * - & 符号格式: &ctext
     * - § 符号格式: §ctext
     * - 十六进制格式: &#FF0000text
     *
     * @param text           包含颜色代码的文本
     * @param resetOnNewline 是否在换行时重置颜色
     * @return 解析后的Component
     */
    public static Component parseText(String text, boolean resetOnNewline) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 如果需要在换行时重置颜色，先预处理文本
        if (resetOnNewline) {
            text = processNewlineColorReset(text);
        }

        // 优先尝试解析十六进制颜色代码
        text = convertHexColors(text);

        // 尝试解析 MiniMessage 格式
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (Exception e) {
                // 如果 MiniMessage 解析失败，继续尝试其他格式
            }
        }

        // 尝试解析 & 符号格式
        if (text.contains("&")) {
            try {
                return LEGACY_AMPERSAND.deserialize(text);
            } catch (Exception e) {
                // 如果解析失败，继续尝试其他格式
            }
        }

        // 尝试解析 § 符号格式
        if (text.contains("§")) {
            try {
                return LEGACY_SECTION.deserialize(text);
            } catch (Exception e) {
                // 如果解析失败，返回纯文本
            }
        }

        // 如果都解析失败，返回纯文本
        return Component.text(text);
    }

    /**
     * 将 &#RRGGBB 格式的颜色代码转换为 MiniMessage 格式
     */
    private static String convertHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(result, "<color:#" + hexColor + ">");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理换行时的颜色重置
     * 在每个换行符后添加重置代码
     */
    private static String processNewlineColorReset(String text) {
        if (text == null || !text.contains("\n")) {
            return text;
        }

        // 检测文本使用的颜色代码格式
        boolean hasMiniMessage = text.contains("<") && text.contains(">");
        boolean hasAmpersand = text.contains("&");
        boolean hasSection = text.contains("§");
        boolean hasHex = text.contains("&#");

        String resetCode;
        if (hasMiniMessage || hasHex) {
            // 使用 MiniMessage 格式的重置代码
            resetCode = "<reset>";
        } else if (hasAmpersand) {
            // 使用 & 格式的重置代码
            resetCode = "&r";
        } else if (hasSection) {
            // 使用 § 格式的重置代码
            resetCode = "§r";
        } else {
            // 如果没有颜色代码，直接返回原文本
            return text;
        }

        // 将所有换行符替换为换行符+重置代码
        return text.replace("\n", "\n" + resetCode);
    }

    /**
     * 创建带颜色的文本组件
     *
     * @param text  文本内容
     * @param color 颜色
     * @return 带颜色的Component
     */
    public static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    /**
     * 创建带十六进制颜色的文本组件
     *
     * @param text     文本内容
     * @param hexColor 十六进制颜色代码（不带#）
     * @return 带颜色的Component
     */
    public static Component colored(String text, String hexColor) {
        try {
            TextColor color = TextColor.fromHexString("#" + hexColor);
            return Component.text(text, color);
        } catch (Exception e) {
            // 如果颜色代码无效，返回无颜色的文本
            return Component.text(text);
        }
    }

    /**
     * 创建带RGB颜色的文本组件
     *
     * @param text  文本内容
     * @param red   红色值 (0-255)
     * @param green 绿色值 (0-255)
     * @param blue  蓝色值 (0-255)
     * @return 带颜色的Component
     */
    public static Component colored(String text, int red, int green, int blue) {
        try {
            TextColor color = TextColor.color(red, green, blue);
            return Component.text(text, color);
        } catch (Exception e) {
            // 如果颜色值无效，返回无颜色的文本
            return Component.text(text);
        }
    }

    /**
     * 移除文本中的所有颜色代码，返回纯文本
     *
     * @param text 包含颜色代码的文本
     * @return 纯文本
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 移除各种格式的颜色代码
        text = text.replaceAll("&[0-9a-fk-or]", "");
        text = text.replaceAll("§[0-9a-fk-or]", "");
        text = text.replaceAll("&#[A-Fa-f0-9]{6}", "");
        text = text.replaceAll("<[^>]+>", "");

        return text;
    }

    /**
     * 解析多行文本，每行都作为独立的Component处理
     * 自动在换行时重置颜色
     *
     * @param text 多行文本
     * @return Component数组，每个元素代表一行
     */
    public static Component[] parseLines(String text) {
        if (text == null || text.isEmpty()) {
            return new Component[] { Component.empty() };
        }

        String[] lines = text.split("\n");
        Component[] components = new Component[lines.length];

        for (int i = 0; i < lines.length; i++) {
            components[i] = parseText(lines[i], false); // 每行单独处理，不需要换行重置
        }

        return components;
    }

    /**
     * 解析多行文本为单个Component，使用换行符连接
     * 在每个换行时自动重置颜色
     *
     * @param text 多行文本
     * @return 单个Component
     */
    public static Component parseMultilineText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component[] lines = parseLines(text);
        if (lines.length == 1) {
            return lines[0];
        }

        Component result = lines[0];
        for (int i = 1; i < lines.length; i++) {
            result = result.append(Component.newline()).append(lines[i]);
        }

        return result;
    }
}
