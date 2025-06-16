package com.crashvibe.fgateclient.handler.impl;

import com.google.gson.JsonObject;
import com.crashvibe.fgateclient.handler.RequestHandler;
import com.crashvibe.fgateclient.service.PlayerManager;
import com.crashvibe.fgateclient.service.WebSocketManager;

/**
 * 踢出玩家请求处理器
 */
public class KickPlayerHandler extends RequestHandler {

    private final PlayerManager playerManager;

    public KickPlayerHandler(WebSocketManager webSocketManager, PlayerManager playerManager) {
        super(webSocketManager);
        this.playerManager = playerManager;
    }

    @Override
    public String getMethod() {
        return "kick.player";
    }

    @Override
    public void handle(JsonObject request) {
        if (!request.has("params")) {
            return; // 通知类型请求，无需响应
        }

        JsonObject params = request.getAsJsonObject("params");
        String playerIdentifier = params.has("player") ? params.get("player").getAsString()
                : params.has("uuid") ? params.get("uuid").getAsString() : null;
        String reason = params.has("reason") ? params.get("reason").getAsString() : "You are kicked from this server.";

        if (playerIdentifier != null) {
            playerManager.kickPlayer(playerIdentifier, reason);
        }
    }
}
