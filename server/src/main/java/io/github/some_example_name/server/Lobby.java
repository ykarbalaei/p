package io.github.some_example_name.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.some_example_name.shared.model.GameState;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lobby {
    private final Set<String> readyPlayers = new HashSet<>();
    private final List<PlayerConnection> players = new ArrayList<>();
    private final int maxPlayers;
    private boolean gameStarted = false;
    private final GameState gameState = MainServer.getGameState(); // state اختصاصی این لابی
    private GameInstance instance;
    private final ObjectMapper mapper = new ObjectMapper();

    public Lobby(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public synchronized boolean addPlayer(Socket socket) {
        if (gameStarted || players.size() >= maxPlayers) return false;
        String pid = "player" + (players.size() + 1);
        try {
            PlayerConnection pc = new PlayerConnection(socket, this, pid);
            players.add(pc);
            pc.startListening();
// set initial map & position for the joining player
            String mapName = "farm1.tmx";
// ساده: دو موقعیت شروع متفاوت برای دو بازیکن
            int startX = 100 + (players.size()-1) * 200;
            int startY = 100;
            gameState.addPlayer(pid, mapName, startX, startY);
// optionally send the initial state to the new player immediately
            String init = mapper.writeValueAsString(java.util.Map.of("type","STATE","payload", gameState));
            pc.sendRaw(init);

            System.out.println("Player joined lobby (" + players.size() + "/" + maxPlayers + ") id=" + pid);

            if (players.size() == maxPlayers) {
                startGameAndNotify();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private synchronized void startGameAndNotify() {
        if (gameStarted) return;
        gameStarted = true; // بستن لابی به بازیکنان جدید
        System.out.println("🎮 Sending START to " + players.size() + " players...");

        for (int i = 0; i < players.size(); i++) {
            PlayerConnection pc = players.get(i);
            String mapName = "farm1.tmx";
            String msg = String.format("{\"type\":\"START\",\"map\":\"%s\",\"playerId\":\"%s\"}", mapName, pc.getPlayerId());
            pc.sendRaw(msg);
            System.out.println("→ Sent START to " + pc.getPlayerId() + ": " + msg);
        }

        // حالا منتظر READY از کلاینت‌ها بمان (onPlayerReady فراخوانی می‌شود)
        readyPlayers.clear();
        System.out.println("Waiting for READY from players...");
    }


    /** وقتی PlayerConnection اکشنی فرستاد این تابع صدا زده می‌شود */
    public synchronized void onActionReceived(PlayerConnection from, io.github.some_example_name.shared.model.actions.Action action) {
        // می‌توان توی لاگ هم بنویسیم
        System.out.println("Received action from " + from.getPlayerId() + ": " + action);

        // اعمال اکشن روی gameState (باید synchronized باشه)
        synchronized (gameState) {
            gameState.apply(action);
        }
        // اگر خواستی می‌تونی فورا یک ACK برای فرستنده بفرستی:
        try {
            String ack = mapper.writeValueAsString(java.util.Map.of("type","ACK","actionId", action.getId()));
            from.sendRaw(ack);
        } catch (Exception ignored) {}
        // لازم نیست همینجا broadcast کنید چون GameInstance دوره‌ای می‌فرسته.
    }

    public synchronized void onPlayerDisconnected(PlayerConnection pc) {
        System.out.println("Player disconnected: " + pc.getPlayerId());
        players.remove(pc);
        // متوقف کردن instance اگر همه رفتن
        if (players.isEmpty() && instance != null) {
            instance.stop();
            instance = null;
            gameStarted = false;
        }
    }

    public synchronized void onPlayerReady(PlayerConnection pc) {
        readyPlayers.add(pc.getPlayerId());
        System.out.println("Player READY: " + pc.getPlayerId() + " (" + readyPlayers.size() + "/" + players.size() + ")");
        if (readyPlayers.size() == players.size()) {
            actuallyStartGameInstance();
        }
    }

    private synchronized void actuallyStartGameInstance() {
        if (instance != null) return;
        System.out.println("All players READY — starting GameInstance.");
        instance = new GameInstance(players, gameState, 50);
        new Thread(instance, "GameInstance-" + hashCode()).start();
    }


    public boolean isGameStarted() { return gameStarted; }
    public boolean isFull() { return players.size() >= maxPlayers; }
}
