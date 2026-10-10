package com.mbstu.labbooking.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Booking {
    private int id;
    private int userId;
    private int deviceId;
    private LocalDateTime requestDate;
    private LocalDate dueDate;
    private LocalDateTime issueDate;
    private LocalDateTime returnDate;
    private String purpose;
    private String status;      // PENDING/APPROVED/REJECTED/ISSUED/RETURNED

    // joined display fields
    private String requestCode;
    public String getRequestCode() { return requestCode == null ? "Legacy" : requestCode.substring(0, 8); }
    public void setRequestCode(String code) { requestCode = code; }

    private String studentName;
    private String deviceName;

    public Booking(int id, int userId, int deviceId, LocalDateTime requestDate,
                   LocalDate dueDate, LocalDateTime issueDate, LocalDateTime returnDate,
                   String purpose, String status) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.requestDate = requestDate;
        this.dueDate = dueDate;
        this.issueDate = issueDate;
        this.returnDate = returnDate;
        this.purpose = purpose;
        this.status = status;
    }

    public int getId()                     { return id; }
    public int getUserId()                 { return userId; }
    public int getDeviceId()               { return deviceId; }
    public LocalDateTime getRequestDate()  { return requestDate; }
    public LocalDate getDueDate()          { return dueDate; }
    public LocalDateTime getIssueDate()    { return issueDate; }
    public LocalDateTime getReturnDate()   { return returnDate; }
    public String getPurpose()             { return purpose; }
    public String getStatus()              { return status; }

    public String getStudentName()         { return studentName; }
    public String getDeviceName()          { return deviceName; }
    public void setStudentName(String s)   { this.studentName = s; }
    public void setDeviceName(String d)    { this.deviceName = d; }
}
