package com.litesuggar.fgateclient;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class RconManager {

    private final FGateClient plugin;
    private final boolean useBuiltinRcon;
    private final String rconHost;
    private final int rconPort;
    private final String rconPassword;

    private Socket rconSocket;
    private DataOutputStream rconOut;
    private DataInputStream rconIn;
    private boolean rconConnected = false;
    private int requestId = 1;

    public RconManager(FGateClient plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig();

        this.useBuiltinRcon = config.getBoolean("rcon.use-builtin", true);
        this.rconHost = config.getString("rcon.host", "localhost");
        this.rconPort = config.getInt("rcon.port", 25575);
        this.rconPassword = config.getString("rcon.password", "");

        if (!useBuiltinRcon && isRconConfigured()) {
            initializeExternalRcon();
        }
    }

    public boolean isRconAvailable() {
        if (useBuiltinRcon) {
            return true; // 内置RCON总是可用的
        } else {
            return rconConnected;
        }
    }

    public String executeCommand(String command) throws Exception {
        if (useBuiltinRcon) {
            return executeBuiltinCommand(command);
        } else {
            return executeExternalRconCommand(command);
        }
    }
    private String executeBuiltinCommand(String command) throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        // 外层是异步，内部要切换到主线程（Global Region）
        plugin.foliaLib.getScheduler().runAsync(task -> {
            plugin.foliaLib.getScheduler().runNextTick(task1 -> {
                try {
                    ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();

                    boolean success = Bukkit.dispatchCommand(consoleSender, command);
                    String output = success ? "Command executed successfully" : "Command execution failed";
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
            throw new Exception("RCON not connected");
        }

        synchronized (this) {
            int id = requestId++;

            // 发送RCON命令
            sendRconPacket(id, 2, command); // Type 2 = SERVERDATA_EXECCOMMAND

            // 读取响应
            RconPacket response = readRconPacket();

            if (response.id != id) {
                throw new Exception("RCON response ID mismatch");
            }

            return response.body;
        }
    }

    private void initializeExternalRcon() {
        try {
            rconSocket = new Socket(rconHost, rconPort);
            rconOut = new DataOutputStream(rconSocket.getOutputStream());
            rconIn = new DataInputStream(rconSocket.getInputStream());

            // 认证
            sendRconPacket(1, 3, rconPassword); // Type 3 = SERVERDATA_AUTH
            RconPacket authResponse = readRconPacket();

            if (authResponse.id == -1) {
                throw new Exception("RCON authentication failed");
            }

            rconConnected = true;
            plugin.getLogger().info("External RCON connected successfully");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to external RCON", e);
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

    private boolean isRconConfigured() {
        return rconPassword != null && !rconPassword.isEmpty();
    }

    public void close() {
        if (rconSocket != null) {
            try {
                rconSocket.close();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing RCON connection", e);
            }
        }
        rconConnected = false;
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

    private static class CommandOutputCapture {
        // 这里可以实现更复杂的输出捕获逻辑
        // 由于Bukkit的限制，直接捕获命令输出比较困难
        // 在实际实现中，可能需要使用反射或其他方法
    }
}