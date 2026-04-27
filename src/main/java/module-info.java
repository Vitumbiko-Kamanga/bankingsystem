module org.example.bank {
    requires javafx.controls;
    requires javafx.fxml;

    opens bank to javafx.fxml;
    exports bank;
}