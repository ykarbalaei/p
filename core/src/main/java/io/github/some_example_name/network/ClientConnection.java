package io.github.some_example_name.network;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.some_example_name.screens.LobbyScreen;
import io.github.some_example_name.shared.model.GameState;
import io.github.some_example_name.shared.model.actions.Action;
import io.github.some_example_name.views.GameView;

import java.io.*;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientConnection {
    private final String host;
    private final int port;
    private BiConsumer<String,String> onStart; // (mapName, playerId)
    public void setOnStart(BiConsumer<String,String> listener) { this.onStart = listener; }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();

    // callback// دریافت پیام START -> mapName
    private Consumer<GameState> onState;     // دریافت payload STATE -> GameState

    private volatile boolean running = false;
    private Thread readerThread;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

//    /** ثبت callback برای START */
//    public void setOnStart(Consumer<String> listener) {
//        this.onStart = listener;
//
//    }

    /** ثبت callback برای GameState */
    public void setOnGameState(Consumer<GameState> listener) {
        this.onState = listener;
    }

    /**
     * اتصال به سرور و شروع نخ دریافت.
     * باید از ترد گرافیکی فراخوانده شود (یا در show()).
     */
    public void connect() throws IOException {
        if (running) return;
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        readerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    // اگر خط خالی یا whitespace است نادیده بگیر
                    if (line.trim().isEmpty()) continue;

                    try {
                        System.out.println("ClientConnection: got line -> " + line);
                        JsonNode node = mapper.readTree(line);
                        JsonNode typeNode = node.get("type");
                        if (typeNode == null) continue;
                        String type = typeNode.asText();

                        switch (type) {
                            case "START": {
                                JsonNode mapNode = node.get("map");
                                JsonNode pidNode  = node.get("playerId");
                                String mapName = mapNode != null ? mapNode.asText() : null;
                                String playerId = pidNode != null ? pidNode.asText() : null;
                                if (onStart != null) {
                                    Gdx.app.postRunnable(() -> onStart.accept(mapName, playerId));
                                }
                                break;
                            }
                            case "STATE": {
                                JsonNode payload = node.get("payload");
                                GameState state = mapper.treeToValue(payload, GameState.class);
                                GameView gameView= LobbyScreen.getGameView();
                                if (state.getPlayerPositions() == null || state.getPlayerPositions().isEmpty() ||
                                    state.getPlayerMapIds() == null || state.getPlayerMapIds().isEmpty()) {
                                    System.out.println("Received empty or incomplete STATE, ignoring.");
                                    break; // به‌روزرسانی انجام نشود
                                }
                                if(gameView != null) {
                                    gameView.applyGameState(state);
                                }

                                if (onState != null) {
                                    Gdx.app.postRunnable(() -> onState.accept(state));
                                }
                                break;
                            }

                            case "ACK": {
                                // در صورت نیاز پردازش ACK
                                break;
                            }
                            default: {
                                // پیام خام یا دلخواه — می‌تونی لاگ کنی یا پردازش کنی
                                System.out.println("ClientConnection: unknown message type: " + type);
                            }
                        }
                    } catch (Exception je) {
                        System.err.println("ClientConnection: failed parsing JSON line: " + je.getMessage());
                        je.printStackTrace();
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("ClientConnection: reader IO error: " + e.getMessage());
                }
            } finally {
                closeSilently();
            }
        }, "ClientConnection-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * ارسال اکشن به سرور (به صورت JSON در یک خط)
     */
    public synchronized void sendAction(Action a) throws IOException {
        if (!isConnected()) throw new IOException("not connected");
        String json = mapper.writeValueAsString(a);
        System.out.println("ClientConnection: sent action: " + json);
        out.println(json); // println اضافهٔ '\n' را می‌زند
        out.flush();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /** بستن اتصال */
    public synchronized void close() {
        running = false;
        closeSilently();
        if (readerThread != null) {
            try {
                readerThread.join(200);
            } catch (InterruptedException ignored) {}
        }
    }

    private void closeSilently() {
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        out = null; reader = null; socket = null;
    }

    public synchronized void sendMessage(java.util.Map<String,Object> msg) throws IOException {
        if (!isConnected()) throw new IOException("not connected");
        String json = mapper.writeValueAsString(msg);
        out.println(json);
        out.flush();
    }


    public void sendReady(String playerId) throws IOException {
        sendMessage(java.util.Map.of("type","READY","playerId", playerId));
    }

}
