package ru.nikogosyan.CourseProject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.nikogosyan.CourseProject.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;

    @GetMapping("/roles")
    public String rolesPage(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("page", "admin");
        return "admin-roles";
    }

    @PostMapping("/roles/update")
    public String updateRoles(@RequestParam String username,
                              @RequestParam(name = "roles", required = false) List<String> roles,
                              RedirectAttributes ra) {
        try {
            Set<String> roleNames = (roles == null) ? Set.of() : new HashSet<>(roles);
            if (roleNames.isEmpty()) throw new RuntimeException("User must have at least one role");

            userService.setRolesForUser(username, roleNames);
            ra.addFlashAttribute("message", "Роли обновлены для пользователя: " + username);
        } catch (Exception e) {
            log.error("Error updating roles for {}: {}", username, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
