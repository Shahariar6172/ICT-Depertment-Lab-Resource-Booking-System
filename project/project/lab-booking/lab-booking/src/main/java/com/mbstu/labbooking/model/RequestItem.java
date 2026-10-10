package com.mbstu.labbooking.model;

/** Immutable shopping-list line; quantities are merged in the student controller. */
public class RequestItem {
    private final MaterialOption option;
    private final int quantity;
    public RequestItem(MaterialOption option, int quantity) {
        if (option == null || quantity < 1) throw new IllegalArgumentException("Invalid request item.");
        this.option = option;
        this.quantity = quantity;
    }
    public MaterialOption getOption() { return option; }
    public String getMaterialName() { return option.getMaterialName(); }
    public String getVariantName() { return option.getVariantName(); }
    public String getCategory() { return option.getCategory(); }
    public int getQuantity() { return quantity; }
}
