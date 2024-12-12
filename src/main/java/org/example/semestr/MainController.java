package org.example.semestr;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;

public class MainController {
    private static final ConfigReader reader = ConfigReader.getInstance();
    private String username;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader serverReader;

    private String login, password;

    @FXML
    private TextField messageField, loginField;
    @FXML
    private Button buttonLogin, buttonSendMessage;
    @FXML
    private Label loginLabel, registerLabel, currentOnline;
    @FXML
    private TextArea chat;
    @FXML
    private PasswordField passwordField;

    @FXML
    protected void connect() {
        login = loginField.getText();
        password = passwordField.getText();
        System.out.println(login);
        System.out.println(password);
        username = login;
        go();
    }
    @FXML
    protected void register() {
    }
    private void go() {
        try {
            socket = new Socket(reader.getHost(), reader.getPort());
            writer = new PrintWriter(socket.getOutputStream(), true);
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            login();

            System.out.println("Connected to the chat server");

            String loginMsg = serverReader.readLine();

            if (Objects.equals(loginMsg, "false")) {
                socket.close();
                return;
            }

            buttonLogin.setVisible(false);
            loginLabel.setVisible(false);
            registerLabel.setVisible(false);
            passwordField.setVisible(false);
            loginField.setVisible(false);

            buttonSendMessage.setVisible(true);
            messageField.setVisible(true);
            chat.setVisible(true);
            currentOnline.setVisible(true);

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
                    System.out.println("Server connection closed: " + ex.getMessage());
                }
            }).start();

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
    private void login() {
        try {
            writer.println(login);
            writer.println(password);
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
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
                currentOnline.setText("Пользователей: " + users[1]);
            }
        });
    }
}