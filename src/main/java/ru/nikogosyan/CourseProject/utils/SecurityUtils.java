package ru.nikogosyan.CourseProject.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class SecurityUtils {

    public UserInfo getUserInfo(Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        boolean isUser = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_USER"));

        return new UserInfo(username, isAdmin, isUser);
    }

    public record UserInfo(String username, boolean isAdmin, boolean isUser) {
    }
}