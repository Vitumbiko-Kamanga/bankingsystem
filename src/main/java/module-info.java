module org.example.bank {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens bank to javafx.graphics, javafx.fxml;
    opens bank.model to javafx.base;
    opens bank.service to javafx.base;
    opens bank.exception to javafx.base;
}