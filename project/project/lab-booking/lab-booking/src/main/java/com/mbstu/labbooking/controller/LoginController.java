package com.mbstu.labbooking.controller;

import com.mbstu.labbooking.MainApp;
import com.mbstu.labbooking.dao.UserDAO;
import com.mbstu.labbooking.model.User;
import com.mbstu.labbooking.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            messageLabel.setText("Email and password required.");
            return;
        }
        try {
            User user = userDAO.login(email, pass);
            if (user == null) {
                messageLabel.setText("Invalid email or password.");
                return;
            }
            Session.setUser(user);
            if (user.isAdmin()) {
                MainApp.switchScene("admin_dashboard.fxml", "Admin Panel");
            } else {
                MainApp.switchScene("student_dashboard.fxml", "Student Panel");
            }
        } catch (Exception e) {
            messageLabel.setText("Database setup failed. See the error details.");
            javafx.scene.control.Alert dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            dialog.setHeaderText("Cannot prepare the lab database");
            Label details = new Label(e.getMessage());
            details.setWrapText(true);
            details.setMaxWidth(600);
            dialog.getDialogPane().setContent(details);
            dialog.getDialogPane().setPrefWidth(640);
            dialog.setResizable(true);
            dialog.showAndWait();
        }
    }

    @FXML
    private void goToRegister() {
        MainApp.switchScene("register.fxml", "Register");
    }
}
