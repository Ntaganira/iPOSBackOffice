package com.ntaganira.heritier.ipos.backoffice.service;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public void toggleActive(Long id) {
        StorageLocation location = get(id);
        Map<String, Object> before = Map.of("active", location.isActive());
        location.setActive(!location.isActive());
        location.setUpdatedBy(currentUser.username());
        locationRepository.save(location);
        auditService.updated("StorageLocation", location.getId(), location.label(),
                before, Map.of("active", location.isActive()));
    }

    private Map<String, Object> snapshot(StorageLocation location) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("code", location.getCode());
        map.put("name", location.getName());
        map.put("type", location.getType() == null ? null : location.getType().name());
        map.put("description", location.getDescription());
        map.put("active", location.isActive());
        return map;
    }

    public record LocationRow(StorageLocation location, long productCount, BigDecimal totalQuantity) {
    }
}
