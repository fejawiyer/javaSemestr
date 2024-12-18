package org.example.semestr;

public class Message {
    private String type;
    private String recipient;
    private String donor;
    private String text;
    private String time;

    public Message(String type, String recipient, String donor, String text, String time) {
        this.type = type;
        this.recipient = recipient;
        this.donor = donor;
        this.text = text;
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getDonor() {
        return donor;
    }

    public String getText() {
        return text;
    }
    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "[" + this.type + "] " + "[" + this.time + "] " + this.donor + ": " + this.text;
    }
}
