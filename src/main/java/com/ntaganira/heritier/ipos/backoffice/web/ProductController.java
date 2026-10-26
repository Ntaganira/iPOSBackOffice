package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : ProductController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for product.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.ProductForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAuthority('PRODUCT_VIEW')")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) Long locationId,
                           @RequestParam(required = false) Boolean active,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "25") int size,
                           Model model) {
        Page<Product> products = productService.search(q, categoryId, active, locationId,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("products", products);
        model.addAttribute("categories", productService.categories());
        model.addAttribute("locations", productService.locations());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("locationId", locationId);
        model.addAttribute("active", active);
        return "products/list";
    }

    @GetMapping("/products/new")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String createForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        formOptions(model);
        model.addAttribute("editing", false);
        return "products/form";
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String create(@Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult binding,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            formOptions(model);
            model.addAttribute("editing", false);
            return "products/form";
        }
        Product product = productService.create(form, image);
        redirect.addFlashAttribute("success", "Product " + product.getName() + " created.");
        return "redirect:/products";
    }

    @GetMapping("/products/{id}/edit")
    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productForm", productService.formFor(id));
        model.addAttribute("product", productService.get(id));
        formOptions(model);
        model.addAttribute("editing", true);
        return "products/form";
    }

    @PostMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult binding,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("product", productService.get(id));
            formOptions(model);
            model.addAttribute("editing", true);
            return "products/form";
        }
        Product product = productService.update(id, form, image);
        redirect.addFlashAttribute("success", "Product " + product.getName() + " updated.");
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/toggle")
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        productService.toggleActive(id);
        redirect.addFlashAttribute("success", "Product status updated.");
        return "redirect:/products";
    }

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(productService.exportCsv());
    }

    @PostMapping("/products/import")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        if (file == null || file.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose a CSV file to import.");
            return "redirect:/products";
        }
        try {
            ProductService.ImportResult result = productService.importCsv(file.getInputStream());
            String message = result.created() + " product(s) created, " + result.updated() + " updated.";
            if (result.errors().isEmpty()) {
                redirect.addFlashAttribute("success", message);
            } else {
                redirect.addFlashAttribute("error", message + " " + String.join(" ", result.errors()));
            }
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Import failed: " + ex.getMessage());
        }
        return "redirect:/products";
    }

    @GetMapping("/products/categories")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    public String categories(Model model) {
        model.addAttribute("categories", productService.categories());
        return "products/categories";
    }

    @PostMapping("/products/categories")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    public String saveCategory(@RequestParam(required = false) Long id,
                               @RequestParam String name,
                               @RequestParam(defaultValue = "false") boolean active,
                               RedirectAttributes redirect) {
        productService.saveCategory(id, name, active);
        redirect.addFlashAttribute("success", "Category saved.");
        return "redirect:/products/categories";
    }

    @GetMapping("/products/units")
    @PreAuthorize("hasAuthority('UNIT_MANAGE')")
    public String units(Model model) {
        model.addAttribute("units", productService.units());
        return "products/units";
    }

    @PostMapping("/products/units")
    @PreAuthorize("hasAuthority('UNIT_MANAGE')")
    public String saveUnit(@RequestParam(required = false) Long id,
                           @RequestParam String name,
                           @RequestParam String code,
                           @RequestParam(defaultValue = "false") boolean active,
                           RedirectAttributes redirect) {
        productService.saveUnit(id, name, code, active);
        redirect.addFlashAttribute("success", "Unit saved.");
        return "redirect:/products/units";
    }

    private void formOptions(Model model) {
        model.addAttribute("categories", productService.activeCategories());
        model.addAttribute("units", productService.activeUnits());
        model.addAttribute("locations", productService.locations());
        model.addAttribute("taxCategories", productService.taxCategories());
    }
}
