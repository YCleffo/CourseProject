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
        boolean isAdmin = hasAuthority(authentication, Roles.ROLE_ADMIN);
        boolean isUser = hasAuthority(authentication, Roles.ROLE_USER);
        boolean isReadOnly = hasAuthority(authentication, Roles.ROLE_READ_ONLY);
        return new UserInfo(username, isAdmin, isUser, isReadOnly);
    }

    public boolean isAdmin(Authentication authentication) {
        return hasAuthority(authentication, Roles.ROLE_ADMIN);
    }

    @SuppressWarnings("unused")
    public boolean isUser(Authentication authentication) {
        return hasAuthority(authentication, Roles.ROLE_USER);
    }

    public boolean isReadOnly(Authentication authentication) {
        return hasAuthority(authentication, Roles.ROLE_READ_ONLY);
    }

    public boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        } else {
            authentication.getAuthorities();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(authority::equals);
    }

    public record UserInfo(String username, boolean isAdmin, boolean isUser, boolean isReadOnly) {}
}
