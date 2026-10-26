package com.ntaganira.heritier.ipos.backoffice.enums;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.enums
 * - File      : LocationType.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Enumeration for location type.
 * </pre>
 */

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
