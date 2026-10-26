package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : LocationService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Application service for location.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.LocationForm;
import com.ntaganira.heritier.ipos.backoffice.entity.StorageLocation;
import com.ntaganira.heritier.ipos.backoffice.enums.LocationType;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.StorageLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class LocationService {

    private final StorageLocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public LocationService(StorageLocationRepository locationRepository,
                           ProductRepository productRepository,
                           AuditService auditService,
                           CurrentUserService currentUser) {
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<LocationRow> rows() {
        Map<Long, ProductRepository.LocationUsage> usage = new HashMap<>();
        for (ProductRepository.LocationUsage item : productRepository.locationUsage()) {
            usage.put(item.getLocationId(), item);
        }
        return locationRepository.findAllByOrderByCodeAsc().stream()
                .map(location -> {
                    ProductRepository.LocationUsage item = usage.get(location.getId());
                    return new LocationRow(location,
                            item == null ? 0 : item.getProductCount(),
                            item == null ? BigDecimal.ZERO : item.getTotalQuantity());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public StorageLocation get(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Storage location not found."));
    }

    @Transactional(readOnly = true)
    public LocationForm formFor(Long id) {
        return LocationForm.from(get(id));
    }

    @Transactional
    public StorageLocation save(Long id, LocationForm form) {
        StorageLocation location = id == null ? new StorageLocation() : get(id);
        String code = form.getCode().trim().toUpperCase(Locale.ROOT);
        if (id == null && locationRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("A location with code " + code + " already exists.");
        }
        if (id != null && locationRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException("A location with code " + code + " already exists.");
        }
        Map<String, Object> before = id == null ? null : snapshot(location);
        location.setCode(code);
        location.setName(form.getName().trim());
        location.setType(form.getType() == null ? LocationType.OTHER : form.getType());
        location.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
        location.setColorAccent(normalizeColor(form.getColorAccent()));
        location.setActive(form.isActive());
        if (id == null) {
            location.setCreatedBy(currentUser.username());
        } else {
            location.setUpdatedBy(currentUser.username());
        }
        StorageLocation saved = locationRepository.save(location);
        Map<String, Object> after = snapshot(saved);
        if (before == null) {
            auditService.created("StorageLocation", saved.getId(), saved.label(), after);
        } else {
            auditService.updated("StorageLocation", saved.getId(), saved.label(), before, after);
        }
        return saved;
    }

    @Transactional
    public void updateAccent(Long id, String colorAccent) {
        StorageLocation location = get(id);
        String normalized = normalizeColor(colorAccent);
        if (Objects.equals(normalized, location.getColorAccent())) {
            return;
        }
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("colorAccent", location.getColorAccent());
        location.setColorAccent(normalized);
        location.setUpdatedBy(currentUser.username());
        locationRepository.save(location);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("colorAccent", normalized);
        auditService.updated("StorageLocation", location.getId(), location.label(), before, after);
    }

    @Transactional
    public void toggleActive(Long id) {
        StorageLocation location = get(id);
        Map<String, Object> before = Map.of("active", location.isActive());
        location.setActive(!location.isActive());
        location.setUpdatedBy(currentUser.username());
        locationRepository.save(location);
        auditService.updated("StorageLocation", location.getId(), location.label(),
                before, Map.of("active", location.isActive()));
    }

    private String normalizeColor(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String color = value.trim();
        if (!color.matches("^#[0-9a-fA-F]{6}$")) {
            throw new BusinessException("Color accent must be a hex value such as #268071.");
        }
        return color.toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> snapshot(StorageLocation location) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("code", location.getCode());
        map.put("name", location.getName());
        map.put("type", location.getType() == null ? null : location.getType().name());
        map.put("description", location.getDescription());
        map.put("colorAccent", location.getColorAccent());
        map.put("active", location.isActive());
        return map;
    }

    public record LocationRow(StorageLocation location, long productCount, BigDecimal totalQuantity) {
    }
}
