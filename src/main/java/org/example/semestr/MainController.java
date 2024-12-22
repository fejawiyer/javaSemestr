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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import com.google.gson.Gson;

public class MainController {
    private static final ConfigReader reader = ConfigReader.getInstance();

    private PrintWriter writer;
    private BufferedReader serverReader;

    private String connectionType;

    private final ArrayList<String> users = new ArrayList<>();

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

            if (Objects.equals(loginMsg, "Incorrect login or password")) {
                showError("Неправильный логин и/или пароль");
                socket.close();
                return;
            }
            else if (Objects.equals(loginMsg, "User already exists")) {
                showError("Пользователь уже существует.");
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
                Gson gson = new Gson();
                try {
                    while ((serverMessage = serverReader.readLine()) != null) {
                        try {
                            Message message = gson.fromJson(serverMessage, Message.class);
                            if (message.getText().startsWith("/Users")) {
                                updateOnlineUsers(message.getText());
                            } else if (message.getText().startsWith("[private")) {
                                addPrivateMessageToChat(message);
                            }
                            else {
                                addMessageToChat(message);
                            }
                        } catch (Exception e) {
                            javafx.application.Platform.runLater(() -> showError("Ошибка. " + e.getMessage()));
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
    private void addMessageToChat(Message message) {
        javafx.application.Platform.runLater(() -> {
            String formattedMSG= "[" + message.getTime() + "] " + message.getUsername() + ": " + message.getText();
            chat.appendText(formattedMSG + "\n");
        });
    }
    private void addPrivateMessageToChat(Message message) {
        javafx.application.Platform.runLater(() -> {
            String formattedMSG= "[" + message.getTime() + "]" + message.getText();
            chat.appendText(formattedMSG + "\n");
        });
    }
    @FXML
    protected void sendMessage() {
        String message = messageField.getText();
        if (message != null && !message.isEmpty()) {
            try {
                Message msg = new Message(loginField.getText(), message, null);
                Gson gson = new Gson();
                String jsonMSG = gson.toJson(msg);

                writer.println(jsonMSG);
                messageField.clear();
            } catch (Exception e) {
                showError("Ошибка при отправке сообщения: " + e.getMessage());
            }
        }
    }
    private void updateOnlineUsers(String countMessage) {
        javafx.application.Platform.runLater(() -> {
            String[] parts = countMessage.split("online: ");
            if (parts.length > 1) {
                String[] usersInfo = parts[1].trim().split(" ");
                currentOnline.setText("Пользователей в сети: " + usersInfo[0]);

                if(!users.isEmpty()) {
                    users.clear();
                }

                users.addAll(Arrays.asList(usersInfo).subList(1, usersInfo.length));
                String usersText = String.join("\n", users);
                usersList.setText(usersText);
            }
        });
    }

    @FXML
    private void exit() {
        String message = "e596899f114b5162402325dfb31fdaa792fabed718628336cc7a35a24f38eaa9";
        Message msg = new Message(loginField.getText(), message, null);
        Gson gson = new Gson();
        String jsonMSG = gson.toJson(msg);
        writer.println(jsonMSG);
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText("Информационное сообщение");
        alert.setContentText(message);
        alert.setContentText(message);
        alert.showAndWait();
    }
}