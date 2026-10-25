package com.ntaganira.heritier.ipos.backoffice.enums;

public enum LocationType {

    RACK("Rack"),
    DRAWER("Drawer"),
    SHELF("Shelf"),
    ROOM("Room"),
    OTHER("Other");

    private final String label;

    LocationType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
