package com.crashvibe.fgateclient.utils;

import com.crashvibe.fgateclient.config.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture;

public class I18n {
    private final Map<String, YamlConfiguration> cache = new HashMap<>();
    private ConfigManager configManager;
    private String lang;
    private String fallbackLang;
    private File baseDir;

    public I18n(File dataFolder) {
        this.baseDir = new File(dataFolder, "languages");
        // 延迟初始化，避免循环依赖
    }

    /**
     * 初始化配置管理器，在ServiceManager创建后调用
     */
    public void initialize(ConfigManager configManager) {
        this.configManager = configManager;
        this.fallbackLang = configManager.getFallbackLanguage();
    }

    /**
     * 异步初始化配置管理器
     */
    public CompletableFuture<Void> initializeAsync(ConfigManager configManager) {
        return CompletableFuture.runAsync(() -> {
            this.configManager = configManager;
            this.fallbackLang = configManager.getFallbackLanguage();
        });
    }

    public void setFallbackLang(String fallback) {
        this.fallbackLang = fallback;
    }

    /**
     * 检查指定语言文件是否存在
     */
    private boolean isLanguageFileExists(String language) {
        if (language == null)
            return false;
        File langFile = new File(baseDir, language + ".yml");
        return langFile.exists();
    }

    /**
     * 获取有效的语言代码，如果语言文件不存在则使用 fallback
     */
    private String getValidLanguage(String requestedLang) {
        if (isLanguageFileExists(requestedLang)) {
            return requestedLang;
        }

        // 如果请求的语言文件不存在，尝试使用默认语言
        if (isLanguageFileExists(lang)) {
            return lang;
        }

        // 如果默认语言文件也不存在，使用 fallback 语言
        if (isLanguageFileExists(fallbackLang)) {
            return fallbackLang;
        }

        // 如果 fallback 语言文件也不存在，返回请求的语言（让系统处理文件不存在的情况）
        return requestedLang != null ? requestedLang : lang;
    }

    public String getLangForPlayer(Player player) {
        // 如果configManager还未初始化，使用默认值
        if (configManager == null) {
            return "en_US"; // 使用默认语言
        }

        // 获取玩家客户端的locale设置
        String clientLocale = player.locale().toString();
        if (clientLocale != null && !clientLocale.isEmpty()) {
            String convertedLocale = convertMinecraftLocale(clientLocale);
            return getValidLanguage(convertedLocale);
        }
        return getValidLanguage(lang);
    }

    private String convertMinecraftLocale(String minecraftLocale) {
        if (minecraftLocale == null || minecraftLocale.isEmpty()) {
            return lang != null ? lang : "en_US"; // 提供默认值
        }

        String[] parts = minecraftLocale.split("_");
        if (parts.length == 2) {
            return parts[0].toLowerCase() + "_" + parts[1].toUpperCase();
        }

        return minecraftLocale;
    }

    public String get(String key) {
        YamlConfiguration config = getLangConfig();
        String value = config.getString(key, null);
        if (value == null && fallbackLang != null && !fallbackLang.equals(lang)) {
            YamlConfiguration fallbackConfig = getFallbackConfig();
            value = fallbackConfig.getString(key, key);
        }
        return value != null ? value : key;
    }

    public String get(String key, Player player) {
        String oldLang = lang;
        lang = getLangForPlayer(player);
        String value = get(key);
        lang = oldLang;
        return value;
    }

    public String format(String key, Map<String, String> params) {
        String msg = get(key);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return msg;
    }

    public String format(String key, Map<String, String> params, Player player) {
        String oldLang = lang;
        lang = getLangForPlayer(player);
        String msg = get(key);
        lang = oldLang;
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return msg;
    }

    private YamlConfiguration getFallbackConfig() {
        if (baseDir == null || fallbackLang == null)
            throw new IllegalStateException("I18n baseDir or fallbackLang not initialized");

        String validFallbackLang = getValidLanguage(fallbackLang);
        String fileName = validFallbackLang + ".yml";

        if (!cache.containsKey(fileName)) {
            File file = new File(baseDir, fileName);
            cache.put(fileName, YamlConfiguration.loadConfiguration(file));
        }
        return cache.get(fileName);
    }

    private YamlConfiguration getLangConfig() {
        if (baseDir == null || lang == null)
            throw new IllegalStateException("I18n baseDir or lang not initialized");

        String validLang = getValidLanguage(lang);
        String fileName = validLang + ".yml";

        if (!cache.containsKey(fileName)) {
            File file = new File(baseDir, fileName);
            if (!file.exists() && fallbackLang != null) {
                String validFallbackLang = getValidLanguage(fallbackLang);
                file = new File(baseDir, validFallbackLang + ".yml");
                fileName = validFallbackLang + ".yml";
            }
            cache.put(fileName, YamlConfiguration.loadConfiguration(file));
        }
        return cache.get(fileName);
    }

    /**
     * 异步预加载语言文件到缓存
     */
    public CompletableFuture<Void> preloadLanguageFilesAsync() {
        return CompletableFuture.runAsync(() -> {
            if (baseDir == null || !baseDir.exists()) {
                return;
            }

            File[] langFiles = baseDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (langFiles != null) {
                for (File langFile : langFiles) {
                    String fileName = langFile.getName();
                    if (!cache.containsKey(fileName)) {
                        try {
                            cache.put(fileName, YamlConfiguration.loadConfiguration(langFile));
                        } catch (Exception e) {
                            // 记录但不中断，继续加载其他文件
                            System.err.println("Failed to preload language file: " + fileName);
                        }
                    }
                }
            }
        });
    }

    /**
     * 异步清理缓存
     */
    public CompletableFuture<Void> clearCacheAsync() {
        return CompletableFuture.runAsync(() -> {
            cache.clear();
        });
    }
}
