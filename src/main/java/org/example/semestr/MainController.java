package org.example.semestr;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class MainController {
    private static final ConfigReader reader = ConfigReader.getInstance();

    private PrintWriter writer;
    private BufferedReader serverReader;

    private String connectionType;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @FXML
    private TextField messageField, loginField;
    @FXML
    private Button buttonLogin, buttonSendMessage, buttonExit, buttonRegister;
    @FXML
    private Label loginLabel, registerLabel, currentOnline, usersList;
    @FXML
    private TextArea chat;
    @FXML
    private PasswordField passwordField;

    private void go() {
        try {
            Socket socket = new Socket(reader.getHost(), reader.getPort());
            writer = new PrintWriter(socket.getOutputStream(), true);
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String login = loginField.getText();
            String password = passwordField.getText();
            System.out.println(login);
            System.out.println(password);

            if (connectionType.equals("login")) {
                try {
                    writer.println("login");
                } catch (Exception e) {
                    showError("Ошибка при подключении " + e.getMessage());
                }
            }
            else {
                try {
                    writer.println("register");
                } catch (Exception e) {
                    showError("Ошибка при подключении " + e.getMessage());
                }
            }
            writer.println(login);
            writer.println(password);

            String loginMsg = serverReader.readLine();

            if (Objects.equals(loginMsg, "false")) {
                socket.close();
                return;
            }

            showInfo("Успешно подключение к серверу");

            buttonLogin.setVisible(false);
            loginLabel.setVisible(false);
            registerLabel.setVisible(false);
            passwordField.setVisible(false);
            loginField.setVisible(false);
            buttonRegister.setVisible(false);

            buttonSendMessage.setVisible(true);
            messageField.setVisible(true);
            chat.setVisible(true);
            currentOnline.setVisible(true);
            buttonExit.setVisible(true);
            usersList.setVisible(true);

            new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = serverReader.readLine()) != null) {
                        if (serverMessage.startsWith("/Users")) {
                            updateOnlineUsers(serverMessage);
                        }
                        else {
                            addMessageToChat(serverMessage);
                        }
                    }
                } catch (IOException ex) {
                    showError("Сервер закрыл соединение. " + ex.getMessage());
                }
            }).start();

        } catch (UnknownHostException ex) {
            showError("Сервер не найден. " + ex.getMessage());
        } catch (IOException ex) {
            showError("I/O ошибка. " + ex.getMessage());
        }
    }
    @FXML
    private void login() {
        connectionType = "login";
        go();
    }
    private void addMessageToChat(String message) {
        javafx.application.Platform.runLater(() -> {
            chat.appendText(message + "\n");
        });
    }
    @FXML
    protected void sendMessage() {
        String message = messageField.getText();
        if (message != null && !message.isEmpty()) {
            try {
                writer.println(message);
                messageField.clear();
            } catch (Exception e) {
                System.out.println("Error sending message: " + e.getMessage());
            }
        }
    }
    private void updateOnlineUsers(String countMessage) {
        javafx.application.Platform.runLater(() -> {
            String[] users = countMessage.split(": ");
            if (users.length > 1) {
                currentOnline.setText("Пользователей в сети: " + users[1]);
            }
        });
    }
    @FXML
    private void exit() {
        writer.println("aA11231231231Aa554432657dfght675esfd");
        Platform.exit();
    }
    @FXML
    private void register() {
        connectionType = "register";
        go();
    }
    @FXML
    private void initialize() {
        messageField.setOnAction(event -> sendMessage());
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Произошла ошибка");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Предупреждение");
        alert.setHeaderText("Предупреждение");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText("Информационное сообщение");
        alert.setContentText(message);
        alert.showAndWait();
    }
}