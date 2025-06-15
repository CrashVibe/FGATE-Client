package com.litesuggar.fgateclient.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TextUtil测试类
 */
public class TextUtilTest {

    @Test
    public void testPlainText() {
        Component result = TextUtil.parseText("Hello World");
        assertEquals("Hello World", result.toString());
    }

    @Test
    public void testAmpersandColors() {
        Component result = TextUtil.parseText("&cRed text &aGreen text");
        assertNotNull(result);
        // 验证结果不是纯文本
        assertFalse(result.toString().contains("&c"));
    }

    @Test
    public void testSectionColors() {
        Component result = TextUtil.parseText("§cRed text §aGreen text");
        assertNotNull(result);
        // 验证结果不是纯文本
        assertFalse(result.toString().contains("§c"));
    }

    @Test
    public void testMiniMessage() {
        Component result = TextUtil.parseText("<red>Red text</red> <green>Green text</green>");
        assertNotNull(result);
        // 验证结果不是纯文本
        assertFalse(result.toString().contains("<red>"));
    }

    @Test
    public void testHexColors() {
        Component result = TextUtil.parseText("&#FF0000This is red text");
        assertNotNull(result);
        // 验证结果不是纯文本
        assertFalse(result.toString().contains("&#FF0000"));
    }

    @Test
    public void testColoredHelper() {
        Component result = TextUtil.colored("Test", NamedTextColor.RED);
        assertNotNull(result);
        assertEquals("Test", ((net.kyori.adventure.text.TextComponent) result).content());
    }

    @Test
    public void testColoredWithHex() {
        Component result = TextUtil.colored("Test", "FF0000");
        assertNotNull(result);
        assertEquals("Test", ((net.kyori.adventure.text.TextComponent) result).content());
    }

    @Test
    public void testEmptyText() {
        Component result = TextUtil.parseText("");
        assertEquals(Component.empty(), result);
    }

    @Test
    public void testNullText() {
        Component result = TextUtil.parseText(null);
        assertEquals(Component.empty(), result);
    }
}
