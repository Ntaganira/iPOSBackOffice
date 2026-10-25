package com.ntaganira.heritier.ipos.backoffice.dto;

import com.ntaganira.heritier.ipos.backoffice.entity.StorageLocation;
import com.ntaganira.heritier.ipos.backoffice.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LocationForm {

    private Long id;

    @NotBlank(message = "Location code is required.")
    @Size(max = 40, message = "Location code must be 40 characters or fewer.")
    private String code;

    @NotBlank(message = "Location name is required.")
    @Size(max = 120, message = "Location name must be 120 characters or fewer.")
    private String name;

    @NotNull(message = "Choose a location type.")
    private LocationType type = LocationType.RACK;

    @Size(max = 255, message = "Description must be 255 characters or fewer.")
    private String description;

    private boolean active = true;

    public static LocationForm from(StorageLocation location) {
        LocationForm form = new LocationForm();
        form.id = location.getId();
        form.code = location.getCode();
        form.name = location.getName();
        form.type = location.getType();
        form.description = location.getDescription();
        form.active = location.isActive();
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationType getType() {
        return type;
    }

    public void setType(LocationType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
