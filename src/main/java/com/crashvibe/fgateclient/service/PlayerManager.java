package com.crashvibe.fgateclient.service;

import com.crashvibe.fgateclient.utils.TextUtil;
import com.tcoded.folialib.FoliaLib;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 玩家管理器 - 负责玩家相关操作
 */
@SuppressWarnings("unused")
public class PlayerManager {

    private final Logger logger;
    private final FoliaLib foliaLib;

    public PlayerManager(Logger logger, FoliaLib foliaLib) {
        this.logger = logger;
        this.foliaLib = foliaLib;
    }

    @SuppressWarnings("deprecation")
    public void kickPlayer(String playerIdentifier, String reason) {
        foliaLib.getScheduler().runAsync(task -> {
            Player player = findPlayer(playerIdentifier);

            if (player != null && player.isOnline()) {
                // 在主线程执行踢出操作
                foliaLib.getScheduler().runNextTick(kickTask -> {
                    // 使用TextUtil解析颜色代码，支持多行文本和换行时颜色重置
                    Component reasonComponent = TextUtil.parseMultilineText(reason);
                    player.kick(reasonComponent);
                    logger.info("Kicked player " + player.getName() + " for: " + reason);
                });
            } else {
                logger.warning("Who is player '" + playerIdentifier + "'?");
            }
        });
    }

    public boolean isPlayerOnline(String playerIdentifier) {
        Player player = findPlayer(playerIdentifier);
        return player != null && player.isOnline();
    }

    private Player findPlayer(String playerIdentifier) {
        // 首先尝试按玩家名查找
        Player player = Bukkit.getPlayer(playerIdentifier);

        if (player == null) {
            // 尝试通过UUID查找
            try {
                UUID uuid = UUID.fromString(playerIdentifier);
                player = Bukkit.getPlayer(uuid);
            } catch (IllegalArgumentException ignored) {
                // 不是有效的UUID，可能是玩家名
            }
        }

        return player;
    }
}
