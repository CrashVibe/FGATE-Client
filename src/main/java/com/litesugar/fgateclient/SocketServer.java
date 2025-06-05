package com.litesugar.fgateclient;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class SocketServer extends WebSocketServer {
    Logger logger = FGateClient.getPlugin(FGateClient.class).getLogger();
    private final String token = FGateClient.getPlugin(FGateClient.class).getConfig().getString("auth_token");
    Set<WebSocket> authed_clients;

    public SocketServer(String address, int port) {
        super(new InetSocketAddress(address, port));
    }

    private String prepareClientInfo(WebSocket conn) {
        return "Client " + conn + " (" + conn.getRemoteSocketAddress().getAddress().getHostAddress() + ")";
    }

    private void auth(WebSocket conn, ClientHandshake handshake) {
        if (token != null)
            for (Iterator<String> it = handshake.iterateHttpFields(); it.hasNext(); ) {
                String key = it.next();
                if ("Authorization".equalsIgnoreCase(key)) {
                    String authHeader = handshake.getFieldValue(key);
                    if (authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (token.equals(this.token)) {
                            authed_clients.add(conn);
                        } else {
                            conn.close(4001, "Invalid token");
                        }
                    } else {
                        conn.close(4001, "Invalid token format");
                    }
                }
            }
        else {
            authed_clients.add(conn);
            logger.warning("Token hasn't set yet!All requests will be accepted!");
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        auth(conn, handshake); // 验证Tokens
        JSONObject json = new JSONObject();
        JSONObject status = new JSONObject();
        status.put("status", "success");
        status.put("message", "Welcome " + conn + " to connect FGate");
        status.put("client_id", conn);
        json.put("id", 101);
        json.put("result", status);
        conn.send(json.toString());
        logger.info(
                prepareClientInfo(conn) + " 连接到了客户端。");

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        authed_clients.remove(conn);
        logger.info(prepareClientInfo(conn) + " 断开了连接");

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if(!authed_clients.contains(conn))
            conn.close(4001, "Not authorized");
        logger.info(conn + ": " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            authed_clients.remove(conn);
            conn.close(1013,"Server error!Please try again later.");
        }

    }

    @Override
    public void onStart() {
        logger.info("\033[33mWebSocket服务器已成功启动\033[0m");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);

    }

}

