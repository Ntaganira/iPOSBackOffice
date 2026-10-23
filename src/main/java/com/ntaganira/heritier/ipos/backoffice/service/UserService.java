package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.dto.UserForm;
import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public UserService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       StorageService storageService,
                       AuditService auditService,
                       CurrentUserService currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageService = storageService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Page<AppUser> search(String q, UserRole role, Pageable pageable) {
        return userRepository.search(StringUtils.hasText(q)
                ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "", role, pageable);
    }

    @Transactional(readOnly = true)
    public AppUser get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found."));
    }

    @Transactional(readOnly = true)
    public UserForm formFor(Long id) {
        return UserForm.from(get(id));
    }

    @Transactional
    public AppUser create(UserForm form) {
        String username = form.getUsername().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("This username is already in use.");
        }
        validateEmail(form.getEmail(), null);
        validatePassword(form.getPassword(), form.getConfirmPassword());
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setPasswordChangeRequired(form.isPasswordChangeRequired());
        applyForm(user, form, true);
        AppUser saved = userRepository.save(user);
        auditService.created("User", saved.getId(), saved.getUsername(), Map.of(
                "username", saved.getUsername(), "fullName", saved.getFullName(),
                "role", saved.getRole().name(), "enabled", saved.isEnabled()));
        return saved;
    }

    @Transactional
    public AppUser update(Long id, UserForm form) {
        AppUser user = get(id);
        Map<String, Object> before = snapshot(user);
        validateEmail(form.getEmail(), id);
        String username = form.getUsername().trim().toLowerCase(Locale.ROOT);
        if (!user.getUsername().equalsIgnoreCase(username)
                && userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("This username is already in use.");
        }
        if (user.getRole() == UserRole.ADMIN && form.getRole() != UserRole.ADMIN) {
            ensureNotLastAdmin(user);
        }
        if (user.getRole() == UserRole.ADMIN && !form.isEnabled()) {
            ensureNotLastAdmin(user);
        }
        user.setUsername(username);
        applyForm(user, form, true);
        if (StringUtils.hasText(form.getPassword())) {
            validatePassword(form.getPassword(), form.getConfirmPassword());
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
            user.setPasswordChangeRequired(form.isPasswordChangeRequired());
            user.setFailedLoginAttempts(0);
            user.setLocked(false);
        }
        AppUser saved = userRepository.save(user);
        auditService.updated("User", saved.getId(), saved.getUsername(), before, snapshot(saved));
        return saved;
    }

    @Transactional
    public void toggleEnabled(Long id) {
        AppUser user = get(id);
        if (user.getRole() == UserRole.ADMIN && user.isEnabled()) {
            ensureNotLastAdmin(user);
        }
        Map<String, Object> before = Map.of("enabled", user.isEnabled());
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        auditService.updated("User", user.getId(), user.getUsername(), before,
                Map.of("enabled", user.isEnabled()));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword, boolean requireChange) {
        AppUser user = get(id);
        validatePassword(newPassword, newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(requireChange);
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
        auditService.record("PASSWORD_RESET", "User", user.getId(), user.getUsername(),
                null, Map.of("password", "***", "requireChange", requireChange));
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword, String confirmPassword) {
        AppUser user = get(id);
        if (!passwordEncoder.matches(currentPassword == null ? "" : currentPassword, user.getPasswordHash())) {
            throw new BusinessException("Your current password is not correct.");
        }
        validatePassword(newPassword, confirmPassword);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("Choose a password different from your current one.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);
        auditService.record("PASSWORD_CHANGE", "User", user.getId(), user.getUsername(),
                null, Map.of("password", "***"));
    }

    @Transactional
    public AppUser updateProfile(Long id, String fullName, String email, String phone,
                                 String locale, MultipartFile avatar) {
        AppUser user = get(id);
        if (!StringUtils.hasText(fullName)) {
            throw new BusinessException("Full name is required.");
        }
        validateEmail(email, id);
        Map<String, Object> before = snapshot(user);
        user.setFullName(fullName.trim());
        user.setEmail(trimToNull(email));
        user.setPhone(trimToNull(phone));
        user.setLocale(StringUtils.hasText(locale) ? locale.trim() : "en");
        String oldAvatar = user.getAvatarPath();
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatarPath(storageService.saveAvatar(avatar));
            user.setAvatarContentType(storageService.contentType(user.getAvatarPath()));
            if (oldAvatar != null) {
                storageService.delete(oldAvatar);
            }
        }
        AppUser saved = userRepository.save(user);
        auditService.updated("User", saved.getId(), saved.getUsername(), before, snapshot(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AppUser> activeUsers() {
        return userRepository.findByEnabledTrueOrderByFullNameAsc();
    }

    private void applyForm(AppUser user, UserForm form, boolean allowRole) {
        user.setFullName(form.getFullName().trim());
        user.setEmail(trimToNull(form.getEmail()));
        user.setPhone(trimToNull(form.getPhone()));
        user.setEnabled(form.isEnabled());
        if (allowRole && form.getRole() != null) {
            user.setRole(form.getRole());
        }
    }

    private void validateEmail(String email, Long id) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(existing -> {
            if (id == null || !existing.getId().equals(id)) {
                throw new BusinessException("This email address is already used by another user.");
            }
        });
    }

    private void validatePassword(String password, String confirm) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long.");
        }
        if (!password.equals(confirm)) {
            throw new BusinessException("The passwords do not match.");
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            throw new BusinessException("Use at least one letter and one number in the password.");
        }
    }

    private void ensureNotLastAdmin(AppUser user) {
        if (user.isEnabled() && userRepository.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw new BusinessException("At least one enabled administrator must remain.");
        }
    }

    private Map<String, Object> snapshot(AppUser user) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("username", user.getUsername());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole().name());
        map.put("enabled", user.isEnabled());
        map.put("locked", user.isLocked());
        map.put("passwordChangeRequired", user.isPasswordChangeRequired());
        map.put("avatarPath", user.getAvatarPath());
        return map;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
