package com.litesuggar.fgateclient.handler.impl;

import com.google.gson.JsonObject;
import com.litesuggar.fgateclient.handler.RequestHandler;
import com.litesuggar.fgateclient.service.RconManager;
import com.litesuggar.fgateclient.service.WebSocketManager;

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
                sendErrorResponse(requestId, "缺少必需的 'command' 参数");
                return;
            }

            String command = params.get("command").getAsString();

            if (!rconManager.isAvailable()) {
                sendErrorResponse(requestId, "RCON 服务不可用");
                return;
            }

            String output = rconManager.executeCommand(command);

            JsonObject result = new JsonObject();
            result.addProperty("output", output);
            result.addProperty("success", true);

            sendSuccessResponse(requestId, result);

        } catch (Exception e) {
            sendErrorResponse(requestId, "执行 RCON 命令失败: " + e.getMessage());
        }
    }
}
