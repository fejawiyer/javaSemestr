module org.example.semestr {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires spring.security.crypto;
    requires java.sql;
    requires com.google.gson;
    opens org.example.semestr to com.google.gson, javafx.fxml;
    exports org.example.semestr;
}