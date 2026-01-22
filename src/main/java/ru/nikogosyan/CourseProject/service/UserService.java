package ru.nikogosyan.CourseProject.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikogosyan.CourseProject.entity.Role;
import ru.nikogosyan.CourseProject.entity.User;
import ru.nikogosyan.CourseProject.repository.RoleRepository;
import ru.nikogosyan.CourseProject.repository.UserRepository;
import ru.nikogosyan.CourseProject.utils.Roles;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        initRoles();
    }

    private void initRoles() {
        List<String> roleNames = Arrays.asList(Roles.ROLE_READ_ONLY, Roles.ROLE_USER, Roles.ROLE_ADMIN);
        for (String roleName : roleNames) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
                log.info("Созданная роль {}", roleName);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        log.info("Загрузка пользователя по имени пользователя {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        var auths = mapRolesToAuthorities(user.getRoles());
        log.info("Загруженные полномочия для {}: {}", username, auths);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getEnabled()),
                true, true, true,
                auths
        );
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void setRolesForUser(String username, Set<String> roleNames) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Set<Role> newRoles = roleNames.stream()
                .map(rn -> roleRepository.findByName(rn)
                        .orElseThrow(() -> new RuntimeException("Роль не найдена: " + rn)))
                .collect(Collectors.toSet());

        user.getRoles().clear();
        user.getRoles().addAll(newRoles);

        userRepository.save(user);
    }

    @Transactional
    public void registerUser(String username, String password) {
        log.info("Регистрация нового пользователя {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Имя пользователя уже существует");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);

        Role readOnlyRole = roleRepository.findByName(Roles.ROLE_READ_ONLY)
                .orElseThrow(() -> new RuntimeException("Роль, доступная только для чтения, не найдена"));
        user.getRoles().add(readOnlyRole);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
