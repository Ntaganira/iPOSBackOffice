package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.dto.LocationForm;
import com.ntaganira.heritier.ipos.backoffice.enums.LocationType;
import com.ntaganira.heritier.ipos.backoffice.service.BusinessException;
import com.ntaganira.heritier.ipos.backoffice.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAuthority('STOCK_VIEW')")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/stock/locations")
    public String locations(Model model) {
        model.addAttribute("rows", locationService.rows());
        return "stock/locations";
    }

    @GetMapping("/stock/locations/new")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    public String createForm(Model model) {
        model.addAttribute("locationForm", new LocationForm());
        model.addAttribute("editing", false);
        formOptions(model);
        return "stock/location-form";
    }

    @PostMapping("/stock/locations")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    public String create(@Valid @ModelAttribute("locationForm") LocationForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            formOptions(model);
            return "stock/location-form";
        }
        try {
            locationService.save(null, form);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("editing", false);
            formOptions(model);
            return "stock/location-form";
        }
        redirect.addFlashAttribute("success", "Storage location created.");
        return "redirect:/stock/locations";
    }

    @GetMapping("/stock/locations/{id}/edit")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("locationForm", locationService.formFor(id));
        model.addAttribute("editing", true);
        formOptions(model);
        return "stock/location-form";
    }

    @PostMapping("/stock/locations/{id}")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("locationForm") LocationForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", true);
            formOptions(model);
            return "stock/location-form";
        }
        try {
            locationService.save(id, form);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("editing", true);
            formOptions(model);
            return "stock/location-form";
        }
        redirect.addFlashAttribute("success", "Storage location updated.");
        return "redirect:/stock/locations";
    }

    @PostMapping("/stock/locations/{id}/toggle")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        locationService.toggleActive(id);
        redirect.addFlashAttribute("success", "Storage location status updated.");
        return "redirect:/stock/locations";
    }

    private void formOptions(Model model) {
        model.addAttribute("types", LocationType.values());
    }
}
