package com.crashvibe.fgateclient.handler;

import com.google.gson.JsonObject;
import com.crashvibe.fgateclient.service.WebSocketManager;
import org.jetbrains.annotations.Nullable;

/**
 * WebSocket 请求处理器基类
 */
public abstract class RequestHandler {

    protected final WebSocketManager webSocketManager;

    public RequestHandler(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }

    /**
     * 获取处理器支持的方法名
     */
    public abstract String getMethod();

    /**
     * 处理请求
     */
    public abstract void handle(JsonObject request);

    /**
     * 发送成功响应
     */
    protected void sendSuccessResponse(String requestId, JsonObject result) {
        webSocketManager.sendResponse(requestId, result, null);
    }

    /**
     * 发送错误响应
     */
    protected void sendErrorResponse(String requestId, String error) {
        webSocketManager.sendResponse(requestId, null, error);
    }

    /**
     * 检查请求是否有必需的参数
     */
    protected boolean hasRequiredParams(JsonObject request, String... paramNames) {
        if (!request.has("params")) {
            return false;
        }

        JsonObject params = request.getAsJsonObject("params");
        for (String paramName : paramNames) {
            if (!params.has(paramName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取请求ID
     */
    @Nullable
    protected String getRequestId(JsonObject request) {
        return request.has("id") ? request.get("id").getAsString() : null;
    }

    /**
     * 获取请求参数
     */
    @Nullable
    protected JsonObject getParams(JsonObject request) {
        return request.has("params") ? request.getAsJsonObject("params") : null;
    }
}
