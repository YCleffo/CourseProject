package ru.nikogosyan.CourseProject.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Objects;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, org.springframework.ui.Model model) {
        if (authentication != null) {
            model.addAttribute("username", authentication.getName());

            String roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(Objects::nonNull)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.replace("ROLE_", ""))
                    .collect(Collectors.joining(", "));
            model.addAttribute("roles", roles);
        }

        if (!model.containsAttribute("page")) {
            model.addAttribute("page", "");
        }
    }
}
