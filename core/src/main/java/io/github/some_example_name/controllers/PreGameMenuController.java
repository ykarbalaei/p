package io.github.some_example_name.controllers;

import io.github.some_example_name.Main;
import io.github.some_example_name.model.GameAssetManager;
import io.github.some_example_name.model.Pregame;
import io.github.some_example_name.model.game.Game;
import io.github.some_example_name.model.game.GameManager;
import io.github.some_example_name.model.game.WorldMap;
import io.github.some_example_name.network.ClientConnection;
import io.github.some_example_name.views.GameView;
import io.github.some_example_name.views.PreGameMenuView;

import java.util.ArrayList;
import java.util.List;

public class PreGameMenuController {
    private PreGameMenuView view;
    private Pregame pregame;


    public void setView(PreGameMenuView view) {
        this.view = view;
        this.pregame = new Pregame();
    }


 public void hadleTwoGames(ArrayList<String> usernames,String admin) {
     GameManager.createGame(usernames, admin);
     Game game = GameManager.getCurrentGame();
     WorldMap worldMap = new WorldMap();
     game.setWorldMap(worldMap);
     ClientConnection client =new ClientConnection("localhost", 8080);
     try {
         client.connect();
//            PreGameMenuController.addClient();
     } catch (Exception e) {
         e.printStackTrace();
     }
     if (view != null) {
         Main.getMain().getScreen().dispose();
         Main.getMain().setScreen(new GameView(new GameController(), GameAssetManager.getGameAssetManager().getSkin(),client,"player1"));
     }
 }



    public void handlePreGameMenuButtons() {
        List<String> usernames = new ArrayList<>();
        usernames.add("Player1");
        String creatorUsername = "Player1";

        GameManager.createGame(usernames, creatorUsername);
        Game game = GameManager.getCurrentGame();
        WorldMap worldMap = new WorldMap(); // اگر نیاز به پارامتر دارد مقداردهی کن

// ثبت WorldMap
        game.setWorldMap(worldMap);
        ClientConnection client =new ClientConnection("localhost", 8080);
        try {
            client.connect();
//            PreGameMenuController.addClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (view != null) {
            Main.getMain().getScreen().dispose();
            Main.getMain().setScreen(new GameView(new GameController(), GameAssetManager.getGameAssetManager().getSkin(),client,"player1"));
        }
    }

}
