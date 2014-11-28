import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends Application {
    private static final Random RGEN = new Random();
    private static final int NUM_ROUNDS = 4;
    private static final int NUM_CATEGORIES = 12;
    private BorderPane root;
    private MenuBar menuBar;
    private Menu viewMenu;
    private MenuItem fullscreenItem;
    private Stage stage;
    private CentralPane initialPane;
    private Label initialTitle;
    private Button initialButton;
    private ServerSocket serverSocket;
    private List<Player> players;
    private int roundNum;
    private List<String> categories;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        players = new ArrayList<>();

        fullscreenItem = new MenuItem("Fullscreen");
        fullscreenItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        fullscreenItem.setOnAction(this::fullscreenAction);

        viewMenu = new Menu("View");
        viewMenu.getItems().add(fullscreenItem);

        menuBar = new MenuBar();
        menuBar.getMenus().add(viewMenu);

        initialTitle = new Label("Scattergories");
        initialTitle.setStyle(
            "-fx-font-size: 100px;"+
            "-fx-font-family: \"Arial Black\";"+
            "-fx-text-fill: white;"+
            "-fx-effect: innershadow(three-pass-box, rgba(0,0,0,0.7), 6, 0.0, 0, 2);");

        initialButton = new DefaultButton("Start");
        initialButton.setDefaultButton(true);
        initialButton.setOnAction(this::startAction);

        initialPane = new CentralPane();
        initialPane.getChildren().addAll(initialTitle, initialButton);

        root = new BorderPane();
        root.setStyle("-fx-background-color: #1d1d1d;");
        root.setTop(menuBar);
        root.setCenter(initialPane);

        primaryStage.setTitle("Scattergories");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setFullScreenExitHint("Ctrl-F");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setOnCloseRequest(event -> {
            closeServerSocket();
            System.exit(0);
        });
        primaryStage.show();
    }

    private void startAction(ActionEvent actionEvent) {
        FlowPane startGamePane = new FlowPane(Orientation.VERTICAL, 20, 20);
        startGamePane.setAlignment(Pos.CENTER);
        startGamePane.setColumnHalignment(HPos.CENTER);
        Label statusLabel = new DefaultLabel("Establishing a server on port 4444");
        ProgressBar progressBar = new ProgressBar();
        Button readyButton = new DefaultButton("Begin game");
        readyButton.setDisable(true);
        readyButton.setDefaultButton(true);
        startGamePane.getChildren().addAll(statusLabel, progressBar, readyButton);
        root.setCenter(startGamePane);
        new Thread(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                serverSocket = new ServerSocket(4444);
                return null;
            }

            @Override
            protected void failed() {
                statusLabel.setText("Couldn't establish a server");
                statusLabel.setTextFill(Paint.valueOf("red"));
                startGamePane.getChildren().remove(progressBar);
                readyButton.setText("Return to main menu");
                readyButton.setOnAction(event -> root.setCenter(initialPane));
                readyButton.setDisable(false);
            }

            @Override
            protected void succeeded() {
                System.out.println(serverSocket);
                String baseWaitingText = "Waiting for players to connect...";
                Task<Void> acceptPlayers = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        while(!isCancelled()) {
                            Player player = new Player(serverSocket.accept());
                            players.add(player);
                            Platform.runLater(() -> statusLabel.setText(baseWaitingText + String.join(", ",
                                players.stream().map(Player::getName).collect(Collectors.toList()))));
                        }
                        return null;
                    }

                    @Override
                    protected void failed() {
                        getException().printStackTrace();
                    }
                };
                new Thread(acceptPlayers).start();
                statusLabel.setText(baseWaitingText);
                readyButton.setDisable(false);
                readyButton.setOnAction(event -> {
                    readyButton.setDisable(true);
                    acceptPlayers.cancel(true);
                    statusLabel.setText("Starting game...");
                    new Thread(new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            for(Iterator<Player> it = players.iterator(); it.hasNext();) {
                                if(!it.next().isAlive()) {
                                    it.remove();
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            statusLabel.setText(baseWaitingText + String.join(", ",
                                players.stream().map(Player::getName).collect(Collectors.toList())));
                            startRounds();
                        }

                        @Override
                        protected void failed() {
                            getException().printStackTrace();
                        }
                    }).start();
                });
            }
        }).start();
    }

    private void startRounds() {
        players.forEach(Player::resetScore);
        roundNum = 0;
        startRound();
    }

    private void startRound() {
        Button startCountdownButton = new DefaultButton("Start");
        root.setCenter(startCountdownButton);
        startCountdownButton.setDefaultButton(true);
        startCountdownButton.setDisable(true);
        startCountdownButton.setOnAction(event -> {
            char letter = getLetter();
            new Thread(new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    for(Iterator<Player> it = players.iterator(); it.hasNext();) {
                        if(!it.next().sendLetter(letter)) {
                            it.remove();
                        }
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    root.setCenter(new Countdown(20) {
                        @Override
                        protected void onComplete() {
                            roundComplete();
                        }
                    });
                }

                @Override
                protected void failed() {
                    getException().printStackTrace();
                }
            }).start();
        });
        new Thread(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                getCategories();
                for(Iterator<Player> it = players.iterator(); it.hasNext();) {
                    if(!it.next().sendCategories(categories)) {
                        it.remove();
                    }
                }
                return null;
            }

            @Override
            protected void succeeded() {
                startCountdownButton.setDisable(false);
            }

            @Override
            protected void failed() {
                getException().printStackTrace();
            }
        }).start();
    }

    private char getLetter() {
        return (char)('A' + RGEN.nextInt(26));
    }

    private void getCategories() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("categories.txt")));
        List<String> allCategories = new ArrayList<>();
        String line;
        while((line = br.readLine()) != null) {
            allCategories.add(line);
        }
        int numCategories = Math.min(allCategories.size(), NUM_CATEGORIES);
        categories = new ArrayList<>();
        for(int i = 0; i < numCategories; i++) {
            categories.add(allCategories.remove(RGEN.nextInt(allCategories.size())));
        }
    }

    private void roundComplete() {
        new Thread(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for(Iterator<Player> it = players.iterator(); it.hasNext();) {
                    if(!it.next().requestAnswers()) {
                        it.remove();
                    }
                }
                return null;
            }

            @Override
            protected void succeeded() {
                scoreAnswers();
            }

            @Override
            protected void failed() {
                getException().printStackTrace();
            }
        }).start();
    }

    private void scoreAnswers() {
        scoreAnswer(0);
    }

    private void scoreAnswer(int ans) {
        CentralPane pane = new CentralPane();
        String category = categories.get(ans);
        pane.getChildren().add(new DefaultLabel("#" + (ans + 1) + " - " + category));
        Map<CheckBox, Player> playerBoxes = new HashMap<>();
        for(Player player : players) {
            String answer = player.getAnswer(ans);
            CheckBox box = new CheckBox(player.getName() + " - " + (answer.isEmpty() ? "<no answer>" : answer));
            if(answer.isEmpty()) {
                box.setDisable(true);
            }
            playerBoxes.put(box, player);
            pane.getChildren().add(box);
        }
        Button scoreButton = new Button("Score");
        scoreButton.setDefaultButton(true);
        scoreButton.setOnAction(event -> {
            for(CheckBox box : playerBoxes.keySet()) {
                if(box.isSelected()) {
                    playerBoxes.get(box).incrementScore();
                }
            }
            if(ans + 1 < categories.size()) {
                scoreAnswer(ans + 1);
            } else if(roundNum >= NUM_ROUNDS - 1) {
                gameComplete();
            } else {
                roundNum++;
                startRound();
            }
        });
        pane.getChildren().add(scoreButton);
        root.setCenter(pane);
    }

    private void gameComplete() {
        CentralPane pane = new CentralPane();
        for(Player player : players) {
            pane.getChildren().add(new Label(player.getName() + ": " + player.getScore()));
        }
    }

    private void fullscreenAction(ActionEvent actionEvent) {
        stage.setFullScreen(!stage.isFullScreen());
        Platform.runLater(() -> root.setTop(stage.isFullScreen() ? null: menuBar));
    }

    private void closeServerSocket() {
        if(serverSocket != null) {
            try {
                serverSocket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
