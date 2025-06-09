package com.litesuggar.fgateclient.service;

import com.litesuggar.fgateclient.config.ConfigManager;
import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RCON 管理器 - 负责RCON连接和命令执行
 */
public class RconManager {

    private final Logger logger;
    private final FoliaLib foliaLib;
    private final ConfigManager configManager;

    // 外部 RCON 相关字段
    private Socket rconSocket;
    private DataOutputStream rconOut;
    private DataInputStream rconIn;
    private boolean rconConnected = false;
    private int requestId = 1;

    public RconManager(Logger logger, FoliaLib foliaLib, ConfigManager configManager) {
        this.logger = logger;
        this.foliaLib = foliaLib;
        this.configManager = configManager;

        if (!configManager.isUseBuiltinRcon() && configManager.isRconConfigured()) {
            initializeExternalRcon();
        }
    }

    public boolean isAvailable() {
        if (configManager.isUseBuiltinRcon()) {
            return true; // 内置RCON总是可用的
        } else {
            return rconConnected;
        }
    }

    public String executeCommand(String command) throws Exception {
        if (configManager.isUseBuiltinRcon()) {
            return executeBuiltinCommand(command);
        } else {
            return executeExternalRconCommand(command);
        }
    }

    public void close() {
        if (rconSocket != null) {
            try {
                rconSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "关闭 RCON 连接时出错", e);
            }
        }
        rconConnected = false;
    }

    private String executeBuiltinCommand(String command) throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        foliaLib.getScheduler().runAsync(task -> {
            foliaLib.getScheduler().runNextTick(task1 -> {
                try {
                    ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();
                    boolean success = Bukkit.dispatchCommand(consoleSender, command);
                    String output = success ? "命令执行成功" : "命令执行失败";
                    future.complete(output);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        });

        return future.get(10, TimeUnit.SECONDS);
    }

    private String executeExternalRconCommand(String command) throws Exception {
        if (!rconConnected) {
            throw new Exception("RCON 未连接");
        }

        synchronized (this) {
            int id = requestId++;

            // 发送RCON命令
            sendRconPacket(id, 2, command); // Type 2 = SERVERDATA_EXECCOMMAND

            // 读取响应
            RconPacket response = readRconPacket();

            if (response.id != id) {
                throw new Exception("RCON 响应 ID 不匹配");
            }

            return response.body;
        }
    }

    private void initializeExternalRcon() {
        try {
            rconSocket = new Socket(configManager.getRconHost(), configManager.getRconPort());
            rconOut = new DataOutputStream(rconSocket.getOutputStream());
            rconIn = new DataInputStream(rconSocket.getInputStream());

            // 认证
            sendRconPacket(1, 3, configManager.getRconPassword()); // Type 3 = SERVERDATA_AUTH
            RconPacket authResponse = readRconPacket();

            if (authResponse.id == -1) {
                throw new Exception("RCON 认证失败");
            }

            rconConnected = true;
            logger.info("外部 RCON 连接成功");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "连接外部 RCON 失败", e);
            rconConnected = false;
        }
    }

    private void sendRconPacket(int id, int type, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        int length = 4 + 4 + bodyBytes.length + 2; // id + type + body + 2 null bytes

        ByteBuffer buffer = ByteBuffer.allocate(4 + length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(length);
        buffer.putInt(id);
        buffer.putInt(type);
        buffer.put(bodyBytes);
        buffer.put((byte) 0);
        buffer.put((byte) 0);

        rconOut.write(buffer.array());
        rconOut.flush();
    }

    private RconPacket readRconPacket() throws IOException {
        byte[] lengthBytes = new byte[4];
        rconIn.readFully(lengthBytes);

        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
        lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int length = lengthBuffer.getInt();

        byte[] packetBytes = new byte[length];
        rconIn.readFully(packetBytes);

        ByteBuffer packetBuffer = ByteBuffer.wrap(packetBytes);
        packetBuffer.order(ByteOrder.LITTLE_ENDIAN);

        int id = packetBuffer.getInt();
        int type = packetBuffer.getInt();

        byte[] bodyBytes = new byte[length - 10]; // length - id - type - 2 null bytes
        packetBuffer.get(bodyBytes);

        String body = new String(bodyBytes, "UTF-8").trim();

        return new RconPacket(id, type, body);
    }

    private static class RconPacket {
        final int id;
        final int type;
        final String body;

        RconPacket(int id, int type, String body) {
            this.id = id;
            this.type = type;
            this.body = body;
        }
    }
}
