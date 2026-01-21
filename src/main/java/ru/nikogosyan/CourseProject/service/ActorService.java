package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.repository.ActorRepository;
import ru.nikogosyan.CourseProject.utils.Roles;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorService {

    private final ActorRepository actorRepository;
    private final MovieService movieService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<Actor> getAllActors(Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);
        log.info("Getting actors for user {}, isAdmin={}, isUser={}, isReadOnly={}",
                userInfo.username(), userInfo.isAdmin(), userInfo.isUser(), userInfo.isReadOnly());

        if (userInfo.isAdmin()) return actorRepository.findAll();
        if (userInfo.isUser()) return actorRepository.findByCreatedBy(userInfo.username());
        return actorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Actor getActorById(Long id) {
        return actorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actor not found"));
    }

    @Transactional(readOnly = true)
    public Actor getActorForView(Long id, Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);

        Actor actor = getActorById(id);
        if (userInfo.isAdmin() && !userInfo.isUser()) return actor;

        if (!Objects.equals(actor.getCreatedBy(), userInfo.username())) {
            throw new RuntimeException("You dont have permission to view this actor");
        }
        return actor;
    }

    @Transactional(readOnly = true)
    public List<Actor> getActorsByMovieIdForView(Long movieId, Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);

        List<Actor> actors = actorRepository.findByMovies_Id(movieId);

        if (userInfo.isAdmin() && !userInfo.isUser()) return actors;

        return actors.stream()
                .filter(a -> Objects.equals(a.getCreatedBy(), userInfo.username()))
                .toList();
    }

    // Используется в MovieController для списка фильмов: firstActorByMovieId
    @Transactional(readOnly = true)
    public Map<Long, Actor> getFirstActorsByMovieIds(List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) return Map.of();

        List<Actor> all = actorRepository.findByMovies_IdIn(movieIds);

        // для каждого movieId выбираем актёра с минимальным actor.id
        Map<Long, Actor> result = new HashMap<>();
        all.stream()
                .sorted(Comparator.comparing(Actor::getId))
                .forEach(a -> {
                    if (a.getMovies() == null) return;
                    for (Movie m : a.getMovies()) {
                        if (m == null || m.getId() == null) continue;
                        if (!movieIds.contains(m.getId())) continue;
                        result.putIfAbsent(m.getId(), a);
                    }
                });

        return result;
    }

    @Transactional
    public Actor saveActor(Actor actor, Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);
        log.info("Saving actor by user {}, actor={}", userInfo.username(), actor.getName());

        if (actor.getMovieIds() == null || actor.getMovieIds().isEmpty()) {
            throw new RuntimeException("Movie is required");
        }

        Set<Movie> movies = actor.getMovieIds().stream()
                .map(id -> movieService.getMovieForView(id, authentication))
                .collect(Collectors.toSet());

        actor.setCreatedBy(userInfo.username());
        actor.setMovies(movies);

        return actorRepository.save(actor);
    }

    @Transactional
    public void deleteActor(Long id, Authentication authentication) {
        Actor actor = getActorById(id);

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals(Roles.ROLE_ADMIN)); // важно: ROLEADMIN как в вашем Roles [file:4]

        if (!isAdmin && !Objects.equals(actor.getCreatedBy(), username)) {
            throw new RuntimeException("You dont have permission to delete this actor");
        }

        log.info("Deleting actor by user {}, actorId={}", username, id);
        actorRepository.deleteById(id);
    }

    @Transactional
    public Actor updateActor(Long id, Actor updatedActor, Authentication authentication) {
        Actor actor = getActorById(id);

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals(Roles.ROLE_ADMIN)); // важно: ROLEADMIN [file:4]

        if (!isAdmin && !Objects.equals(actor.getCreatedBy(), username)) {
            throw new RuntimeException("You dont have permission to update this actor");
        }

        if (updatedActor.getMovieIds() == null || updatedActor.getMovieIds().isEmpty()) {
            throw new RuntimeException("Movie is required");
        }

        Set<Movie> movies = updatedActor.getMovieIds().stream()
                .map(mid -> movieService.getMovieForView(mid, authentication))
                .collect(Collectors.toSet());

        log.info("Updating actor by user {}, actorId={}", username, id);

        actor.setName(updatedActor.getName());
        actor.setRoleName(updatedActor.getRoleName());
        actor.setSalary(updatedActor.getSalary());
        actor.setMovies(movies);

        return actorRepository.save(actor);
    }
}
