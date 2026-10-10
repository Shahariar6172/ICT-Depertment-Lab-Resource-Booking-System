package com.mbstu.labbooking.model;

/**
 * One row per distinct device name+category, with unit counts.
 * Used for the stock / browse views so that 15 Arduino Megas show as
 * a single row saying "15 total, 12 available" instead of 15 rows.
 */
public class DeviceGroup {
    private final String name;
    private final String category;
    private final int total;
    private final int available;
    private final int issued;
    private final int damaged;

    public DeviceGroup(String name, String category,
                       int total, int available, int issued, int damaged) {
        this.name = name;
        this.category = category;
        this.total = total;
        this.available = available;
        this.issued = issued;
        this.damaged = damaged;
    }

    public String getName()     { return name; }
    public String getCategory() { return category; }
    public int getTotal()       { return total; }
    public int getAvailable()   { return available; }
    public int getIssued()      { return issued; }
    public int getDamaged()     { return damaged; }

    /** "12 / 15" -- available out of total. */
    public String getStockText() { return available + " / " + total; }

    @Override
    public String toString() { return name + " (" + available + " of " + total + " free)"; }
}
