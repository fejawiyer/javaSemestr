module org.example.semestr {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;


    opens org.example.semestr to javafx.fxml;
    exports org.example.semestr;
}