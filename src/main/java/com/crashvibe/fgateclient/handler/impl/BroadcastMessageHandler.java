package com.crashvibe.fgateclient.handler.impl;

import com.google.gson.JsonObject;
import com.crashvibe.fgateclient.handler.RequestHandler;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.crashvibe.fgateclient.utils.TextUtil;
import com.tcoded.folialib.FoliaLib;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * 广播消息处理器 - 处理来自主机端的广播消息
 */
public class BroadcastMessageHandler extends RequestHandler {

    private final Logger logger;
    private final FoliaLib foliaLib;

    public BroadcastMessageHandler(WebSocketManager webSocketManager, Logger logger, FoliaLib foliaLib) {
        super(webSocketManager);
        this.logger = logger;
        this.foliaLib = foliaLib;
    }

    @Override
    public String getMethod() {
        return "broadcast.message";
    }

    @Override
    public void handle(JsonObject request) {
        if (!request.has("params")) {
            logger.warning("Broadcast message request missing params");
            return;
        }

        JsonObject params = request.getAsJsonObject("params");
        if (!params.has("message")) {
            logger.warning("Broadcast message request missing message parameter");
            return;
        }

        String message = params.get("message").getAsString();

        // 解析包含颜色代码的消息
        Component messageComponent = TextUtil.parseText(message);

        // 在主线程中发送消息给所有在线玩家
        foliaLib.getScheduler().runAsync(task -> {
            Bukkit.broadcast(messageComponent);

            // 记录日志
            if (logger != null) {
                logger.info("Broadcasted message to all players: " + TextUtil.stripColors(message));
            }
        });
    }
}
