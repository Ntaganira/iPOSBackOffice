package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.dto.CartRequest;
import com.ntaganira.heritier.ipos.backoffice.dto.CustomerForm;
import com.ntaganira.heritier.ipos.backoffice.dto.ProductForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.entity.HeldSale;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.enums.CustomerType;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.service.BusinessException;
import com.ntaganira.heritier.ipos.backoffice.service.CustomerService;
import com.ntaganira.heritier.ipos.backoffice.service.ProductService;
import com.ntaganira.heritier.ipos.backoffice.service.SaleService;
import com.ntaganira.heritier.ipos.backoffice.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyAuthority('POS_USE','SALE_CREATE')")
public class PosController {

    private final ProductService productService;
    private final CustomerService customerService;
    private final SaleService saleService;
    private final SettingsService settingsService;

    public PosController(ProductService productService,
                         CustomerService customerService,
                         SaleService saleService,
                         SettingsService settingsService) {
        this.productService = productService;
        this.customerService = customerService;
        this.saleService = saleService;
        this.settingsService = settingsService;
    }

    @GetMapping("/pos")
    public String pos(Model model) {
        model.addAttribute("products", productService.forPos(null, 60).stream().map(this::productJson).toList());
        model.addAttribute("customers", customerService.active().stream().map(this::customerJson).toList());
        model.addAttribute("heldSales", saleService.heldSales());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("settings", settingsService.get());
        model.addAttribute("categories", productService.activeCategories());
        model.addAttribute("units", productService.activeUnits());
        model.addAttribute("taxCategories", productService.taxCategories());
        model.addAttribute("customerTypes", CustomerType.values());
        return "pos/index";
    }

    @GetMapping("/pos/products")
    @ResponseBody
    public List<Map<String, Object>> products(@RequestParam(required = false) String q) {
        return productService.forPos(q, 40).stream().map(this::productJson).toList();
    }

    @GetMapping("/pos/customers")
    @ResponseBody
    public List<Map<String, Object>> customers(@RequestParam(required = false) String q) {
        return customerService.forPos(q, 20).stream().map(this::customerJson).toList();
    }

    @GetMapping("/pos/customers/lookup")
    @ResponseBody
    public Map<String, Object> lookupCustomer(@RequestParam(required = false) String phone,
                                              @RequestParam(required = false) String tin) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", false);
        customerService.lookup(phone, tin).ifPresent(customer -> {
            result.put("found", true);
            result.put("customer", customerJson(customer));
        });
        return result;
    }

    @PostMapping("/pos/customers")
    @ResponseBody
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public Map<String, Object> quickCustomer(@Valid @RequestBody CustomerForm form) {
        Customer customer = customerService.create(form);
        return customerJson(customer);
    }

    @PostMapping("/pos/products")
    @ResponseBody
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public Map<String, Object> quickProduct(@Valid @RequestBody ProductForm form) {
        Product product = productService.create(form, null);
        return productJson(product);
    }

    @PostMapping("/pos/checkout")
    @ResponseBody
    public Map<String, Object> checkout(@RequestBody CartRequest cart) {
        Sale sale = saleService.completeSale(cart);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", sale.getId());
        result.put("receiptNumber", sale.getReceiptNumber());
        result.put("total", sale.getTotal());
        result.put("amountPaid", sale.getAmountPaid());
        result.put("changeDue", sale.getChangeDue());
        result.put("balanceDue", sale.getBalanceDue());
        result.put("paymentStatus", sale.getPaymentStatus());
        result.put("customerName", sale.getCustomer() == null ? "Walk-in" : sale.getCustomer().getName());
        result.put("receiptUrl", "/sales/" + sale.getId() + "/receipt");
        result.put("printFormat", settingsService.get().getDefaultPrintFormat().name());
        return result;
    }

    @PostMapping("/pos/hold")
    @ResponseBody
    public Map<String, Object> hold(@RequestBody CartRequest cart, @RequestParam(required = false) String label) {
        HeldSale held = saleService.hold(cart, label);
        return Map.of("id", held.getId(), "label", held.getLabel() == null ? "Held sale" : held.getLabel());
    }

    @GetMapping("/pos/held")
    @ResponseBody
    public List<Map<String, Object>> held() {
        return saleService.heldSales().stream()
                .map(held -> Map.<String, Object>of(
                        "id", held.getId(),
                        "label", held.getLabel() == null ? "Held sale" : held.getLabel(),
                        "createdAt", held.getCreatedAt()))
                .toList();
    }

    @GetMapping("/pos/held/{id}")
    @ResponseBody
    public CartRequest heldCart(@PathVariable Long id) {
        return saleService.heldCart(id);
    }

    @DeleteMapping("/pos/held/{id}")
    @ResponseBody
    public Map<String, Object> deleteHeld(@PathVariable Long id) {
        saleService.deleteHeld(id);
        return Map.of("deleted", true);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> businessError(BusinessException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validationError(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Please check the highlighted fields.", "fields", fields));
    }

    private Map<String, Object> productJson(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("code", product.getCode());
        map.put("barcode", product.getBarcode());
        map.put("price", product.getSellingPrice());
        map.put("stock", product.getStockQuantity());
        map.put("unit", product.getUnit() == null ? "" : product.getUnit().getCode());
        map.put("taxRate", product.getTaxCategory() == null || product.getTaxCategory().isExempt()
                ? BigDecimal.ZERO : product.getTaxCategory().getRate());
        map.put("category", product.getCategory() == null ? "" : product.getCategory().getName());
        map.put("imagePath", product.getImagePath());
        return map;
    }

    private Map<String, Object> customerJson(Customer customer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", customer.getId());
        map.put("name", customer.getName());
        map.put("phone", customer.getPhone());
        map.put("tin", customer.getTin());
        map.put("email", customer.getEmail());
        map.put("address", customer.getAddress());
        map.put("customerType", customer.getCustomerType());
        map.put("balance", customer.getBalance());
        map.put("creditLimit", customer.getCreditLimit());
        return map;
    }
}
