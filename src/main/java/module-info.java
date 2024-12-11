module org.example.semestr {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.semestr to javafx.fxml;
    exports org.example.semestr;
}