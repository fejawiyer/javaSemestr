package org.example.semestr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final ConfigReader configReader = ConfigReader.getInstance();

    static Set<ClientHandler> clientHandlers = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private static final ArrayList<String> users = new ArrayList<>();

    public static final String commandList = "/help, /list";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(configReader.getPort())) {
            logger.info("Waiting for clients...");
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException ex) {
            logger.error("Server error {}", ex.getMessage());
        }
    }

    static ArrayList<String> getUsers() {
        return users;
    }

    static void addUser(String username) {
        users.add(username);
        updateUserCount();
    }

    static void broadcast(String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.sendMessage(message);
        }
    }

    static void sendMessageToUser(String message, String username) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getUsername().equals(username)) {
                clientHandler.sendMessage(message);
                break;
            }
        }
    }
    public static void updateUserCount() {
        String userCountMessage = "/Users online: " + users.size();
        broadcast(userCountMessage);
    }
    static void removeUser(String username) {
        users.remove(username);
        updateUserCount();
    }
}

class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter writer;
    private String username;

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            this.writer = writer;
            this.username = reader.readLine();
            logger.info("Client connected with username {}", username);
            ChatServer.addUser(this.username);
            ChatServer.updateUserCount();
            String text;
            do {
                text = reader.readLine();
                if (text.startsWith("/")) {
                    String command = text.substring(1);
                    logger.info("User {} entered /{} command", username, command);
                    if (command.equals("help")) {
                        ChatServer.sendMessageToUser(ChatServer.commandList, username);
                    }
                    else if (command.equals("list")) {
                        ChatServer.sendMessageToUser(ChatServer.getUsers().toString(), username);
                    }
                    else {
                        logger.warn("Command {} not found", command);
                        ChatServer.sendMessageToUser("Command not found", username);
                    }
                }

                else if (text.startsWith("@")) {
                    int spaceIndex = text.indexOf(' ');
                    String recipient;
                    String message;
                    if (spaceIndex == -1) {
                        spaceIndex = text.length() - 1;
                        recipient = text.substring(1, spaceIndex + 1);
                        message = "null";
                    }
                    else {
                        recipient = text.substring(1, spaceIndex);
                        message = text.substring(spaceIndex + 1);
                    }
                    if (message.equals("null")) {
                        logger.warn("Null message received");
                        message = String.valueOf(' ');
                    }
                    logger.info("Message from {} to {}", username, recipient);

                    if (!ChatServer.getUsers().contains(recipient)) {
                        logger.warn("Recipient {} is not exists", recipient);
                        ChatServer.sendMessageToUser("User " + recipient + " is not exists", username);
                    }
                    else {
                        ChatServer.sendMessageToUser(message, recipient);
                    }

                } else {
                    logger.info("Broadcast message {} from {}", text, username);

                    if (text.equals("null"))
                        logger.warn("Null message received");

                    ChatServer.broadcast(username + ": " + text);
                }
            } while (!text.equalsIgnoreCase("exit"));

            socket.close();
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        } finally {
            ChatServer.clientHandlers.remove(this);
            logger.info("Client {} disconnected", username);
        }
    }

    void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    String getUsername() {
        return username;
    }
}