package ru.nikogosyan.CourseProject.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.nikogosyan.CourseProject.service.UserService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {
        try {
            if (username.length() < 3) {
                model.addAttribute("error", "Имя пользователя должно быть не менее 3 символов");
                return "register";
            }

            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Пароли не совпадают");
                return "register";
            }

            userService.registerUser(username, password);
            log.info("User registered successfully: {}", username);
            model.addAttribute("message", "Регистрация прошла успешно! Пожалуйста, войдите в систему.");
            return "login";
        } catch (RuntimeException e) {
            log.error("Ошибка регистрации: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}