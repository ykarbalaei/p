package io.github.some_example_name.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.some_example_name.Main;
import io.github.some_example_name.controllers.GameController;
import io.github.some_example_name.controllers.PreGameMenuController;
import io.github.some_example_name.model.GameAssetManager;
import io.github.some_example_name.model.game.Game;
import io.github.some_example_name.model.game.GameManager;
import io.github.some_example_name.network.ClientConnection;
import io.github.some_example_name.shared.model.GameState;
import io.github.some_example_name.views.GameView;

import java.io.IOException;
import java.util.ArrayList;

public class LobbyScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private Skin skin;

    private ClientConnection client;
    private Label statusLabel;

    public LobbyScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("skin/plain-james-ui.json"));

        Table table = new Table(skin);
        table.setFillParent(true);
        table.center();
        table.pad(20);

        Label title = new Label("Lobby", skin);
        title.setFontScale(2f);

        statusLabel = new Label("⏳ Waiting for other players...", skin);

        table.add(title).padBottom(30).row();
        table.add(statusLabel).pad(10).row();

        stage.addActor(table);

        // اتصال به سرور
        client = new ClientConnection("localhost", 8080);

        client.setOnStart((mapName, playerId) -> {
            System.out.println("🎯 Switching to GameView with map: " + mapName + " id=" + playerId);
            // 1. ساخت بازی در کلاینت با playerId
            ArrayList<String> usernames = new ArrayList<>();
            usernames.add(playerId);
            GameManager.createGame(usernames, playerId);

            // 2. گرفتن گیم ساخته‌شده
            Game game = GameManager.getCurrentGame();

            // 3. ساخت کنترلر و ویو — پاس دادن client و playerId به GameView
            GameController controller = new GameController();
            gameView = new GameView(controller, GameAssetManager.getGameAssetManager().getSkin(), client, playerId);

            // 4. لود مپ برای این کلاینت (فرض بر سینکرون بودن load)
            controller.loadFarmMap();

            // 5. اعلام READY به سرور (بعد از بارگذاری منابع)
            try {
                client.sendReady(playerId);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 6. تغییر صفحه
            Main.getMain().setScreen(gameView);
        });

        // زمانی که وضعیت بازی رسید
        client.setOnGameState(this::onGameStateReceived);

        // زمانی که GAME_START دریافت شد
//

        try {
            client.connect();
//            PreGameMenuController.addClient();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌ Connection failed");
        }
    }

    private volatile io.github.some_example_name.shared.model.GameState pendingState = null;
    private static GameView gameView = null; // وقتی GameView ساخته شد اینو ست کن

    // متد:
    private void onGameStateReceived(GameState state) {
        System.out.println("✅ GameState received (raw) -> " + (state == null ? "null" : "players=" + state.getPlayerPositions().size()));

        // اگر UI می‌خوای آپدیت کنی، باید داخل نخ گرافیکی باشه:
        Gdx.app.postRunnable(() -> {
            if (state == null) {
                // فقط لاگ باشه
                statusLabel.setText("Received empty game state");
                return;
            }

            // 1) اگر هنوز وارد GameView نشدیم، وضعیت رو ذخیره کن و لِیبل لابی رو آپدیت کن
            if (gameView == null) {
                pendingState = state;
                // آپدیت تعداد پلیرها در لابی (اختیاری)
                int n = state.getPlayerPositions().size();
                statusLabel.setText("⏳ Waiting players... current players: " + n);
                System.out.println("Lobby: saved pendingState, players=" + n);
                return;
            }

            // 2) اگر GameView فعال است، مستقیم اعمال کن
            try {
                // اگر GameView.applyGameState() خصوصی است، آن را به public تغییر بده
                gameView.applyGameState(state);
                System.out.println("GameView: applied state, players=" + state.getPlayerPositions().size());
            } catch (Exception ex) {
                System.err.println("Failed to apply state to GameView: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public static GameView getGameView() {
        return gameView;
    }
}
