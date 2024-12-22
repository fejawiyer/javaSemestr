package org.example.semestr;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private String time;
    private String username;
    private String text;
    private String recipient;
    private boolean isServerMSG; // 0 - user message 1 - server message

    public Message(String username, String text) {
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.username = username;
        this.text = text;
        this.recipient = null;
        this.isServerMSG = false;
    }

    public Message(String username, String text, String recipient) {
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.username = username;
        this.text = text;
        this.recipient = recipient;
        this.isServerMSG = false;
    }
    public Message(String text) {
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.username = null;
        this.text = text;
        this.recipient = null;
        this.isServerMSG = true;
    }
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    @Override
    public String toString() {
        return "Message{" +
                "time='" + time + '\'' +
                ", username='" + username + '\'' +
                ", text='" + text + '\'' +
                ", recipient='" + recipient + '\'' +
                '}';
    }
}

