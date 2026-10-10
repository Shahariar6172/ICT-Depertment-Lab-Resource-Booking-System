package com.mbstu.labbooking.model;

import java.time.LocalDateTime;

public class Penalty {
    private int id;
    private int bookingId;
    private String type;      // LATE / DAMAGE
    private double amount;
    private boolean paid;
    private LocalDateTime createdAt;

    // joined display fields
    private String studentName;
    private String deviceName;

    public Penalty(int id, int bookingId, String type, double amount,
                   boolean paid, LocalDateTime createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.type = type;
        this.amount = amount;
        this.paid = paid;
        this.createdAt = createdAt;
    }

    public int getId()                   { return id; }
    public int getBookingId()            { return bookingId; }
    public String getType()              { return type; }
    public double getAmount()            { return amount; }
    public boolean isPaid()              { return paid; }
    public String getPaidText()          { return paid ? "PAID" : "UNPAID"; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public String getStudentName()       { return studentName; }
    public String getDeviceName()        { return deviceName; }
    public void setStudentName(String s) { this.studentName = s; }
    public void setDeviceName(String d)  { this.deviceName = d; }
}
