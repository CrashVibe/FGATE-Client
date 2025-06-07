package com.litesugar.fgateclient.LibWebSocket;

import com.litesugar.fgateclient.FGateClient;

import javax.websocket.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class AsyncSocketClient extends Endpoint {

    // 连接同步锁
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    public final static int API_VERSION = 1;
    protected Session session;
    Logger logger = FGateClient.getPlugin(FGateClient.class).getLogger();

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        logger.info("Connected to server: " + session.getId());
        this.session = session;
        connectLatch.countDown();// 释放锁
        // 线程池，启动！！！
        session.addMessageHandler((MessageHandler.Whole<String>) (message) ->
                CompletableFuture.runAsync(() -> new MessageRunner().runner(message,this)));

    }


    @OnClose
    public void onClose(Session session, CloseReason reason) {
        logger.info("Disconnected: " + reason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error:");
        throwable.printStackTrace();
    }

    /**
     * 异步发送文本消息
     *
     * @param message 文本消息
     * @return CompletableFuture<Boolean> 发送结果
     */
    public CompletableFuture<Boolean> sendTextAsync(String message) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        session.getAsyncRemote().sendText(message, result -> {
            if (result.isOK()) {
                resultFuture.complete(true);
            } else {
                resultFuture.completeExceptionally(result.getException());
            }
        });

        return resultFuture;
    }

    /**
     * 异步发送二进制消息
     *
     * @param data 二进制数据
     * @return CompletableFuture<Boolean> 发送结果
     */
    public CompletableFuture<Boolean> sendBinaryAsync(ByteBuffer data) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        session.getAsyncRemote().sendBinary(data, result -> {
            if (result.isOK()) {
                resultFuture.complete(true);
            } else {
                resultFuture.completeExceptionally(result.getException());
            }
        });

        return resultFuture;
    }

    /**
     * 异步Ping操作
     *
     * @return CompletableFuture<Boolean> 发送结果
     */
    public CompletableFuture<Boolean> sendPingAsync() {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        try {
            session.getAsyncRemote().sendPing(ByteBuffer.wrap("PING".getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return resultFuture;
    }

    /**
     * 等待连接建立
     *
     * @param timeout 超时时间(秒)
     * @throws InterruptedException 等待中断
     */
    public void awaitConnection(int timeout) throws InterruptedException {
        if (!connectLatch.await(timeout, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Connection timeout");
        }
    }


}