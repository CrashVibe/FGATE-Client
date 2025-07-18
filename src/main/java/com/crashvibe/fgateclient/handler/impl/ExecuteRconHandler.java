package com.crashvibe.fgateclient.handler.impl;

import com.crashvibe.fgateclient.handler.RequestHandler;
import com.crashvibe.fgateclient.service.RconManager;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.google.gson.JsonObject;

/**
 * 执行 RCON 命令请求处理器
 */
public class ExecuteRconHandler extends RequestHandler {

    private final RconManager rconManager;

    public ExecuteRconHandler(WebSocketManager webSocketManager, RconManager rconManager) {
        super(webSocketManager);
        this.rconManager = rconManager;
    }

    @Override
    public String getMethod() {
        return "execute.rcon";
    }

    @Override
    public void handle(JsonObject request) {
        String requestId = getRequestId(request);
        if (requestId == null) {
            return; // 无效请求ID
        }

        try {
            JsonObject params = getParams(request);
            if (params == null || !params.has("command")) {

                sendErrorResponse(requestId, "Argument 'command' is required, but it's missing!");
                return;
            }

            String command = params.get("command").getAsString();

            if (!rconManager.isAvailable()) {
                sendErrorResponse(requestId, "RCON service is unavailable.");
                return;
            }

            String output = rconManager.executeCommand(command);

            JsonObject result = new JsonObject();
            result.addProperty("output", output);
            result.addProperty("success", true);

            sendSuccessResponse(requestId, result);

        } catch (Exception e) {
            sendErrorResponse(requestId, "Fail to excuse RCON command because: " + e.getMessage());
        }
    }
}
