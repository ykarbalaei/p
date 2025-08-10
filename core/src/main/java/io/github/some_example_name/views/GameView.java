package io.github.some_example_name.views;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.some_example_name.Main;
import io.github.some_example_name.controllers.GameController;
import io.github.some_example_name.model.Animal.AnimalKind;
import io.github.some_example_name.model.Plant.Enums.CropType;
import io.github.some_example_name.model.Player.Player;
import io.github.some_example_name.model.Tools.Tool;
import io.github.some_example_name.model.building.Barn;
import io.github.some_example_name.model.building.BarnType;
import io.github.some_example_name.model.building.Coop;
import io.github.some_example_name.model.building.CoopType;
import io.github.some_example_name.model.crafting.CraftingManager;
import io.github.some_example_name.model.enums.ToolActionState;
import io.github.some_example_name.model.game.Position;
import io.github.some_example_name.model.user.User;
import io.github.some_example_name.network.ClientConnection;
import io.github.some_example_name.shared.model.GameState;
import io.github.some_example_name.shared.model.actions.MoveAction;
import io.github.some_example_name.views.Graphic.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import com.badlogic.gdx.graphics.g2d.TextureAtlas; // اگر نیاز دارید


public class GameView implements Screen, InputProcessor {
    public enum Area { FARM, BARN ,Coop,Home}
    private static Area currentArea = Area.FARM;

    private ObjectMapper mapper;
    private PrintWriter out;

//    private Texture otherPlayerTexture;


    private static Stage stage;
    private GameController controller;
    private InventoryMenu inventoryMenu;
    private boolean isInventoryOpen = false;
    private Skin skin;
    private ToolInventoryView toolInventoryView;
    private boolean isToolInventoryOpen = false;
    private boolean isCookingMenuOpen = false;
    private CookingMenu cookingMenu;
    private TerminalWindow terminalWindow;
    private boolean isTerminalWindowOpen = false;
    private InventoryView inventoryView;
    private FridgeView fridgeView;
    private boolean isFridgeOpen = false;
    private CraftingView craftingView;
    private boolean isCraftingViewOpen = false;


    private ShapeRenderer shapeRenderer;
    private float currentEnergy = 75f;
    private float maxEnergy     = 100f;
    private float barWidth      = 20f;
    private float barHeight     = 100f;
    private float margin        = 10f;

    private final Vector2 tmpVect = new Vector2();
    ArrayList<AnimalView> animals = new ArrayList<>();
    private static final List<AnimalView> farmAnimals = new ArrayList<>();
    private static final List<AnimalView> barnAnimals = new ArrayList<>();
    ArrayList<BarnView> barns = new ArrayList<>();
    ArrayList<CoopView> coops = new ArrayList<>();
    ArrayList<PlantView> plants = new ArrayList<>();
    ArrayList<Vector2> tillPositions = new ArrayList<>();
    Texture tillSheet  = new Texture("plants/graphics/till.png");

    private Animation<TextureRegion> feedAnimation;
    private Stage stage_copy;
    // برای رشد گیاهان
    private TextureRegion[] growthStages;
    private TextureRegion matureTexture;

    private ShapeRenderer shapeRenderer2 = new ShapeRenderer();
    private static Animation<TextureRegion> tillAnim;

    private final Map<String, Vector2> allPlayerPositions = new HashMap<>();
    private final ClientConnection client;
    private final String localPlayerId;
    public GameView(GameController controller, Skin skin, ClientConnection client, String localPlayerId) {
        this.controller = controller;
        this.skin = skin;
        this.client = client;
        this.localPlayerId = localPlayerId;
        controller.setView(this);
//        localPlayerId="player1";
    }



    @Override
    public void show() {

        stage = new Stage(new ScreenViewport());
        stage_copy = stage;
        Gdx.input.setInputProcessor(this);
        Player player = controller.getPlayerController().getPlayer();
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage_copy);
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
        GameHUD hud = new GameHUD(controller);
        GameHUD.showMessage = hud::showMessage;
        stage.addActor(hud);
        inventoryMenu = new InventoryMenu(player, skin);
        inventoryMenu.setVisible(false);
        inventoryMenu.setPosition((Gdx.graphics.getWidth() - inventoryMenu.getWidth()) / 2f, (Gdx.graphics.getHeight() - inventoryMenu.getHeight()) / 2f);
        stage.addActor(inventoryMenu);
        toolInventoryView = new ToolInventoryView(player, skin);
        toolInventoryView.setVisible(false);
        toolInventoryView.updateToolTable();
        stage.addActor(toolInventoryView);
        cookingMenu = new CookingMenu(player, skin);
        cookingMenu.setVisible(false);
        stage.addActor(cookingMenu);
        inventoryView = new InventoryView(player, skin);
        inventoryView.setVisible(false);
        inventoryView.setPosition((Gdx.graphics.getWidth() - inventoryView.getWidth()) / 2f, (Gdx.graphics.getHeight() - inventoryView.getHeight()) / 2f - 350);
        stage.addActor(inventoryView);
        fridgeView = new FridgeView(player, skin);
        fridgeView.setVisible(false);
        fridgeView.setPosition((Gdx.graphics.getWidth() - fridgeView.getWidth()) / 2f, (Gdx.graphics.getHeight() - fridgeView.getHeight()) / 2f - 300);
        stage.addActor(fridgeView);
        inventoryView.addFridgeDropTarget(fridgeView);
        fridgeView.setOnBackToCookingMenu(() -> {
            fridgeView.setVisible(false);
            inventoryView.setVisible(false);
            cookingMenu.setVisible(true);
            isFridgeOpen = false;
            isCookingMenuOpen = true;
        });
        cookingMenu.setOnFridgeIconClicked(() -> {
            cookingMenu.setVisible(false);
            fridgeView.update();
            inventoryView.refresh();
            fridgeView.setVisible(true);
            inventoryView.setVisible(true);
            isFridgeOpen = true;
            isCookingMenuOpen = false;
        });
        terminalWindow = new
            TerminalWindow(skin, stage, controller);
        terminalWindow.setVisible(false);
        stage.addActor(terminalWindow);
        CraftingManager.initializeRecipes();
        craftingView = new CraftingView(player, skin);
        craftingView.setVisible(false);
        craftingView.setPosition((Gdx.graphics.getWidth() - craftingView.getWidth()) / 2f, (Gdx.graphics.getHeight() - craftingView.getHeight()) / 2f - 300);
        stage.addActor(craftingView);
        shapeRenderer = new ShapeRenderer();

        //C:\Users\Dotcom\Desktop\New folder (2)\advanced-programming-phase-1-group-48\assets\Content (unpacked)\Animals
        Texture feedSheet = new Texture(Gdx.files.internal("Content (unpacked)/Animals/feed_spritesheet.png"));
        TextureRegion[][] tmp = TextureRegion.split(feedSheet, 32, 32);

        // ۲) جمع‌آوری فریم‌ها در یک آرایهٔ TextureRegion
        TextureRegion[] frames = new TextureRegion[tmp[0].length];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = tmp[0][i];
        }

        // ۳) ساخت Animation با تاخیر ۰.۱ ثانیه بین فریم‌ها
        feedAnimation = new Animation<>(0.1f, frames);


        //تصویر نهایی گیاهط
        matureTexture = new TextureRegion(new Texture("plants/carrot_mature.png"));
//        otherPlayerTexture = new Texture(Gdx.files.internal("player_other.png"));


    }
private GameState gameState;
    public void applyGameState(GameState state) {
        System.out.println("ooooooooooooooooo");
        if(state==gameState){
            System.out.println("jjjjjjjjjjjjjjjjj : ");
        }
        Map<String, int[]> positions = state.getPlayerPositions();
        System.out.println("Received positions:");
        for (Map.Entry<String, int[]> e : positions.entrySet()) {
            System.out.println("Player " + e.getKey() + ": x=" + e.getValue()[0] + ", y=" + e.getValue()[1]);
        }
        System.out.println("applyGameState: got " + positions.size() + " player positions");

        allPlayerPositions.clear();
        for (Map.Entry<String, int[]> e : positions.entrySet()) {
            String pid = e.getKey();
            int[] p = e.getValue();
            System.out.println(pid+":"+p[0]+":"+p[1]+pid.equals(localPlayerId));
        }
        for (Map.Entry<String, int[]> e : positions.entrySet()) {
            String pid = e.getKey();
            int[] p = e.getValue();
            if (p == null || p.length < 2) continue;
            allPlayerPositions.put(pid, new Vector2(p[0], p[1]));

            if (!pid.equals(localPlayerId)) {
                // فقط بازیکن‌های دیگه رو آپدیت کن
                p=state.getPlayerPositions().get(pid);
//                state.getPlayerPositions().put(pid, p);
                allPlayerPositions.put(pid, new Vector2(p[0], p[1]));
            } else {
                // مختصات لوکال رو از خود بازی/ورودی بگیر
                Player player = controller.getPlayerController().getPlayer();
                allPlayerPositions.put(pid, new Vector2(player.getPosX(), player.getPosY()));
            }
        }
gameState = state;
        System.out.println("applyGameState: allPlayerPositions size = " + allPlayerPositions.size());
    }

    private Map<String, Player> otherPlayers = new HashMap<>();

    private void syncOtherPlayers(Map<String, Vector2> allPlayerPositions, String localPlayerId) {
        for (Map.Entry<String, Vector2> entry : allPlayerPositions.entrySet()) {
            String pid = entry.getKey();
            Vector2 pos = entry.getValue();

            // بازیکن خودت رو رد کن
            if (pid.equals(localPlayerId) || pos == null) continue;

            Player p = otherPlayers.get(pid);
            if (p == null) {
                // ساخت بازیکن جدید
                User user=new User();
                p = new Player(pid,new Position((int)pos.x,(int)pos.y),null,user);
                 // تکسچر پیش‌فرض
                otherPlayers.put(pid, p);
            }

            // آپدیت پوزیشن
            p.setPosX(pos.x);
            p.setPosY(pos.y);
        }
    }



    @Override
    public void render(float delta) {
        try {
            Main.getBatch().setProjectionMatrix(controller.getWorldController().getCamera().combined);
            syncOtherPlayers(allPlayerPositions, localPlayerId);
            List<AnimalView> toDraw = (currentArea == Area.FARM)
                ? farmAnimals
                : barnAnimals;

            // 1️⃣ پاک کردن صفحه
            ScreenUtils.clear(0, 0, 0, 1);

            // 2️⃣ رندر همه اجزای بازی با SpriteBatch
            Main.getBatch().begin();

            controller.updateGame(delta);
            Player localPlayer = controller.getPlayerController().getPlayer();
            for (Player p : otherPlayers.values()) {
                drawPlayer(p);
            }
            // رسم بازیکنان دیگر
//            for (Map.Entry<String, Vector2> entry : allPlayerPositions.entrySet()) {
//                String pid = entry.getKey();
//                Vector2 pos = entry.getValue();
//                if (pid.equals(localPlayerId) || pos == null) continue;
//                Main.getBatch().draw(otherPlayerTexture, pos.x, pos.y);
//            }

            // رسم ابزار بازیکن محلی
            Tool equippedTool = localPlayer.getEquippedTool();
            if (equippedTool != null && localPlayer.getEquippedToolSprite() != null) {
                Sprite toolSprite = localPlayer.getEquippedToolSprite();
                float mouseX = Gdx.input.getX();
                float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
                float angle = MathUtils.atan2(mouseY - localPlayer.getPosY(), mouseX - localPlayer.getPosX()) * MathUtils.radiansToDegrees;
                toolSprite.setRotation(angle);
                toolSprite.setPosition(localPlayer.getPosX() + 30, localPlayer.getPosY() + 30);
                if (localPlayer.getToolState() == ToolActionState.USING) {
                    toolSprite.setScale(1.2f);
                    toolSprite.setAlpha(0.8f);
                } else {
                    toolSprite.setScale(1f);
                    toolSprite.setAlpha(1f);
                }
                toolSprite.draw(Main.getBatch());
                toolSprite.setAlpha(1f);
            }

            localPlayer.updateTool(delta);
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                localPlayer.useTool();
            }

            // رسم حیوانات
            for (AnimalView a : toDraw) {
                a.update(delta);
                a.render(Main.getBatch());
            }

            // سایر اجزای بازی
            drawTill(Main.getBatch());
            drawPlants(Main.getBatch(), delta);
            Main.getBatch().draw(feedAnimation.getKeyFrame(0), 10, 10);
            drawBarns(Main.getBatch(), delta);
            drawCoops(Main.getBatch(), delta);

            Main.getBatch().end();

            // 3️⃣ رندر UI
            drawEnergy();
            stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
            stage.draw();

            // 4️⃣ رندر شکل‌ها
            shapeRenderer.setProjectionMatrix(controller.getWorldController().getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rect(100, 100, 32, 32);
            shapeRenderer.end();

        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
            e.printStackTrace(); // برای دیدن دقیق خطا
        }
    }


    private void drawPlayer(Player player) {
        if (player.getPlayerSprite() != null) {
            player.getPlayerSprite().setPosition(player.getPosX(), player.getPosY());
            player.getPlayerSprite().draw(Main.getBatch());
        }
    }

    private void drawEnergy() {
        Player player = controller.getPlayerController().getPlayer();
        currentEnergy = player.getEnergy();          // یا متدی که انرژی فعلی رو می‌ده
        maxEnergy     = player.getMaxEnergy();
        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        float filledHeight = (currentEnergy / maxEnergy) * barHeight;
        float x = Gdx.graphics.getWidth() - barWidth - margin;
        float y = margin;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(x, y, barWidth, filledHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, barWidth, barHeight);
        shapeRenderer.end();
    }

        private void drawAnimals(SpriteBatch batch,float delta) {
            if(animals.size() > 0) {
                for (AnimalView animal : animals) {
                    Player player = controller.getPlayerController().getPlayer();
//                    System.out.println("Camera: " + stage.getCamera().position);
//                    System.out.println("Player: " + player.getPosX() + "," + player.getPosY());
//                    System.out.println("Animal: " + animal.getX() + "," + animal.getY());
                    animal.render(batch);
                    animal.update(delta);
                }
            }
        }

        private void drawPlants(SpriteBatch batch,float delta) {
        if(plants.size() > 0) {
            for (PlantView plant : plants) {
                Player player = controller.getPlayerController().getPlayer();
                plant.update(delta);
                plant.render(batch);
            }
        }
        }

    private void drawCoops(SpriteBatch batch, float delta) {
        if (coops == null) return;

        // 1) این لیست را حتماً مقداردهی اولیه کن
        ArrayList<Vector2> positionOfBarn = new ArrayList<>();

        // 2) جمع‌آوری مختصات هر BarnView
        for (CoopView barn : coops) {
            positionOfBarn.add(barn.getPosition());
        }

        // 3) پیمایش ایمن با Iterator
        Iterator<Vector2> it = positionOfBarn.iterator();
        while (it.hasNext()) {
            Vector2 pos = it.next();
            boolean consumed = false;

            // اگر داریم داخل استیج داخلیِ طویله‌ایم
            if (stage != stage_copy) {
                it.remove();     // حذف pos از لیست
                consumed = true; // علامت می‌زنیم دیگر رسم نشود
            }

            // 4) اگر مصرف نشده بود، همه barns را رسم و به‌روز کنیم
            if (!consumed) {
                for (CoopView b : coops) {
                    b.render(batch);
                    b.update(delta);
                }
            }
        }
    }



    private void drawBarns(SpriteBatch batch, float delta) {
        if (barns == null) return;

        // 1) این لیست را حتماً مقداردهی اولیه کن
        ArrayList<Vector2> positionOfBarn = new ArrayList<>();

        // 2) جمع‌آوری مختصات هر BarnView
        for (BarnView barn : barns) {
            positionOfBarn.add(barn.getPosition());
        }

        // 3) پیمایش ایمن با Iterator
        Iterator<Vector2> it = positionOfBarn.iterator();
        while (it.hasNext()) {
            Vector2 pos = it.next();
            boolean consumed = false;

            // اگر داریم داخل استیج داخلیِ طویله‌ایم
            if (stage != stage_copy) {
                it.remove();     // حذف pos از لیست
                consumed = true; // علامت می‌زنیم دیگر رسم نشود
            }

            // 4) اگر مصرف نشده بود، همه barns را رسم و به‌روز کنیم
            if (!consumed) {
                for (BarnView b : barns) {
                    b.render(batch);
                    b.update(delta);
                }
            }
        }
    }


    public void drawTill(Batch batch) {
        if (tillPositions == null) return;

        Iterator<Vector2> it = tillPositions.iterator();
        while (it.hasNext()) {
            Vector2 pos = it.next();

            // آیا این pos داخل هیچ PlantView ای هست؟
            boolean consumed = false;
            for (PlantView plant : plants) {
                if (plant.getPosition().epsilonEquals(pos, 0.001f)) {
                    // اگر گیاه روی این نقطه هست، حذفش کن
                    it.remove();
                    consumed = true;
                    break;
                }
            }

            // اگر هنوز مصرف نشده، رسمش کن
            if (!consumed) {
                batch.draw(tillSheet, pos.x, pos.y);
            }
        }
    }


    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.T) {
            isToolInventoryOpen = !isToolInventoryOpen;
            toolInventoryView.updateToolTable();
            toolInventoryView.setVisible(isToolInventoryOpen);
        }
        if (keycode == Input.Keys.ESCAPE) {
            isInventoryOpen = !isInventoryOpen;
            inventoryMenu.setVisible(isInventoryOpen);
        }
        if (keycode == Input.Keys.C) {
            isCookingMenuOpen = !isCookingMenuOpen;
            cookingMenu.updateRecipes();
            cookingMenu.setVisible(isCookingMenuOpen);
        }
        if (keycode == Input.Keys.R) {
            isTerminalWindowOpen = !isTerminalWindowOpen;
            terminalWindow.toggle();
        }
        if (keycode == Input.Keys.B) {
            isCraftingViewOpen = !isCraftingViewOpen;
            craftingView.updateRecipes();
            craftingView.setVisible(isCraftingViewOpen);
        }
        if(keycode == Input.Keys.Z) {
            Player player = controller.getPlayerController().getPlayer();
            float spawnX = player.getPosX();
            float spawnY = player.getPosY();
            AnimalView animal = new AnimalView(spawnX, spawnY, new Animal(AnimalKind.SHEEP));
            animal.setFeedAnimation(feedAnimation);
            animals.add(animal);
            if (currentArea == Area.FARM) {
                farmAnimals.add(animal);
            } else {
                barnAnimals.add(animal);
            }

        }
        if(keycode == Input.Keys.O) {
            Player player = controller.getPlayerController().getPlayer();
            float spawnX = player.getPosX()+100;
            float spawnY = player.getPosY();
            barns.add(new BarnView(spawnX,spawnY, new Barn(BarnType.BARN)));
        }
        if(keycode == Input.Keys.I) {
            Player player = controller.getPlayerController().getPlayer();
            float spawnX = player.getPosX()+100;
            float spawnY = player.getPosY();
            coops.add(new CoopView(spawnX,spawnY, new Coop(CoopType.COOP)));
        }
        if (keycode == Input.Keys.L ) {
            //چوپانی
            Player player = controller.getPlayerController().getPlayer();
            Vector2 playerPos = new Vector2(player.getPosX(), player.getPosY());

            for (AnimalView av : animals) {
                if (av.getPosition().dst(playerPos) < 500) { // فاصله مجاز چوپانی
                    av.startShepherding(playerPos);
                }
            }
            return true;
        }
        if(keycode == Input.Keys.X) {
            Player player = controller.getPlayerController().getPlayer();
            tillPositions.add(new Vector2(player.getPosX()+100, player.getPosY()));
        }
        if(keycode == Input.Keys.V) {
            Player player = controller.getPlayerController().getPlayer();
            float x=player.getPosX()+100;
            float y=player.getPosY();
            for(Vector2 position:tillPositions) {
                if(position.x==x && position.y==y) {
                    plants.add(new PlantView(new Plant(CropType.AMARANTH),x,y));
                }
            }
        }
        if (keycode == Input.Keys.M) {
            Player player = controller.getPlayerController().getPlayer();
            float x=player.getPosX()+100;
            float y=player.getPosY();
            for (PlantView plantView : plants) {
                if(plantView.getPosition().dst(x,y)<500) {
                    plantView.doWater();
                }
            }
        }
        if(keycode == Input.Keys.N) {
            Player player = controller.getPlayerController().getPlayer();
            float x=player.getPosX()+100;
            float y=player.getPosY();
            for (PlantView plantView : plants) {
                if(plantView.getPosition().dst(x,y)<500) {
                    plantView.doFilter();
                }
            }
        }
        if(keycode == Input.Keys.Q) {
            System.out.println("kkkkkk");
            Player player = controller.getPlayerController().getPlayer();
            float x=player.getPosX()+100;
            float y=player.getPosY();
            for (PlantView plantView : plants) {
                System.out.println("oooo");
                if(plantView.getPosition().dst(x,y)<500) {
                    System.out.println("lllllll");
                    plantView.collectProduct();
                }
            }
        }
        if (keycode == Input.Keys.A) {
            Player player = controller.getPlayerController().getPlayer();
            // ۱) خود بازی جا‌به‌جا کن (اختیاری)
//            player.moveLeft();
            // ۲) یک MoveAction بساز و به سرور بفرست:

            MoveAction mv = new MoveAction(localPlayerId, -1, 0);
            try {
                client.sendAction(mv);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (keycode == Input.Keys.D) {
            Player player = controller.getPlayerController().getPlayer();
            MoveAction mv = new MoveAction(localPlayerId, 20, 0);
            try {
                client.sendAction(mv);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (keycode == Input.Keys.S) {
            Player player = controller.getPlayerController().getPlayer();
            MoveAction mv = new MoveAction(localPlayerId, 0, 20);
            try {
                client.sendAction(mv);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (keycode == Input.Keys.W) {
            Player player = controller.getPlayerController().getPlayer();
            MoveAction mv = new MoveAction(localPlayerId, 0, -20);
            try {
                client.sendAction(mv);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



        return false;
    }

    public static void releaseAllBarnAnimals() {
        // فقط اگر دارید در طویله هستید معنی دارد
        System.out.println("jjjjjj");
        if (currentArea == Area.BARN) {
            System.out.println("ppppp");
            // همهٔ barnAnimals را به farmAnimals منتقل کن
            farmAnimals.addAll(barnAnimals);
            barnAnimals.clear();

            // اگر می‌خواهی بلافاصله به فارم برگردی:
            currentArea = Area.FARM;
        }
    }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.RIGHT) return false;

        // 1) تبدیل مختصات کل
        // یک صفحه -> دنیای بازی
        Vector3 world = new Vector3(screenX, screenY, 0);
        controller.getWorldController().getCamera().unproject(world);
        float worldX = world.x, worldY = world.y;
        Gdx.app.log("CLICK", "world click at: " + worldX + ", " + worldY);

        // 2) جستجو در میان حیوانات
        for (int i = 0; i < animals.size(); i++) {
            AnimalView av = animals.get(i);
            // باکس برخورد در دنیای بازی
            Rectangle r = av.getBounds();
            Gdx.app.log("ANIMAL[" + i + "]", "bounds = " + r);

            if (r.contains(worldX, worldY)) {

                // 3) ساخت منوی کانتکست
                Skin menuSkin = new Skin(
                    Gdx.files.internal("skin/plain-james-ui.json"),
                    new TextureAtlas(Gdx.files.internal("skin/plain-james-ui.atlas"))
                );
                AnimalContextMenu menu = new AnimalContextMenu(av, menuSkin);
                menu.pack();

                // 4) موقعیت‌دهی منو زیر موس (مختصات UI == screen)
                //    چون stage ما ScreenViewport داره، screen coords == stage coords
                menu.setPosition(screenX, Gdx.graphics.getHeight() - screenY);

                // 5) اضافه کردن منو به stage
                stage.addActor(menu);
                return true;  // مصرف رویداد
            }
        }

        Gdx.app.log("CLICK", "no animal hit");
        return false;
    }





    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

    public ArrayList<BarnView> getBarns() {
        return barns;
    }

    public void setStage(Stage newStage) {
        this.stage = newStage;
        // دوباره InputProcessor رو ست می‌کنیم
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(stage);   // Stage جدید (UI + منوها)
        mux.addProcessor(this);    // خودِ GameView برای keyDown و touchDown
        Gdx.input.setInputProcessor(mux);
    }

    public static Stage getStage() {
        return stage;
    }

    public Stage getStage_copy() {
        return stage_copy;
    }

    public ArrayList<AnimalView> getAnimals() {
        return animals;
    }

    public void setCurrentArea(Area currentArea) {
        this.currentArea = currentArea;
    }

    public Area getCurrentArea() {
        return currentArea;
    }

    public ArrayList<CoopView> getCoops() {
        return coops;
    }






    public Map<String, Vector2> getAllPlayerPositions() {
        return allPlayerPositions;
    }

    public String getLocalPlayerId() {
        return localPlayerId;
    }
}
