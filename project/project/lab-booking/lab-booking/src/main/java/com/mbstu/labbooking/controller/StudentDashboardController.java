package com.mbstu.labbooking.controller;

import com.mbstu.labbooking.MainApp;
import com.mbstu.labbooking.dao.BookingDAO;
import com.mbstu.labbooking.dao.DeviceDAO;
import com.mbstu.labbooking.dao.PenaltyDAO;
import com.mbstu.labbooking.model.*;
import com.mbstu.labbooking.util.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboardController {
    @FXML private ComboBox<String> subjectBox, materialBox;
    @FXML private CheckBox allStockCheck;
    @FXML private Label catalogStatus;
    @FXML private ComboBox<MaterialOption> variantBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Button addButton, submitButton;
    @FXML private Label stockLabel, cartSummary, welcomeLabel;
    @FXML private DatePicker duePicker;
    @FXML private TextField purposeField;
    @FXML private TableView<RequestItem> cartTable;
    @FXML private TableColumn<RequestItem, String> colCartMaterial, colCartVariant, colCartCategory;
    @FXML private TableColumn<RequestItem, Number> colCartQuantity;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> colBkBatch, colBkDevice, colBkRequest, colBkDue, colBkStatus;
    @FXML private TableView<Penalty> penaltyTable;
    @FXML private TableColumn<Penalty, String> colPnDevice, colPnType, colPnPaid;
    @FXML private TableColumn<Penalty, Number> colPnAmount;

    private static final String CUSTOM = "Other / custom work (all materials)";
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private final DeviceDAO deviceDAO = new DeviceDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PenaltyDAO penaltyDAO = new PenaltyDAO();
    private final ObservableList<RequestItem> cart = FXCollections.observableArrayList();
    private List<MaterialOption> options = new ArrayList<>();
    private boolean initialized;

    @FXML public void initialize() {
        welcomeLabel.setText("Hi, " + Session.getUser().getName());
        cartTable.setItems(cart);
        colCartMaterial.setCellValueFactory(new PropertyValueFactory<>("materialName"));
        colCartVariant.setCellValueFactory(new PropertyValueFactory<>("variantName"));
        colCartCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCartQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colBkBatch.setCellValueFactory(new PropertyValueFactory<>("requestCode"));
        colBkDevice.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        colBkRequest.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getRequestDate().format(DT)));
        colBkDue.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDueDate().toString()));
        colBkStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPnDevice.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        colPnType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPnAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colPnPaid.setCellValueFactory(new PropertyValueFactory<>("paidText"));
        subjectBox.valueProperty().addListener((o, oldValue, value) -> {
            if (!initialized) return;
            // A request belongs to one subject. Explicitly reset the draft when switching.
            cart.clear();
            updateSummary();
            loadDevices();
        });
        materialBox.valueProperty().addListener((o, oldValue, value) -> updateVariants());
        variantBox.valueProperty().addListener((o, oldValue, value) -> updateQuantity());
        updateSummary();
        updateQuantity();
        try {
            subjectBox.getItems().setAll(deviceDAO.findSubjects());
            subjectBox.getItems().add(CUSTOM);
            initialized = true;
            subjectBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Cannot load subjects. Database details: " + e.getMessage());
        }
    }

    @FXML public void loadDevices() {
        String previousMaterial = materialBox.getValue();
        options = new ArrayList<>();
        materialBox.getItems().clear();
        variantBox.getItems().clear();
        updateQuantity();
        if (subjectBox.getValue() == null) {
            catalogStatus.setText("Select a subject or work to browse materials.");
            return;
        }
        try {
            String subject = CUSTOM.equals(subjectBox.getValue()) || allStockCheck.isSelected()
                    ? null : subjectBox.getValue();
            options = deviceDAO.findMaterialOptions(subject);
            materialBox.getItems().setAll(options.stream().map(MaterialOption::getMaterialName).distinct().toList());
            if (materialBox.getItems().contains(previousMaterial)) materialBox.setValue(previousMaterial);
            else {
                String firstAvailable = options.stream().filter(o -> o.getAvailable() > 0)
                        .map(MaterialOption::getMaterialName).findFirst().orElse(null);
                if (firstAvailable != null) materialBox.setValue(firstAvailable);
                else materialBox.getSelectionModel().selectFirst();
            }
            int available = options.stream().mapToInt(MaterialOption::getAvailable).sum();
            catalogStatus.setText(options.isEmpty()
                    ? "No catalog materials linked to this subject. Select Show all lab stock."
                    : available == 0
                        ? "No units available in this view. Try Show all lab stock, or ask the admin to add/return stock."
                        : materialBox.getItems().size() + " materials | " + available + " available units");
        } catch (Exception e) {
            catalogStatus.setText("Could not load stock. Check the error details, then click Refresh Stock.");
            alert(Alert.AlertType.ERROR, "Cannot load materials: " + e.getMessage());
        }
    }

    private void updateVariants() {
        variantBox.getItems().setAll(options.stream()
                .filter(v -> v.getMaterialName().equals(materialBox.getValue())).toList());
        variantBox.getSelectionModel().clearSelection();
        MaterialOption available = variantBox.getItems().stream().filter(v -> v.getAvailable() > alreadyAdded(v)).findFirst().orElse(null);
        if (available != null) variantBox.setValue(available);
        else if (!variantBox.getItems().isEmpty()) variantBox.getSelectionModel().selectFirst();
        updateQuantity();
    }

    private int alreadyAdded(MaterialOption option) {
        return cart.stream().filter(i -> i.getOption().sameStock(option))
                .mapToInt(RequestItem::getQuantity).sum();
    }

    private void updateQuantity() {
        MaterialOption option = variantBox.getValue();
        int remaining = option == null ? 0 : Math.max(0, option.getAvailable() - alreadyAdded(option));
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, remaining), 1));
        quantitySpinner.setDisable(remaining == 0);
        addButton.setDisable(remaining == 0);
        stockLabel.setText(option == null ? "Choose a material and variant." :
                option.getAvailable() + " available | " + alreadyAdded(option) + " in your list | " + remaining + " left to add");
    }

    @FXML private void handleAddItem() {
        MaterialOption option = variantBox.getValue();
        if (option == null) return;
        int quantity = quantitySpinner.getValue();
        int existing = alreadyAdded(option);
        if (quantity < 1 || quantity > option.getAvailable() - existing) {
            alert(Alert.AlertType.WARNING, "Quantity exceeds available stock. Refresh and try again.");
            return;
        }
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).getOption().sameStock(option)) {
                cart.set(i, new RequestItem(option, existing + quantity));
                updateSummary();
                updateQuantity();
                return;
            }
        }
        cart.add(new RequestItem(option, quantity));
        updateSummary();
        updateQuantity();
    }

    @FXML private void handleRemoveItem() {
        RequestItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item != null) cart.remove(item);
        updateSummary();
        updateQuantity();
    }

    @FXML private void handleClearCart() {
        cart.clear();
        updateSummary();
        updateQuantity();
    }

    private void updateSummary() {
        cartSummary.setText(cart.size() + " variants | " + cart.stream().mapToInt(RequestItem::getQuantity).sum() + " total units");
        submitButton.setDisable(cart.isEmpty());
    }

    @FXML private void handleBook() {
        String subject = subjectBox.getValue();
        String purpose = purposeField.getText().trim();
        LocalDate due = duePicker.getValue();
        if (subject == null || cart.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Select a subject/work and add materials to your list.");
            return;
        }
        if (purpose.isEmpty() || purpose.length() > 255) {
            alert(Alert.AlertType.WARNING, "Enter your work/purpose (1 to 255 characters).");
            return;
        }
        if (due == null || !due.isAfter(LocalDate.now())) {
            alert(Alert.AlertType.WARNING, "Pick a return date after today.");
            return;
        }
        submitButton.setDisable(true);
        try {
            int units = cart.stream().mapToInt(RequestItem::getQuantity).sum();
            String code = bookingDAO.createBatchRequest(Session.getUser().getId(),
                    CUSTOM.equals(subject) ? "Custom work" : subject, purpose, due, new ArrayList<>(cart));
            cart.clear();
            purposeField.clear();
            duePicker.setValue(null);
            updateSummary();
            loadDevices();
            loadMyBookings();
            alert(Alert.AlertType.INFORMATION, "Request " + code.substring(0, 8) + " sent with " + units +
                    " units. Wait for admin approval.");
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Request failed. Your list is kept. " + e.getMessage());
            loadDevices();
        } finally {
            updateSummary();
        }
    }

    @FXML
    public void loadMyBookings() {
        if (bookingTable == null) return;
        try {
            bookingTable.setItems(FXCollections.observableArrayList(
                    bookingDAO.findByStudent(Session.getUser().getId())));
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "DB error: " + e.getMessage());
        }
    }

    @FXML
    public void loadMyPenalties() {
        if (penaltyTable == null) return;
        try {
            penaltyTable.setItems(FXCollections.observableArrayList(
                    penaltyDAO.findByStudent(Session.getUser().getId())));
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "DB error: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        MainApp.switchScene("login.fxml", "Login");
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert dialog = new Alert(type);
        dialog.setHeaderText(type == Alert.AlertType.ERROR ? "Database operation failed" : null);
        Label message = new Label(msg);
        message.setWrapText(true);
        message.setMaxWidth(600);
        dialog.getDialogPane().setContent(message);
        dialog.getDialogPane().setPrefWidth(640);
        dialog.setResizable(true);
        dialog.showAndWait();
    }
}
