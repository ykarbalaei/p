package io.github.some_example_name.shared.model;

import io.github.some_example_name.shared.model.actions.Action;
import io.github.some_example_name.shared.model.actions.MoveAction;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GameState implements Serializable {
    private Map<String, int[]> playerPositions = new HashMap<>();
    private Map<String, String> playerMapIds = new HashMap<>(); // مثلاً player1 → farm1.tmx

    public Map<String, String> getPlayerMapIds() {
        return playerMapIds;
    }
    public synchronized void addPlayer(String playerId, String mapId, int x, int y) {
        // ساخت یا ثبت پلیر جدید
        setPlayerMapId(playerId, mapId);
        setPlayerPosition(playerId, x, y);
    }


    public void apply(Action action) {
        if (action instanceof MoveAction) {
            MoveAction move = (MoveAction) action;

            // موقعیت فعلی بازیکن رو بگیر
            int[] pos = playerPositions.getOrDefault(move.getPlayerId(), new int[]{0, 0});

            // به‌روزرسانی موقعیت
            pos[0] += move.getDx();
            pos[1] += move.getDy();

            // ذخیره در نقشه
            playerPositions.put(move.getPlayerId(), pos);
        }
    }

    public synchronized void applyMove(String playerId, int dx, int dy) {
        int[] pos = playerPositions.getOrDefault(playerId, new int[]{0,0});
        pos[0] += dx;
        pos[1] += dy;
        playerPositions.put(playerId, pos);
        System.out.println("applyMove " + playerId + " -> " + dx + "," + dy);
    }

    // generic update (در صورت نیاز)
    public synchronized void setPlayerPosition(String playerId, int x, int y) {
        playerPositions.put(playerId, new int[]{x,y});
        System.out.println("add position: " + playerPositions.size());
    }

    public synchronized Map<String, int[]> getPlayerPositions() {
        // بازگردانی کپی یا unmodifiable برای امنیت نخ‌ها
        return Collections.unmodifiableMap(new HashMap<>(playerPositions));
    }

    public synchronized void setPlayerMapId(String playerId, String mapId) {
        playerMapIds.put(playerId, mapId);
    }
}
