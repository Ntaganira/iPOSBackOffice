package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : UserController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for user.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.UserForm;
import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.service.BusinessException;
import com.ntaganira.heritier.ipos.backoffice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAuthority('USER_VIEW')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q,
                        @RequestParam(required = false) UserRole role,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "25") int size,
                        Model model) {
        Page<AppUser> users = userService.search(q, role,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.ASC, "fullName")));
        model.addAttribute("users", users);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        return "users/list";
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String createForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        formOptions(model);
        model.addAttribute("editing", false);
        return "users/form";
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String create(@Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            formOptions(model);
            model.addAttribute("editing", false);
            return "users/form";
        }
        try {
            AppUser user = userService.create(form);
            redirect.addFlashAttribute("success", "User " + user.getUsername() + " created.");
            return "redirect:/users";
        } catch (BusinessException ex) {
            model.addAttribute("formError", ex.getMessage());
            formOptions(model);
            model.addAttribute("editing", false);
            return "users/form";
        }
    }

    @GetMapping("/users/{id}/edit")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("userForm", userService.formFor(id));
        formOptions(model);
        model.addAttribute("editing", true);
        model.addAttribute("editedUser", userService.get(id));
        return "users/form";
    }

    @PostMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            formOptions(model);
            model.addAttribute("editing", true);
            model.addAttribute("editedUser", userService.get(id));
            return "users/form";
        }
        try {
            AppUser user = userService.update(id, form);
            redirect.addFlashAttribute("success", "User " + user.getUsername() + " updated.");
            return "redirect:/users";
        } catch (BusinessException ex) {
            model.addAttribute("formError", ex.getMessage());
            formOptions(model);
            model.addAttribute("editing", true);
            model.addAttribute("editedUser", userService.get(id));
            return "users/form";
        }
    }

    @PostMapping("/users/{id}/toggle")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            userService.toggleEnabled(id);
            redirect.addFlashAttribute("success", "User status updated.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                @RequestParam(defaultValue = "true") boolean requireChange,
                                RedirectAttributes redirect) {
        try {
            userService.resetPassword(id, newPassword, requireChange);
            redirect.addFlashAttribute("success", "Password reset.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users/" + id + "/edit";
    }

    private void formOptions(Model model) {
        model.addAttribute("roles", UserRole.values());
    }
}
