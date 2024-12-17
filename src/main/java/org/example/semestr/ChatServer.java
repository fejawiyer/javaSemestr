package org.example.semestr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ChatServer {
    private static final ConfigReader configReader = ConfigReader.getInstance();

    static Set<ClientHandler> clientHandlers = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private static final ArrayList<String> users = new ArrayList<>();

    public static final String commandList = "/help, /list";

    private static Connection connection;

    private static Statement stat;

    public static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static void main(String[] args) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:users.db");
        stat = connection.createStatement();
        stat.execute("CREATE TABLE if not exists 'users' ('username' INTEGER, 'password' text);");

        try (ServerSocket serverSocket = new ServerSocket(configReader.getPort())) {
            logger.info("Waiting for clients...");
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket, connection);
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

    static void addUserToDB(String username, String password) {
        try {
            password = encoder.encode(password);
            logger.info("INSERT INTO 'users' ('username', 'password') VALUES('{}', '{}');", username, password);
            stat.execute("INSERT INTO 'users' ('username', 'password') VALUES('" + username + "', '" + password + "');");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        users.add(username);
    }
    static void addUser(String username) {
        users.add(username);
        updateUserCount();
    }
    public static void select() throws SQLException {
        ResultSet sel = stat.executeQuery("SELECT * FROM users");
        while(sel.next()) {
            String name = sel.getString("username");
            String password = sel.getString("password");
            System.out.println("username = " + name);
            System.out.println("password = " + password);
        }
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
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);


    public ClientHandler(Socket socket, Connection connection) throws SQLException {
        this.socket = socket;
        this.connection = connection;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {
            String type = reader.readLine();
            String login = reader.readLine();
            String password = reader.readLine();

            if(type.equals("login")) {
                if (!isValidLogin(login, password)) {
                    writer.println("false");
                    socket.close();
                    return;
                } else {
                    writer.println("true");
                }

                this.writer = writer;
                this.username = login;
            }
            if(type.equals("register")) {
                writer.println("true");
                this.writer = writer;
                this.username = login;
                logger.info("Register username:{}, password:{}", this.username, password);
                ChatServer.addUserToDB(this.username, password);
            }
            logger.info("Client connected with username {}", username);
            ChatServer.addUser(this.username);
            ChatServer.updateUserCount();
            String text;

            do {
                text = reader.readLine();
                if (text.equalsIgnoreCase("aA11231231231Aa554432657dfght675esfd")) {
                    ChatServer.removeUser(username);
                }
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
                    if (!text.equals("aA11231231231Aa554432657dfght675esfd")) {
                        logger.info("Broadcast message {} from {}", text, username);

                        if (text.equals("null"))
                            logger.warn("Null message received");

                        ChatServer.broadcast(username + ": " + text);
                    }
                }
            } while (!text.equalsIgnoreCase("aA11231231231Aa554432657dfght675esfd"));

            socket.close();
        } catch (IOException | SQLException ex) {
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
    private boolean isValidLogin(String login, String password) throws SQLException {
        Statement stat = connection.createStatement();
        ResultSet res = stat.executeQuery("SELECT * FROM USERS WHERE USERNAME = '" + login + "'");
        logger.info("SELECT * FROM USERS WHERE USERNAME = '{}'", login);
        String passCheck = "";
        while(res.next()) {
            passCheck = res.getString("password");
        }
        return ChatServer.encoder.matches(password, passCheck);
    }
    String getUsername() {
        return username;
    }
}