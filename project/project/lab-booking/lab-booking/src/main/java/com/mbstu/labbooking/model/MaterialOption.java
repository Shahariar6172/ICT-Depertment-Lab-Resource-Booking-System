package com.mbstu.labbooking.model;

/** One inventory-backed material variant. Name + category identifies physical stock. */
public class MaterialOption {
    private final String materialName, variantName, deviceName, category;
    private final int available;
    public MaterialOption(String materialName, String variantName, String deviceName,
                          String category, int available) {
        this.materialName = materialName;
        this.variantName = variantName;
        this.deviceName = deviceName;
        this.category = category;
        this.available = available;
    }
    public String getMaterialName() { return materialName; }
    public String getVariantName() { return variantName; }
    public String getDeviceName() { return deviceName; }
    public String getCategory() { return category; }
    public int getAvailable() { return available; }
    public boolean sameStock(MaterialOption other) {
        return deviceName.equals(other.deviceName) && category.equals(other.category);
    }
    @Override public String toString() {
        return variantName + " — " + deviceName + " (" + category + ", " + available + " available)";
    }
}
