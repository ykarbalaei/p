package io.github.some_example_name.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.some_example_name.shared.model.GameState;
import io.github.some_example_name.shared.model.actions.MoveAction;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MainServer {
    private static final int PORT = 8080;
    private static final GameState gameState = new GameState();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CopyOnWriteArrayList<PrintWriter> clients = new CopyOnWriteArrayList<>();
private ArrayList<Lobby> lobbies = new ArrayList<>();
    private final ScheduledExecutorService broadCaster = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        new MainServer().start();
    }

    public void start() {
        // start broadcaster
        broadCaster.scheduleAtFixedRate(this::broadcastState, 0, 50, TimeUnit.MILLISECONDS);
        Lobby  lobby=new Lobby(2);
lobbies.add(lobby);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server running on port " + PORT);
            while (true) {
                Socket client = serverSocket.accept();
                lobby.addPlayer(client);
                System.out.println("New client: " + client.getInetAddress());
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            broadCaster.shutdown();
        }
    }

    private void handleClient(Socket socket) {
        PrintWriter out = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);
            clients.add(out);

            // ارسال وضعیت اولیه
            if (gameState.getPlayerPositions() != null && !gameState.getPlayerPositions().isEmpty()
                && gameState.getPlayerMapIds() != null && !gameState.getPlayerMapIds().isEmpty()) {
                out.println(mapper.writeValueAsString(
                    Map.of("type", "STATE", "payload", gameState)
                ));
            }

            System.out.println("client: " + clients.size());

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    System.out.println(",,,,,,,, : " + line);

                    JsonNode n = mapper.readTree(line);
                    if (n.has("type")) {
                        String t = n.get("type").asText();

                        if ("move".equalsIgnoreCase(t)) {
                            System.out.println("move");
                            // پارس MoveAction
                            MoveAction mv = mapper.treeToValue(n, MoveAction.class);
                            synchronized (gameState) {
                                gameState.applyMove(mv.getPlayerId(), mv.getDx(), mv.getDy());
                            }
                            System.out.println("Received move " + mv.getPlayerId() + " -> " + mv.getDx() + "," + mv.getDy());

                            // ارسال به همه کلاینت‌ها
                            String moveJson = mapper.writeValueAsString(mv);
                            for (PrintWriter clientOut : clients) {
                                clientOut.println(moveJson);
                            }
                        }

                        // اینجا می‌تونی انواع اکشن‌های دیگه رو هم هندل کنی
                    } else {
                        System.err.println("Message without type: " + line);
                    }
                } catch (Exception je) {
                    System.err.println("Parse error: " + je.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            if (out != null) clients.remove(out);
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void broadcastState() {
        try {
            if (gameState.getPlayerPositions() == null || gameState.getPlayerPositions().isEmpty()) {
                // وضعیت خالی را ارسال نکن
                return;
            }
            String json = mapper.writeValueAsString(Map.of("type","STATE","payload", gameState));
            for (PrintWriter w : clients) {
                w.println(json);
            }
        } catch (Exception e) {
            System.err.println("broadcast failed: " + e.getMessage());
        }
    }


    public static GameState getGameState() {
        return gameState;
    }
}
