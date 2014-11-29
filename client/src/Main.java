import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private List<String> categories;
    private char letter;
    private Label letterLabel;
    private List<TextField> answerFields;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        Label hostLabel = new Label("Host:");
        Label nameLabel = new Label("Name:");
        TextField hostField = new TextField();
        TextField nameField = new TextField();
        Button connectButton = new Button("Connect");
        connectButton.setDefaultButton(true);
        connectButton.setOnAction(event -> {
            String name = nameField.getText();
            String host = hostField.getText();
            if(host.isEmpty()) {
                showPopupMessage("No host entered", primaryStage);
                return;
            }
            if(name.isEmpty()) {
                showPopupMessage("No name entered", primaryStage);
                return;
            }
            try {
                socket = new Socket(host, 4444);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println(name);
                connectButton.setDisable(true);
                System.out.println("Connected");
                new Thread(new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        String line;
                        while((line = in.readLine()) != null) {
                            handleServerMessage(line);
                        }
                        return null;
                    }
                }).start();
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.BOTTOM_RIGHT);
        buttonBox.getChildren().add(connectButton);

        GridPane initialPane = new GridPane();
        initialPane.setAlignment(Pos.CENTER);
        initialPane.setHgap(10);
        initialPane.setVgap(10);
        initialPane.add(hostLabel, 0, 0);
        initialPane.add(hostField, 1, 0);
        initialPane.add(nameLabel, 0, 1);
        initialPane.add(nameField, 1, 1);
        initialPane.add(buttonBox, 0, 2, 2, 1);

        primaryStage.setTitle("Scattergories");
        primaryStage.setScene(new Scene(initialPane, 350, 250));
        primaryStage.setOnCloseRequest(event -> {
            closeSocket();
            System.exit(0);
        });
        primaryStage.show();
    }

    private void handleServerMessage(String line) {
        switch(line) {
            case "alive?":
                out.println("yes");
                break;
            case "categories":
                readCategories();
                break;
            case "letter":
                readLetter();
                break;
            case "answers":
                sendAnswers();
                break;
            default:
                System.out.println("Unknown message " + line);
        }
    }

    private void sendAnswers() {
        System.out.println("Sending answers");
        Platform.runLater(() -> answerFields.forEach(field -> field.setDisable(true)));
        System.out.println("Time's up");
        for(int i = 0; i < categories.size(); i++) {
            out.println(answerFields.get(i).getText().trim());
        }
        System.out.println("Sent");
    }

    private void readLetter() {
        try {
            letter = in.readLine().charAt(0);
            if("received?".equals(in.readLine())) {
                out.println("yes");
                Platform.runLater(this::showLetter);
            } else {
                System.out.println("Didn't get server request for read receipt");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void readCategories() {
        try {
            int numCategories = Integer.parseInt(in.readLine());
            categories = new ArrayList<>();
            for(int i = 0; i < numCategories; i++) {
                categories.add(in.readLine());
            }
            if("received?".equals(in.readLine())) {
                out.println("yes");
                Platform.runLater(this::showCategories);
            } else {
                System.out.println("Didn't get server request for read receipt");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void showLetter() {
        letterLabel.setText("Letter: " + letter);
        answerFields.forEach(field -> field.setDisable(false));
    }

    private void showCategories() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setAlignment(Pos.CENTER);
        letterLabel = new Label("Letter: ?");
        letterLabel.setAlignment(Pos.CENTER);
        letterLabel.setStyle("-fx-font-size: 32px;");
        pane.add(letterLabel, 0, 0, 2, 1);
        answerFields = new ArrayList<>();
        for(int i = 0; i < categories.size(); i++) {
            pane.add(new Label(categories.get(i)), 0, i + 1);
            TextField answerField = new TextField();
            answerField.setDisable(true);
            answerFields.add(answerField);
            pane.add(answerField, 1, i + 1);
        }
        stage.setScene(new Scene(pane, 600, 700));
    }

    private void closeSocket() {
        if(socket != null) {
            try {
                socket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Popup createPopup(final String message) {
        final Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        Label label = new Label(message);
        label.setOnMouseReleased(e -> popup.hide());
        label.setStyle(
            "-fx-background-color: cornsilk;" +
            "-fx-padding: 10;" +
            "-fx-border-color: black;" +
            "-fx-border-width: 5;" +
            "-fx-font-size: 16;");
        popup.getContent().add(label);
        return popup;
    }

    public static void showPopupMessage(final String message, final Stage stage) {
        final Popup popup = createPopup(message);
        popup.setOnShown(e -> {
            popup.setX(stage.getX() + stage.getWidth()/2 - popup.getWidth()/2);
            popup.setY(stage.getY() + stage.getHeight()/2 - popup.getHeight()/2);
        });
        popup.show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
