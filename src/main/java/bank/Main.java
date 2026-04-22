package bank;

import bank.model.*;
import bank.service.*;

import javafx.application.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

public class Main extends Application {

    private final BankService bankService = new BankService();
    private final UserService userService = new UserService();
    private final AdminService adminService = new AdminService();

    private Stage primaryStage;
    private User loggedInUser;

    // ─────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        userService.loadUsers();
        bankService.loadAccounts();

        showLoginScreen();

        stage.setTitle("🏦 Bank Simulator");
        stage.show();
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────
    private void showLoginScreen() {

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Text title = new Text("🏦 Bank System");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        TextField username = new TextField();
        username.setPromptText("Username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Label error = new Label();
        error.setTextFill(Color.RED);

        Button loginBtn = styledButton("Login", "#0f3460");

        loginBtn.setOnAction(e -> {

            String u = username.getText().trim();
            String p = password.getText().trim();

            // ADMIN LOGIN
            if (adminService.login(u, p)) {
                showAdminDashboard();
                return;
            }

            // USER LOGIN
            User user = userService.login(u, p);

            if (user == null) {
                error.setText("Invalid credentials");
                return;
            }

            loggedInUser = user;
            showUserDashboard();
        });

        root.getChildren().addAll(title, username, password, loginBtn, error);

        primaryStage.setScene(new Scene(root, 450, 400));
    }

    // ─────────────────────────────────────────────
    // USER DASHBOARD
    // ─────────────────────────────────────────────
    private void showUserDashboard() {

        Account account = bankService.getAccount(loggedInUser.getAccountId());

        if (account == null) {
            showAlert("Error", "Account not found!");
            showLoginScreen();
            return;
        }

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label welcome = new Label("Welcome " + loggedInUser.getUsername());
        welcome.setTextFill(Color.WHITE);

        Label balance = new Label();
        balance.setTextFill(Color.web("#4ecca3"));
        balance.setFont(Font.font(24));

        refreshBalance(balance, account);

        Button deposit = styledButton("Deposit", "#27ae60");
        Button withdraw = styledButton("Withdraw", "#e74c3c");
        Button transfer = styledButton("Transfer", "#2980b9");
        Button history = styledButton("History", "#8e44ad");
        Button logout = styledButton("Logout", "#e94560");

        deposit.setOnAction(e ->
                showAmountDialog("Deposit", amt ->
                        run(() -> bankService.deposit(account.getAccountId(), amt))
                )
        );

        withdraw.setOnAction(e ->
                showAmountDialog("Withdraw", amt ->
                        run(() -> bankService.withdraw(account.getAccountId(), amt))
                )
        );

        transfer.setOnAction(e -> showTransferDialog(account, balance));

        history.setOnAction(e -> showHistory(account));

        logout.setOnAction(e -> showLoginScreen());

        root.getChildren().addAll(welcome, balance, deposit, withdraw, transfer, history, logout);

        primaryStage.setScene(new Scene(root, 500, 500));
    }

    // ─────────────────────────────────────────────
    // ADMIN DASHBOARD
    // ─────────────────────────────────────────────
    private void showAdminDashboard() {

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Button users = styledButton("View Users", "#27ae60");
        Button tx = styledButton("View Transactions", "#2980b9");
        Button create = styledButton("Create User", "#f39c12");
        Button logout = styledButton("Logout", "#e94560");

        users.setOnAction(e -> showUsersTable());
        tx.setOnAction(e -> showTransactions());
        create.setOnAction(e -> showCreateUserDialog());
        logout.setOnAction(e -> showLoginScreen());

        root.getChildren().addAll(users, tx, create, logout);

        primaryStage.setScene(new Scene(root, 400, 300));
    }

    // ─────────────────────────────────────────────
    // CREATE USER
    // ─────────────────────────────────────────────
    private void showCreateUserDialog() {

        TextInputDialog u = new TextInputDialog();
        u.setHeaderText("Enter Username");

        u.showAndWait().ifPresent(username -> {

            TextInputDialog p = new TextInputDialog();
            p.setHeaderText("Enter Password");

            p.showAndWait().ifPresent(pass -> {

                String accId = "ACC" + System.currentTimeMillis();

                bankService.addAccount(
                        new SavingsAccount(accId, accId, username, 0)
                );

                userService.register(
                        new User(username, pass, accId)
                );

                showAlert("Success", "User created successfully");
            });
        });
    }

    // ─────────────────────────────────────────────
    // TRANSFER (FIXED)
    // ─────────────────────────────────────────────
    private void showTransferDialog(Account from, Label balance) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transfer");

        TextField acc = new TextField();
        acc.setPromptText("Recipient Account Number");

        TextField amt = new TextField();
        amt.setPromptText("Amount");

        VBox box = new VBox(10, acc, amt);
        dialog.getDialogPane().setContent(box);

        ButtonType ok = new ButtonType("Transfer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn);

        dialog.showAndWait().ifPresent(btn -> {

            try {
                double amount = Double.parseDouble(amt.getText().trim());

                run(() -> bankService.transferByAccountNumber(
                        from.getAccountId(),
                        acc.getText().trim(),
                        amount
                ));

                refreshBalance(balance, from);

            } catch (Exception ex) {
                showAlert("Error", "Invalid input");
            }
        });
    }

    // ─────────────────────────────────────────────
    // HISTORY
    // ─────────────────────────────────────────────
    private void showHistory(Account account) {

        ListView<String> list = new ListView<>();
        list.getItems().addAll(
                FileService.getUserTransactions(account.getAccountId())
        );

        Stage s = new Stage();
        s.setScene(new Scene(list, 500, 400));
        s.setTitle("Transaction History");
        s.show();
    }

    // ─────────────────────────────────────────────
    // USERS TABLE
    // ─────────────────────────────────────────────
    private void showUsersTable() {

        TableView<User> table = new TableView<>();

        TableColumn<User, String> u = new TableColumn<>("Username");
        u.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));

        TableColumn<User, String> a = new TableColumn<>("Account");
        a.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccountId()));

        table.getColumns().addAll(u, a);
        table.getItems().addAll(userService.getAllUsers());

        Stage s = new Stage();
        s.setScene(new Scene(table, 400, 300));
        s.show();
    }

    // ─────────────────────────────────────────────
    // TRANSACTIONS TABLE
    // ─────────────────────────────────────────────
    private void showTransactions() {

        ListView<String> list = new ListView<>();
        list.getItems().addAll(FileService.getAllTransactions());

        Stage s = new Stage();
        s.setScene(new Scene(list, 600, 400));
        s.show();
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private void refreshBalance(Label label, Account acc) {
        label.setText("MWK " + acc.getBalance());
    }

    private void showAmountDialog(String title, java.util.function.Consumer<Double> action) {

        TextInputDialog d = new TextInputDialog();
        d.setHeaderText(title);

        d.showAndWait().ifPresent(v -> {
            try {
                action.accept(Double.parseDouble(v));
            } catch (Exception e) {
                showAlert("Error", "Invalid number");
            }
        });
    }

    private void run(java.util.concurrent.Callable<java.util.concurrent.Future<String>> task) {

        try {
            var f = task.call();

            new Thread(() -> {
                try {
                    String res = f.get();
                    Platform.runLater(() -> showAlert("Result", res));
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Error", e.getMessage()));
                }
            }).start();

        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    private Button styledButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-padding: 10 20;
            -fx-background-radius: 6;
        """.formatted(color));
        return b;
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}