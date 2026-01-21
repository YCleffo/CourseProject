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

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        List<Actor> actors = actorRepository.findByMovie_Id(movieId); // было findByMovieId(movieId)

        if (userInfo.isAdmin() && !userInfo.isUser()) return actors;

        return actors.stream()
                .filter(a -> Objects.equals(a.getCreatedBy(), userInfo.username()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Actor> getFirstActorsByMovieIds(List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) return Map.of();

        List<Actor> all = actorRepository.findByMovie_IdIn(movieIds); // было findByMovieIdIn(movieIds)

        return all.stream()
                .collect(Collectors.groupingBy(a -> a.getMovie().getId()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .sorted(java.util.Comparator.comparing(Actor::getId))
                                .findFirst()
                                .orElse(null)
                ));
    }

    @Transactional
    public Actor saveActor(Actor actor, Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);

        log.info("Saving actor by user {}, actor={}", userInfo.username(), actor.getName());

        if (actor.getMovieId() == null) {
            throw new RuntimeException("Movie is required");
        }

        Movie movie = movieService.getMovieForView(actor.getMovieId(), authentication);

        actor.setCreatedBy(userInfo.username());
        actor.setMovie(movie);

        return actorRepository.save(actor);
    }

    @Transactional
    public void deleteActor(Long id, Authentication authentication) {
        Actor actor = getActorById(id);

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals(Roles.ROLE_ADMIN));

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
                .anyMatch(role -> role.equals(Roles.ROLE_ADMIN));

        if (!isAdmin && !Objects.equals(actor.getCreatedBy(), username)) {
            throw new RuntimeException("You dont have permission to update this actor");
        }

        if (updatedActor.getMovieId() == null) {
            throw new RuntimeException("Movie is required");
        }

        Movie movie = movieService.getMovieForView(updatedActor.getMovieId(), authentication);

        log.info("Updating actor by user {}, actorId={}", username, id);

        actor.setName(updatedActor.getName());
        actor.setRoleName(updatedActor.getRoleName());
        actor.setSalary(updatedActor.getSalary());
        actor.setMovie(movie);

        return actorRepository.save(actor);
    }
}
