package ru.nikogosyan.CourseProject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.nikogosyan.CourseProject.service.UserService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/roles/add")
    public String addRole(@RequestParam String username,
                          @RequestParam String roleName,
                          Model model) {
        try {
            userService.addRoleToUser(username, "ROLE_" + roleName);
            log.info("Role {} added to user {}", roleName, username);
            model.addAttribute("message", "Role added successfully");
        } catch (Exception e) {
            log.error("Error adding role: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("users", userService.findAllUsers());
        return "admin-roles";
    }

    @PostMapping("/roles/remove")
    public String removeRole(@RequestParam String username,
                             @RequestParam String roleName,
                             Model model) {
        try {
            userService.removeRoleFromUser(username, "ROLE_" + roleName);
            log.info("Role {} removed from user {}", roleName, username);
            model.addAttribute("message", "Role removed successfully");
        } catch (Exception e) {
            log.error("Error removing role: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("users", userService.findAllUsers());
        return "admin-roles";
    }
}