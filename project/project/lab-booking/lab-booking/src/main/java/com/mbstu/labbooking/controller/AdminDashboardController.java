package com.mbstu.labbooking.controller;

import com.mbstu.labbooking.MainApp;
import com.mbstu.labbooking.dao.BookingDAO;
import com.mbstu.labbooking.dao.DeviceDAO;
import com.mbstu.labbooking.dao.PenaltyDAO;
import com.mbstu.labbooking.model.Booking;
import com.mbstu.labbooking.model.Device;
import com.mbstu.labbooking.model.DeviceGroup;
import com.mbstu.labbooking.model.Penalty;
import com.mbstu.labbooking.util.PenaltyCalculator;
import com.mbstu.labbooking.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;

    // Pending tab
    @FXML private TableView<Booking> pendingTable;
    @FXML private TableColumn<Booking, String> colPdStudent, colPdDevice, colPdRequest, colPdDue, colPdPurpose;

    // Issue/Return tab
    @FXML private TableView<Booking> approvedTable;
    @FXML private TableColumn<Booking, String> colApStudent, colApDevice, colApDue;
    @FXML private TableView<Booking> issuedTable;
    @FXML private TableColumn<Booking, String> colIsStudent, colIsDevice, colIsDue, colIsIssueDate;
    @FXML private CheckBox damagedCheck;
    @FXML private Label returnInfoLabel;

    // Stock tab (grouped)
    @FXML private TableView<DeviceGroup> stockTable;
    @FXML private TableColumn<DeviceGroup, String> colStName, colStCategory;
    @FXML private TableColumn<DeviceGroup, Number> colStTotal, colStAvailable, colStIssued, colStDamaged;
    @FXML private Label stockSummaryLabel;

    // Devices tab (unit level)
    @FXML private TableView<Device> deviceTable;
    @FXML private TableColumn<Device, String> colDvName, colDvCategory, colDvSerial, colDvStatus;
    @FXML private TextField devNameField, devSerialField;
    @FXML private ComboBox<String> devCategoryBox;

    // Penalties tab
    @FXML private TableView<Penalty> penaltyTable;
    @FXML private TableColumn<Penalty, String> colPnStudent, colPnDevice, colPnType, colPnPaid;
    @FXML private TableColumn<Penalty, Number> colPnAmount;
    @FXML private Label totalLabel;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final DeviceDAO deviceDAO   = new DeviceDAO();
    private final PenaltyDAO penaltyDAO = new PenaltyDAO();

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    public void initialize() {
        welcomeLabel.setText("Hi, " + Session.getUser().getName());

        colPdStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colPdDevice.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        colPdRequest.setCellValueFactory(c -> str(c.getValue().getRequestDate().format(DT)));
        colPdDue.setCellValueFactory(c -> str(c.getValue().getDueDate().toString()));
        colPdPurpose.setCellValueFactory(new PropertyValueFactory<>("purpose"));

        colApStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colApDevice.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        colApDue.setCellValueFactory(c -> str(c.getValue().getDueDate().toString()));

        colIsStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colIsDevice.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        colIsDue.setCellValueFactory(c -> str(c.getValue().getDueDate().toString()));
        colIsIssueDate.setCellValueFactory(c -> str(
                c.getValue().getIssueDate() == null ? "" : c.getValue().getIssueDate().format(DT)));

        colDvName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDvCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDvSerial.setCellValueFactory(new PropertyValueFactory<>("serialNo"));
        colDvStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        devCategoryBox.setItems(FXCollections.observableArrayList(
                "BOARD", "SENSOR", "MODULE", "DISPLAY", "ACTUATOR",
                "POWER", "CABLE", "COMPONENT", "ACCESSORY"));
        devCategoryBox.getSelectionModel().selectFirst();

        colPnStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colPnDevice.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        colPnType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPnAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colPnPaid.setCellValueFactory(new PropertyValueFactory<>("paidText"));

        colStName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colStAvailable.setCellValueFactory(new PropertyValueFactory<>("available"));
        colStIssued.setCellValueFactory(new PropertyValueFactory<>("issued"));
        colStDamaged.setCellValueFactory(new PropertyValueFactory<>("damaged"));

        loadPending();
    }

    private javafx.beans.property.SimpleStringProperty str(String s) {
        return new javafx.beans.property.SimpleStringProperty(s);
    }

    // ---------- Pending ----------
    @FXML
    public void loadPending() {
        try {
            pendingTable.setItems(FXCollections.observableArrayList(
                    bookingDAO.findByStatus("PENDING")));
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleApprove() {
        Booking b = pendingTable.getSelectionModel().getSelectedItem();
        if (b == null) { warn("Select a request first."); return; }
        try {
            bookingDAO.updateStatus(b.getId(), "APPROVED");
            loadPending();
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleReject() {
        Booking b = pendingTable.getSelectionModel().getSelectedItem();
        if (b == null) { warn("Select a request first."); return; }
        try {
            bookingDAO.reject(b);
            loadPending();
        } catch (Exception e) { error(e); }
    }

    // ---------- Issue / Return ----------
    @FXML
    public void loadIssueReturn() {
        try {
            approvedTable.setItems(FXCollections.observableArrayList(
                    bookingDAO.findByStatus("APPROVED")));
            issuedTable.setItems(FXCollections.observableArrayList(
                    bookingDAO.findByStatus("ISSUED")));
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleIssue() {
        Booking b = approvedTable.getSelectionModel().getSelectedItem();
        if (b == null) { warn("Select an approved booking first."); return; }
        try {
            bookingDAO.issue(b);
            loadIssueReturn();
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleReturn() {
        Booking b = issuedTable.getSelectionModel().getSelectedItem();
        if (b == null) { warn("Select an issued booking first."); return; }
        try {
            boolean damaged = damagedCheck.isSelected();
            bookingDAO.processReturn(b, damaged);

            StringBuilder info = new StringBuilder("Returned.");
            // auto late fine
            double lateFine = PenaltyCalculator.lateFine(b.getDueDate(), LocalDate.now());
            if (lateFine > 0) {
                penaltyDAO.insert(b.getId(), "LATE", lateFine);
                info.append(" Late fine: ").append(lateFine).append(" Tk.");
            }
            // damage fine
            if (damaged) {
                penaltyDAO.insert(b.getId(), "DAMAGE", PenaltyCalculator.DAMAGE_FINE);
                info.append(" Damage fine: ").append(PenaltyCalculator.DAMAGE_FINE).append(" Tk.");
            }
            returnInfoLabel.setText(info.toString());
            damagedCheck.setSelected(false);
            loadIssueReturn();
        } catch (Exception e) { error(e); }
    }

    // ---------- Stock (grouped) ----------
    @FXML
    public void loadStock() {
        try {
            var groups = deviceDAO.findGroups("", "ALL");
            stockTable.setItems(FXCollections.observableArrayList(groups));
            int units  = groups.stream().mapToInt(DeviceGroup::getTotal).sum();
            int free   = groups.stream().mapToInt(DeviceGroup::getAvailable).sum();
            int broken = groups.stream().mapToInt(DeviceGroup::getDamaged).sum();
            stockSummaryLabel.setText(
                    groups.size() + " device types  |  " + units + " units  |  "
                    + free + " available  |  " + broken + " damaged");
        } catch (Exception e) { error(e); }
    }

    // ---------- Devices ----------
    @FXML
    public void loadDevices() {
        try {
            deviceTable.setItems(FXCollections.observableArrayList(deviceDAO.findAll()));
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleAddDevice() {
        String name = devNameField.getText().trim();
        String serial = devSerialField.getText().trim();
        if (name.isEmpty() || serial.isEmpty()) { warn("Name and serial required."); return; }
        try {
            deviceDAO.insert(name, devCategoryBox.getValue(), serial);
            devNameField.clear();
            devSerialField.clear();
            loadDevices();
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleMarkRepaired() {
        Device d = deviceTable.getSelectionModel().getSelectedItem();
        if (d == null || !"DAMAGED".equals(d.getStatus())) {
            warn("Select a DAMAGED device first.");
            return;
        }
        try {
            deviceDAO.updateStatus(d.getId(), "AVAILABLE");
            loadDevices();
        } catch (Exception e) { error(e); }
    }

    // ---------- Penalties ----------
    @FXML
    public void loadPenalties() {
        try {
            var list = penaltyDAO.findAll();
            penaltyTable.setItems(FXCollections.observableArrayList(list));
            double unpaid = list.stream().filter(p -> !p.isPaid())
                                .mapToDouble(Penalty::getAmount).sum();
            totalLabel.setText("Total unpaid: " + unpaid + " Tk");
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleMarkPaid() {
        Penalty p = penaltyTable.getSelectionModel().getSelectedItem();
        if (p == null) { warn("Select a penalty first."); return; }
        try {
            penaltyDAO.markPaid(p.getId());
            loadPenalties();
        } catch (Exception e) { error(e); }
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        MainApp.switchScene("login.fxml", "Login");
    }

    private void warn(String msg)  { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void error(Exception e){ new Alert(Alert.AlertType.ERROR, "DB error: " + e.getMessage()).showAndWait(); }
}
