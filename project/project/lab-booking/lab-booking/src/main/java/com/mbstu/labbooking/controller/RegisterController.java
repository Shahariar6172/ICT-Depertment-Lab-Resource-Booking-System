package com.mbstu.labbooking.controller;

import com.mbstu.labbooking.MainApp;
import com.mbstu.labbooking.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField studentIdField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label messageLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String sid  = studentIdField.getText().trim();
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();

        if (name.isEmpty() || sid.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }
        if (!pass.equals(confirmField.getText())) {
            messageLabel.setText("Passwords do not match.");
            return;
        }
        try {
            userDAO.register(name, sid, email, pass);
            MainApp.switchScene("login.fxml", "Login");
        } catch (Exception e) {
            messageLabel.setText("Failed: email or student ID already used?");
        }
    }

    @FXML
    private void goToLogin() {
        MainApp.switchScene("login.fxml", "Login");
    }
}
