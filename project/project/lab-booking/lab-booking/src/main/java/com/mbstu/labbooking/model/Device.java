package com.mbstu.labbooking.model;

public class Device {
    private int id;
    private String name;
    private String category;
    private String serialNo;
    private String status;   // AVAILABLE / BOOKED / ISSUED / DAMAGED

    public Device(int id, String name, String category, String serialNo, String status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.serialNo = serialNo;
        this.status = status;
    }

    public int getId()           { return id; }
    public String getName()      { return name; }
    public String getCategory()  { return category; }
    public String getSerialNo()  { return serialNo; }
    public String getStatus()    { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() { return name + " (" + serialNo + ")"; }
}
