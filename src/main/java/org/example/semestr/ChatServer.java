package org.example.semestr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.google.gson.Gson;

public class ChatServer {
    private static final ConfigReader configReader = ConfigReader.getInstance();

    static Set<ClientHandler> clientHandlers = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private static final ArrayList<String> users = new ArrayList<>();

    private static Statement stat;

    public static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static void main(String[] args) throws DatabaseConnectionException {
        Connection connection;
        try {
            logger.info("Trying to connect database...");
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
            logger.info("Success connect to database.");
            stat = connection.createStatement();
            stat.execute("CREATE TABLE if not exists 'users' ('username' text unique, 'password' text);");
            stat.execute("CREATE TABLE if not exists 'bc_message' ('time' text, 'username' text, 'message' text);");
            stat.execute("CREATE TABLE if not exists 'message' ('time' text, 'recipient' text, 'donor' text, 'message' text);");
        } catch (SQLException e) {
            logger.error("Failed connect to database. ");
            throw new DatabaseConnectionException("Failed connect to database. ");
        }
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

    static boolean addUserToDB(String username, String password) {
        try {
            ResultSet rs = stat.executeQuery("SELECT * FROM users WHERE username = '" + username + "'");
            if (rs.next()) {
                logger.warn("Username '{}' already exists. Registration denied.", username);
                throw new UserAlreadyExistsException("User with this username already exists");
            }
            password = encoder.encode(password);
            logger.info("INSERT INTO 'users' ('username', 'password') VALUES('{}', '{}');", username, password);
            stat.execute("INSERT INTO 'users' ('username', 'password') VALUES('" + username + "', '" + password + "');");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    static void addUser(String username) {
        users.add(username);
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

    static void broadcastByServer(String message) {
        Message msg = new Message(message);
        logger.info("Server broadcast: {}", msg.getText());
        logger.info("{}", msg);
        try {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.sendMessageFromServer(msg);
            }
        } catch (Exception e) {
            logger.error("Error while broadcast: {}", e.getMessage());
        }
    }
    static void broadcast(String username, String message) {
        Message msg = new Message(username, message);
        logger.info("broadcast: {}", msg.getText());
        logger.info("{}", msg);
        try {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.sendMessageFromServer(msg);
            }
        } catch (Exception e) {
            logger.error("Error while broadcast: {}", e.getMessage());
        }
    }

    static void sendMessageToUser(String message, String username, String recipient) {
        Message msg = new Message(username, message);
        try {for (ClientHandler clientHandler : clientHandlers) {
                if (clientHandler.getUsername().equals(recipient)) {
                    clientHandler.sendMessage(msg);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error sending message to {}: {}", username, e.getMessage());
        }
    }
    public static void updateUserCount() {
        StringBuilder userCountMessage = new StringBuilder("/Users online: " + users.size() + " ");
        for (String user : users) {
            userCountMessage.append(user).append(" ");
        }
        userCountMessage = new StringBuilder(userCountMessage.substring(0, userCountMessage.length() - 1));
        broadcastByServer(userCountMessage.toString());
    }

    static void removeUser(String username) {
        users.remove(username);
        updateUserCount();
    }

    static void insertBCMSG(String text, String username) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        try {
            logger.info("INSERT INTO 'bc_message' ('time', 'username', 'message') VALUES('{}', '{}', '{}');", time,  username, text);
            stat.execute("INSERT INTO 'bc_message' ('time', 'username', 'message') VALUES('" + time + "', '" + username + "', '" + text + "');");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    static void insertMSG(String text, String donor, String recipient) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        try {
            logger.info("INSERT INTO 'message' ('time', 'recipient', 'donor', 'message') VALUES('{}', '{}', '{}', '{}');", time,  recipient, donor, text);
            stat.execute("INSERT INTO 'message' ('time', 'recipient', 'donor', 'message') VALUES('" + time + "', '" + recipient + "', '" + donor + "', '"+ text + "');");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter writer;
    private String username;
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);


    public ClientHandler(Socket socket, Connection connection) {
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
                    logger.info("User {} try to login unsuccessfully", login);
                    writer.println("Incorrect login or password");
                    socket.close();
                    return;
                } else {
                    writer.println("true");
                }

                this.writer = writer;
                this.username = login;
            }
            if(type.equals("register")) {
                try {
                    this.writer = writer;
                    this.username = login;
                    if(ChatServer.addUserToDB(this.username, password)) {
                        writer.println("true");
                        logger.info("Register username:{}, password:{}", this.username, password);
                    }
                } catch (RuntimeException e) {
                    logger.error("Something got wrong. {}", e.getMessage());
                    if (e.getMessage().equals("User with this username already exists")) {
                        writer.println("User already exists");
                    }
                    else {
                        writer.println("false");
                    }
                    socket.close();
                    return;
                }
            }
            logger.info("Client connected with username {}", username);
            ChatServer.addUser(this.username);
            ChatServer.updateUserCount();
            Message msg;
            String text;
            Gson gson = new Gson();
            do {
                msg = gson.fromJson(reader.readLine(), Message.class);
                text = msg.getText();
                if (text.equalsIgnoreCase("e596899f114b5162402325dfb31fdaa792fabed718628336cc7a35a24f38eaa9")) {
                    ChatServer.removeUser(username);
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
                        ChatServer.sendMessageToUser("User " + recipient + " is not exists", "SERVER", username);
                    }
                    else {
                        message = "[private] " + username + ": " +message;
                        ChatServer.sendMessageToUser(message, username, recipient);
                        ChatServer.insertMSG(message, recipient, username);
                    }

                } else {
                    if (!text.equals("e596899f114b5162402325dfb31fdaa792fabed718628336cc7a35a24f38eaa9")) {
                        logger.info("Broadcast message {} from {}", text, username);

                        if (text.equals("null"))
                            logger.warn("Null message received");

                        ChatServer.insertBCMSG(text, username);
                        ChatServer.broadcast(username, text);
                    }
                }
            } while (!text.equalsIgnoreCase("e596899f114b5162402325dfb31fdaa792fabed718628336cc7a35a24f38eaa9"));

            socket.close();
        } catch (IOException | SQLException ex) {
            logger.error("Server exception: {}", ex.getMessage());
        } finally {
            ChatServer.clientHandlers.remove(this);
            logger.info("Client {} disconnected", username);
        }
    }
    void sendMessageFromServer(Message message) {
        if (writer != null) {
            try {
                Gson gson = new Gson();
                String jsonMSG = gson.toJson(message);
                logger.info("Send message from server:{}", message);
                writer.println(jsonMSG);
            } catch (Exception e) {
                logger.error("Error while sending server message: {}", e.getMessage());
            }
        }
    }
    void sendMessage(Message message) {
        if (writer != null) {
            try {
                Gson gson = new Gson();
                String jsonMSG = gson.toJson(message);
                writer.println(jsonMSG);
            } catch (Exception e) {
                logger.error("Error while sending message: {}", e.getMessage());
            }
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