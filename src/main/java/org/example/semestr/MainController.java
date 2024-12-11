package org.example.semestr;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainController {
    private static final ConfigReader reader = ConfigReader.getInstance();
    private String username;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader serverReader;

    @FXML
    private TextField usernameField, messageField;
    @FXML
    private Button buttonLogin, buttonSendMessage;
    @FXML
    private Label helloLabel;
    @FXML
    private TextArea chat;

    @FXML
    protected void connect() {
        username = usernameField.getText();
        go();
    }

    private void go() {
        try {
            socket = new Socket(reader.getHost(), reader.getPort());
            writer = new PrintWriter(socket.getOutputStream(), true);
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to the chat server");
            System.out.println(username);
            writer.println(username);

            buttonLogin.setVisible(false);
            helloLabel.setVisible(false);
            usernameField.setVisible(false);
            buttonSendMessage.setVisible(true);
            messageField.setVisible(true);
            chat.setVisible(true);

            new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = serverReader.readLine()) != null) {
                        addMessageToChat(serverMessage);
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
}