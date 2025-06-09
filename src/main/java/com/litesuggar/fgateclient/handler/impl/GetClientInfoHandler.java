package com.litesuggar.fgateclient.handler.impl;

import com.google.gson.JsonObject;
import com.litesuggar.fgateclient.handler.RequestHandler;
import com.litesuggar.fgateclient.service.RconManager;
import com.litesuggar.fgateclient.service.WebSocketManager;
import org.bukkit.Bukkit;

/**
 * 获取客户端信息请求处理器
 */
public class GetClientInfoHandler extends RequestHandler {

    private final RconManager rconManager;

    public GetClientInfoHandler(WebSocketManager webSocketManager, RconManager rconManager) {
        super(webSocketManager);
        this.rconManager = rconManager;
    }

    @Override
    public String getMethod() {
        return "get.client.info";
    }

    @Override
    public void handle(JsonObject request) {
        String requestId = getRequestId(request);
        if (requestId == null) {
            return; // 无效请求ID
        }

        try {
            JsonObject data = new JsonObject();
            data.addProperty("minecraft_version", Bukkit.getVersion());
            data.addProperty("minecraft_software", Bukkit.getName());
            data.addProperty("supports_papi", Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null);
            data.addProperty("supports_rcon", rconManager.isAvailable());

            JsonObject result = new JsonObject();
            result.add("data", data);

            sendSuccessResponse(requestId, result);

        } catch (Exception e) {
            sendErrorResponse(requestId, "获取客户端信息失败: " + e.getMessage());
        }
    }
}
