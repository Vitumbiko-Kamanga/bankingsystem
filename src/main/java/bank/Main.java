package bank;

import bank.model.*;
import bank.service.*;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class Main extends Application {

    private final BankService  bankService  = new BankService();
    private final UserService  userService  = new UserService();
    private final AdminService adminService = new AdminService();

    private Stage primaryStage;
    private User  loggedInUser;

    private static final String CSS_PATH = "/style.css";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        userService.loadUsers();
        bankService.loadAccounts();

        adminService.setUserService(userService);
        adminService.setBankService(bankService);

        showLoginScreen();

        stage.setTitle(" Bank Simulator");
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }

    // ── FIXED LOGIN SCREEN ────────────────────────────────────────────
    private void showLoginScreen() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60, 50, 60, 50));
        root.getStyleClass().add("root");

        VBox loginForm = new VBox(20);
        loginForm.getStyleClass().add("login-form");

        Text title = new Text(" Min Bank System");
        title.getStyleClass().add("title");

        TextField     username = new TextField();
        username.setPromptText("👤 Username");
        username.getStyleClass().add("input-field");

        PasswordField password = new PasswordField();
        password.setPromptText(" Password");
        password.getStyleClass().add("input-field");

        Label error = new Label();
        error.getStyleClass().add("error-label");

        Button loginBtn = styledButton(" Login", "btn-primary");

        // LOGIN HANDLER
        loginBtn.setOnAction(event -> {
            String user = username.getText().trim();
            String pass = password.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                error.setText("Please enter both username and password.");
                return;
            }

            // Check regular user login
            User foundUser = userService.authenticate(user, pass);
            if (foundUser != null) {
                loggedInUser = foundUser;
                error.setText("Welcome back, " + user + "!");
                error.getStyleClass().remove("error-label");
                error.getStyleClass().add("success-label");
                showUserDashboard();
                return;
            }

            // Check admin login
            if (adminService.login("admin", pass)) {
                error.setText("Admin login successful!");
                error.getStyleClass().remove("error-label");
                error.getStyleClass().add("success-label");
                showAdminDashboard();
                return;
            }

            // Failed login
            error.setText("Invalid username or password!");
            error.getStyleClass().remove("success-label");
            error.getStyleClass().add("error-label");
        });

        // Enter key support
        username.setOnAction(e -> loginBtn.fire());
        password.setOnAction(e -> loginBtn.fire());


        loginForm.getChildren().addAll(title, username, password, loginBtn, error);
        root.getChildren().add(loginForm);

        Scene scene = new Scene(root, 600, 500);
        loadStylesheet(scene);
        primaryStage.setScene(scene);
    }

    // ── User dashboard
    private void showUserDashboard() {
        Account account = bankService.getAccount(loggedInUser.getAccountId());

        if (account == null) {
            showAlert("Error", "Account not found!");
            showLoginScreen();
            return;
        }

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root");

        Label welcome = new Label("Welcome, " + loggedInUser.getUsername() + "!");
        welcome.getStyleClass().add("subtitle");

        Label balance = new Label();
        balance.getStyleClass().add("balance");
        refreshBalance(balance, account);

        Button deposit    = styledButton(" Deposit",         "btn-primary");
        Button withdraw   = styledButton(" Withdraw",        "btn-danger");
        Button transfer   = styledButton(" Transfer",        "btn-info");
        Button history    = styledButton(" History",         "btn-purple");
        Button accInfo    = styledButton(" Account Number",  "btn-warning");
        Button changePass = styledButton(" Change Password", "btn-warning");
        Button logout     = styledButton(" Logout",          "btn-logout");

        deposit.setOnAction(event ->
                showAmountDialog("Deposit", amt -> {
                    String result = bankService.deposit(account.getAccountId(), amt);
                    refreshBalance(balance, account);
                    showAlert("Deposit", result);
                })
        );
        withdraw.setOnAction(event ->
                showAmountDialog("Withdraw", amt -> {
                    String result = bankService.withdraw(account.getAccountId(), amt);
                    refreshBalance(balance, account);
                    showAlert("Withdraw", result);
                })
        );
        transfer.setOnAction(event   -> showTransferDialog(account, balance));
        history.setOnAction(event    -> showHistory(account));
        accInfo.setOnAction(event    -> showAccountInfo(account));
        changePass.setOnAction(event -> showChangePasswordDialog());
        logout.setOnAction(event -> {
            loggedInUser = null;
            showLoginScreen();
        });

        root.getChildren().addAll(welcome, balance, deposit, withdraw,
                transfer, history, accInfo, changePass, logout);
        Scene scene = new Scene(root, 650, 800);
        loadStylesheet(scene);
        primaryStage.setScene(scene);
    }

    // ── Admin dashboard
    private void showAdminDashboard() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("root");

        Text title = new Text(" Admin Panel");
        title.getStyleClass().add("title");

        Button users      = styledButton(" View Users",      "btn-primary");
        Button tx         = styledButton(" Transactions",    "btn-info");
        Button create     = styledButton(" Create User",      "btn-warning");
        Button delete     = styledButton("️ Delete User",     "btn-danger");
        Button changePass = styledButton(" Change Password", "btn-purple");
        Button logout     = styledButton(" Logout",          "btn-logout");

        users.setOnAction(event      -> showUsersTable());
        tx.setOnAction(event         -> showTransactions());
        create.setOnAction(event     -> showCreateUserDialog());
        delete.setOnAction(event     -> showDeleteUserDialog());
        changePass.setOnAction(event -> showAdminChangePasswordDialog());
        logout.setOnAction(event     -> showLoginScreen());

        root.getChildren().addAll(title, users, tx, create, delete, changePass, logout);
        Scene scene = new Scene(root, 550, 600);
        loadStylesheet(scene);
        primaryStage.setScene(scene);
    }

    // ── Account info
    private void showAccountInfo(Account account) {
        Dialog<ButtonType> dialog = new Dialog<>();
        loadDialogStylesheet(dialog);
        dialog.setTitle(" Account Information");
        dialog.setHeaderText("Your account details");

        Label ownerLabel = new Label(" Name:");
        ownerLabel.getStyleClass().add("subtitle");
        Label ownerValue = new Label(account.getOwner());

        Label accNoLabel = new Label(" Account Number:");
        accNoLabel.getStyleClass().add("subtitle");
        Label accNoValue = new Label(account.getAccountNumber());
        accNoValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label typeLabel = new Label(" Account Type:");
        typeLabel.getStyleClass().add("subtitle");
        Label typeValue = new Label(account.getAccountType());

        Button copyBtn = styledButton(" Copy Account Number", "btn-info");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(account.getAccountNumber());
            clipboard.setContent(content);
            copyBtn.setText(" Copied!");
        });

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);
        grid.setPadding(new Insets(24));
        grid.setAlignment(Pos.CENTER_LEFT);

        grid.add(ownerLabel, 0, 0);  grid.add(ownerValue, 1, 0);
        grid.add(accNoLabel, 0, 1);  grid.add(accNoValue, 1, 1);
        grid.add(typeLabel,  0, 2);  grid.add(typeValue,  1, 2);
        grid.add(copyBtn,    1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ── Transactions screen
    private void showTransactions() {
        TableView<Transaction> table = buildTransactionTable();

        List<Transaction> transactions = FileService.getAllTransactionsAsObjects();
        table.setItems(FXCollections.observableArrayList(transactions));

        if (transactions.isEmpty()) {
            table.setPlaceholder(new Label(" No transactions found"));
        }

        Label header = new Label(" All Bank Transactions (" + transactions.size() + ")");
        header.getStyleClass().add("subtitle");

        VBox vbox = new VBox(15, header, table);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        VBox.setVgrow(table, Priority.ALWAYS);

        Stage stage = createMaximizedStage(" Bank Transactions");
        Scene scene = new Scene(vbox);
        loadStylesheet(scene);
        stage.setScene(scene);
        stage.show();
    }

    // ── Users table
    private void showUsersTable() {
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> uCol = new TableColumn<>(" Username");
        uCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));

        TableColumn<User, String> aCol = new TableColumn<>(" Account ID");
        aCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAccountId()));

        TableColumn<User, String> nCol = new TableColumn<>(" Account Number");
        nCol.setCellValueFactory(d -> {
            Account acc = bankService.getAccount(d.getValue().getAccountId());
            return new SimpleStringProperty(acc != null ? acc.getAccountNumber() : "—");
        });

        table.getColumns().addAll(uCol, aCol, nCol);
        table.getItems().addAll(userService.getAllUsers());

        if (table.getItems().isEmpty()) {
            table.setPlaceholder(new Label(" No users registered yet."));
        }

        Stage stage = createSizedStage(" All Registered Users", 700, 500);
        Scene scene = new Scene(table);
        loadStylesheet(scene);
        stage.setScene(scene);
        stage.show();
    }

    // ── History
    private void showHistory(Account account) {
        ListView<String> list = new ListView<>();
        list.getStyleClass().add("list-view");
        list.getItems().addAll(FileService.getUserTransactions(account.getAccountId()));

        if (list.getItems().isEmpty()) {
            list.getItems().add(" No transactions found.");
        }

        Stage stage = createSizedStage(" Transaction History — " + account.getOwner(), 800, 600);
        Scene scene = new Scene(list);
        loadStylesheet(scene);
        stage.setScene(scene);
        stage.show();
    }

    // ── Table builder
    private TableView<Transaction> buildTransactionTable() {
        TableView<Transaction> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                buildAccountColumn(),
                buildTypeColumn(),
                buildAmountColumn(),
                buildDescriptionColumn(),
                buildDateColumn(),
                buildTimeColumn()
        );
        return table;
    }

    private TableColumn<Transaction, String> buildAccountColumn() {
        TableColumn<Transaction, String> col = new TableColumn<>("ACCOUNT");
        col.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAccountId()));
        col.setPrefWidth(170);
        return col;
    }

    private TableColumn<Transaction, Transaction.Type> buildTypeColumn() {
        TableColumn<Transaction, Transaction.Type> col = new TableColumn<>("TYPE");
        col.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getType()));
        col.setPrefWidth(120);

        col.setCellFactory(c -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("badge");
                setGraphic(badge);
            }

            @Override
            protected void updateItem(Transaction.Type type, boolean empty) {
                super.updateItem(type, empty);
                badge.getStyleClass().removeAll(
                        "badge-deposit", "badge-withdrawal", "badge-transfer");

                if (empty || type == null) {
                    badge.setText(null);
                } else {
                    badge.setText(type.name());
                    badge.getStyleClass().add(switch (type) {
                        case DEPOSIT    -> "badge-deposit";
                        case WITHDRAWAL -> "badge-withdrawal";
                        case TRANSFER   -> "badge-transfer";
                    });
                }
            }
        });
        return col;
    }

    private TableColumn<Transaction, Double> buildAmountColumn() {
        TableColumn<Transaction, Double> col = new TableColumn<>("AMOUNT");
        col.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getAmount()));
        col.setPrefWidth(140);

        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("amount-positive", "amount-negative");

                if (empty || value == null) {
                    setText(null);
                } else {
                    Transaction tx = getTableRow().getItem();
                    setText(String.format("MWK %,.2f", value));
                    if (tx != null) {
                        boolean isCredit = tx.getType() == Transaction.Type.DEPOSIT;
                        getStyleClass().add(isCredit ? "amount-positive" : "amount-negative");
                    }
                }
            }
        });
        return col;
    }

    private TableColumn<Transaction, String> buildDescriptionColumn() {
        TableColumn<Transaction, String> col = new TableColumn<>("DESCRIPTION");
        col.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
        col.setPrefWidth(200);
        return col;
    }

    private TableColumn<Transaction, String> buildDateColumn() {
        TableColumn<Transaction, String> col = new TableColumn<>("DATE");
        col.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFormattedDate()));
        col.setPrefWidth(110);
        return col;
    }

    private TableColumn<Transaction, String> buildTimeColumn() {
        TableColumn<Transaction, String> col = new TableColumn<>("TIME");
        col.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFormattedTime()));
        col.setPrefWidth(90);
        return col;
    }

    // ── Dialogs
    private void showChangePasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        loadDialogStylesheet(dialog);
        dialog.setTitle(" Change Password");
        dialog.setHeaderText(" Update your password");

        PasswordField oldPass     = new PasswordField();
        oldPass.setPromptText("Current Password");
        oldPass.getStyleClass().add("input-field");

        PasswordField newPass     = new PasswordField();
        newPass.setPromptText("New Password (min 6 chars)");
        newPass.getStyleClass().add("input-field");

        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirm New Password");
        confirmPass.getStyleClass().add("input-field");

        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox box = new VBox(15, oldPass, newPass, confirmPass, error);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        dialog.getDialogPane().setContent(box);

        ButtonType okBtn = new ButtonType(" Update Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(okBtn).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    String np = newPass.getText().trim();
                    String cp = confirmPass.getText().trim();
                    if (np.length() < 6) {
                        error.setText(" Password must be at least 6 characters.");
                        event.consume();
                        return;
                    }
                    if (!np.equals(cp)) {
                        error.setText(" Passwords do not match.");
                        event.consume();
                    }
                }
        );

        dialog.showAndWait().ifPresent(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String result = userService.changePassword(
                        loggedInUser.getUsername(),
                        oldPass.getText().trim(),
                        newPass.getText().trim()
                );
                showAlert("Change Password", result);
            }
        });
    }

    private void showAdminChangePasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        loadDialogStylesheet(dialog);
        dialog.setTitle(" Change Admin Password");
        dialog.setHeaderText("New password will be SAVED to admin.txt");

        PasswordField oldPass = new PasswordField();
        oldPass.setPromptText("Current Password");
        oldPass.getStyleClass().add("input-field");

        PasswordField newPass     = new PasswordField();
        newPass.setPromptText("New Password (6+ chars)");
        newPass.getStyleClass().add("input-field");

        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirm New Password");
        confirmPass.getStyleClass().add("input-field");

        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox box = new VBox(15, oldPass, newPass, confirmPass, error);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        dialog.getDialogPane().setContent(box);

        ButtonType okBtn = new ButtonType(" Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(okBtn).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    String np = newPass.getText().trim();
                    String cp = confirmPass.getText().trim();
                    if (np.length() < 6) {
                        error.setText(" Min 6 characters");
                        event.consume();
                    } else if (!np.equals(cp)) {
                        error.setText(" Passwords don't match");
                        event.consume();
                    }
                }
        );

        dialog.showAndWait().ifPresent(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String result = adminService.changeAdminPassword(
                        oldPass.getText().trim(), newPass.getText().trim());
                showAlert("Admin Password", result);
            }
        });
    }

    private void showCreateUserDialog() {
        TextInputDialog u = new TextInputDialog();
        loadDialogStylesheet(u);
        u.setHeaderText(" Enter Username");

        u.showAndWait().ifPresent(username -> {
            if (username.trim().isEmpty()) {
                showAlert("Error", "Username cannot be empty.");
                return;
            }
            TextInputDialog p = new TextInputDialog();
            loadDialogStylesheet(p);
            p.setHeaderText(" Enter Password");

            p.showAndWait().ifPresent(pass -> {
                if (pass.trim().length() < 6) {
                    showAlert("Error", "Password must be at least 6 characters.");
                    return;
                }
                String accId = "ACC" + System.currentTimeMillis();
                String accNo = generateAccountNumber();
                bankService.addAccount(new SavingsAccount(accId, accNo, username.trim(), 0));
                userService.register(new User(username.trim(), pass.trim(), accId));
                showAlert(" Success",
                        "User '" + username.trim() + "' created.\nAccount No: " + accNo);
            });
        });
    }

    private void showDeleteUserDialog() {
        TextInputDialog dialog = new TextInputDialog();
        loadDialogStylesheet(dialog);
        dialog.setTitle("🗑 Delete User");
        dialog.setHeaderText("Enter the username to delete");
        dialog.setContentText("Username:");

        dialog.showAndWait().ifPresent(username -> {
            if (username.trim().isEmpty()) {
                showAlert("Error", "Username cannot be empty.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            loadDialogStylesheet(confirm);
            confirm.setTitle(" Confirm Delete");
            confirm.setContentText(
                    "Are you sure you want to delete user '" + username + "'?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    String result = adminService.deleteUser(username.trim());
                    showAlert("Delete User", result);
                }
            });
        });
    }

    private void showTransferDialog(Account from, Label balance) {
        Dialog<ButtonType> dialog = new Dialog<>();
        loadDialogStylesheet(dialog);
        dialog.setTitle(" Transfer Funds");

        TextField acc = new TextField();
        acc.setPromptText("Recipient Account Number");
        acc.getStyleClass().add("input-field");

        TextField amt = new TextField();
        amt.setPromptText("Amount (MWK)");
        amt.getStyleClass().add("input-field");

        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox box = new VBox(15, acc, amt, error);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(box);

        ButtonType ok = new ButtonType(" Transfer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(ok).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (acc.getText().trim().isEmpty()) {
                        error.setText(" Recipient account cannot be empty.");
                        event.consume();
                        return;
                    }
                    try {
                        double val = Double.parseDouble(amt.getText().trim());
                        if (val <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException ex) {
                        error.setText(" Enter a valid positive amount.");
                        event.consume();
                    }
                }
        );

        dialog.showAndWait().ifPresent(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    double amount = Double.parseDouble(amt.getText().trim());
                    String result = bankService.transferByAccountNumber(
                            from.getAccountId(), acc.getText().trim(), amount);
                    refreshBalance(balance, from);
                    showAlert("Transfer", result);
                } catch (Exception ex) {
                    showAlert("Error", "Transfer failed: " + ex.getMessage());
                }
            }
        });
    }

    private void showAmountDialog(String title, Consumer<Double> action) {
        TextInputDialog d = new TextInputDialog();
        loadDialogStylesheet(d);
        d.setTitle(title);
        d.setHeaderText(" Enter amount for " + title);
        d.setContentText("Amount (MWK):");

        d.showAndWait().ifPresent(v -> {
            try {
                double amount = Double.parseDouble(v.trim());
                if (amount <= 0) throw new NumberFormatException();
                action.accept(amount);
            } catch (NumberFormatException e) {
                showAlert("Error", "Please enter a valid positive number.");
            }
        });
    }

    // ── Window Helper Methods
    private Stage createMaximizedStage(String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setMaximized(true);
        stage.setResizable(true);
        return stage;
    }

    private Stage createSizedStage(String title, double width, double height) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setResizable(true);
        stage.setMinWidth(400);
        stage.setMinHeight(300);
        if (width > 0 && height > 0) {
            stage.setWidth(width);
            stage.setHeight(height);
        }
        return stage;
    }

    // ── Helpers
    private String generateAccountNumber() {
        long base = System.currentTimeMillis() % 1_000_000_000L;
        int  lead = (int)(Math.random() * 9) + 1;
        return String.format("%d%09d", lead, base);
    }

    private void refreshBalance(Label label, Account acc) {
        double balance = bankService.calculateBalance(acc.getAccountId());
        label.setText(String.format(" Balance: MWK %.2f", balance));
    }

    private Button styledButton(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        return b;
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        loadDialogStylesheet(a);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }

    private void loadStylesheet(Scene scene) {
        try {
            var css = getClass().getResource(CSS_PATH);
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            else System.err.println("CSS not found: " + CSS_PATH);
        } catch (Exception e) {
            System.err.println("Error loading CSS: " + e.getMessage());
        }
    }

    private void loadDialogStylesheet(Dialog<?> dialog) {
        try {
            var css = getClass().getResource(CSS_PATH);
            if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        } catch (Exception e) {
            System.err.println("Error loading CSS for dialog: " + e.getMessage());
        }
    }

    private void loadDialogStylesheet(TextInputDialog dialog) {
        try {
            var css = getClass().getResource(CSS_PATH);
            if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        } catch (Exception e) {
            System.err.println("Error loading CSS for TextInputDialog: " + e.getMessage());
        }
    }

    private void loadDialogStylesheet(Alert alert) {
        try {
            var css = getClass().getResource(CSS_PATH);
            if (css != null) alert.getDialogPane().getStylesheets().add(css.toExternalForm());
        } catch (Exception e) {
            System.err.println("Error loading CSS for Alert: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        launch(args);
    }
}