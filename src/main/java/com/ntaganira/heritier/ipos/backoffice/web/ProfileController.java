package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.service.BusinessException;
import com.ntaganira.heritier.ipos.backoffice.service.CurrentUserService;
import com.ntaganira.heritier.ipos.backoffice.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final CurrentUserService currentUser;
    private final UserService userService;

    public ProfileController(CurrentUserService currentUser, UserService userService) {
        this.currentUser = currentUser;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        AppUser user = currentUser.requireUser();
        model.addAttribute("profileUser", user);
        return "profile/index";
    }

    @PostMapping("/profile")
    public String update(@RequestParam String fullName,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String locale,
                         @RequestPart(value = "avatar", required = false) MultipartFile avatar,
                         RedirectAttributes redirect) {
        try {
            userService.updateProfile(currentUser.requireUser().getId(), fullName, email, phone, locale, avatar);
            redirect.addFlashAttribute("success", "Profile updated.");
        } catch (BusinessException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirect) {
        try {
            userService.changePassword(currentUser.requireUser().getId(), currentPassword, newPassword, confirmPassword);
            redirect.addFlashAttribute("success", "Password changed.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
