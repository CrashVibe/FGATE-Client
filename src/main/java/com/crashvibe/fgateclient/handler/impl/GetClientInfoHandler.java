package com.crashvibe.fgateclient.handler.impl;

import com.google.gson.JsonObject;
import com.crashvibe.fgateclient.handler.RequestHandler;
import com.crashvibe.fgateclient.service.RconManager;
import com.crashvibe.fgateclient.service.WebSocketManager;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * 获取客户端信息请求处理器
 */
public class GetClientInfoHandler extends RequestHandler {

    private final RconManager rconManager;
    private final Logger logger;

    public GetClientInfoHandler(WebSocketManager webSocketManager, RconManager rconManager, Logger logger) {
        super(webSocketManager);
        this.rconManager = rconManager;
        this.logger = logger;
    }

    @Override
    public String getMethod() {
        return "get.client.info";
    }

    @Override
    public void handle(JsonObject request) {
        String requestId = getRequestId(request);
        logger.info("Received request: " + request);
        if (requestId == null) {
            logger.warning("Missed request id!");
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
            sendErrorResponse(requestId, "Fail to send client info: " + e.getMessage());
        }
    }
}
