package com.litesuggar.fgateclient.handler;

import com.google.gson.JsonObject;
import com.tcoded.folialib.FoliaLib;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 请求分发器 - 负责将请求分发给对应的处理器
 */
public class RequestDispatcher {

    private final Logger logger;
    private final FoliaLib foliaLib;
    private final Map<String, RequestHandler> handlers = new HashMap<>();

    public RequestDispatcher(Logger logger, FoliaLib foliaLib) {
        this.logger = logger;
        this.foliaLib = foliaLib;
    }

    /**
     * 注册请求处理器
     */
    public void registerHandler(RequestHandler handler) {
        handlers.put(handler.getMethod(), handler);
        logger.info("注册请求处理器: " + handler.getMethod());
    }

    /**
     * 分发请求
     */
    public void dispatch(String method, JsonObject request) {
        RequestHandler handler = handlers.get(method);
        if (handler == null) {
            logger.warning("未知的请求方法: " + method);
            return;
        }

        // 异步处理请求
        foliaLib.getScheduler().runAsync(task -> {
            try {
                handler.handle(request);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "处理请求时发生错误: " + method, e);
            }
        });
    }

    /**
     * 获取已注册的处理器数量
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
