package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.entity.MovieCast;
import ru.nikogosyan.CourseProject.repository.ActorRepository;
import ru.nikogosyan.CourseProject.repository.MovieCastRepository;
import ru.nikogosyan.CourseProject.repository.MovieRepository;
import ru.nikogosyan.CourseProject.utils.Roles;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorService {

    private final ActorRepository actorRepository;
    private final MovieRepository movieRepository;
    private final MovieCastRepository movieCastRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<Actor> getAllActors(Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);
        if (userInfo.isAdmin()) return actorRepository.findAll();
        if (userInfo.isUser()) return actorRepository.findByCreatedBy(userInfo.username());
        return actorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Actor getActorById(Long id) {
        return actorRepository.findById(id).orElseThrow(() -> new RuntimeException("Actor not found"));
    }

    @Transactional(readOnly = true)
    public Actor getActorForView(Long id, Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);
        Actor actor = getActorById(id);

        if (userInfo.isAdmin() || !userInfo.isUser()) return actor;

        if (!Objects.equals(actor.getCreatedBy(), userInfo.username())) {
            throw new RuntimeException("You dont have permission to view this actor");
        }
        return actor;
    }

    @Transactional(readOnly = true)
    public void applyRoleForMovie(Actor actor, Long movieId) {
        if (actor == null || actor.getId() == null || movieId == null) return;

        movieCastRepository.findByActorIdAndMovieId(actor.getId(), movieId).ifPresent(mc -> {
            actor.setRoleName(mc.getRoleName());
            actor.setSalary(mc.getSalary());
        });
    }

    @Transactional(readOnly = true)
    public List<Actor> getActorsByMovieIdForView(Long movieId, Authentication authentication) {
        List<Actor> actors = actorRepository.findByMoviesId(movieId);

        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);
        if (ui.isUser() && !ui.isAdmin()) {
            actors = actors.stream()
                    .filter(a -> Objects.equals(a.getCreatedBy(), ui.username()))
                    .toList();
        }

        for (Actor a : actors) {
            applyRoleForMovie(a, movieId);
        }
        return actors;
    }

    @Transactional
    public Actor saveActor(Actor actor, Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);

        Set<Movie> movies = new HashSet<>();
        if (actor.getMovieIds() != null && !actor.getMovieIds().isEmpty()) {
            movies.addAll(movieRepository.findAllById(actor.getMovieIds()));
        }
        actor.setMovies(movies);

        actor.setCreatedBy(userInfo.username());
        Actor saved = actorRepository.save(actor);

        syncMovieCast(saved);

        return saved;
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

        actor.setName(updatedActor.getName());

        Set<Movie> movies = new HashSet<>();
        if (updatedActor.getMovieIds() != null && !updatedActor.getMovieIds().isEmpty()) {
            movies.addAll(movieRepository.findAllById(updatedActor.getMovieIds()));
        }
        actor.setMovies(movies);

        actor.setRoleName(updatedActor.getRoleName());
        actor.setSalary(updatedActor.getSalary());

        Actor saved = actorRepository.save(actor);
        syncMovieCast(saved);

        return saved;
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

        movieCastRepository.deleteByActorId(id);

        actorRepository.deleteById(id);
    }

    private void syncMovieCast(Actor actor) {
        if (actor.getId() == null) return;

        List<MovieCast> existing = movieCastRepository.findByActorIdOrderByIdAsc(actor.getId());

        Map<Long, MovieCast> existingByMovieId = existing.stream()
                .filter(mc -> mc.getMovie() != null && mc.getMovie().getId() != null)
                .collect(Collectors.toMap(
                        mc -> mc.getMovie().getId(),
                        mc -> mc,
                        (a, b) -> a
                ));

        Set<Long> desiredMovieIds = actor.getMovies() == null ? Set.of() :
                actor.getMovies().stream()
                        .map(Movie::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        for (MovieCast mc : existing) {
            Long mid = (mc.getMovie() != null ? mc.getMovie().getId() : null);
            if (mid != null && !desiredMovieIds.contains(mid)) {
                movieCastRepository.delete(mc);
            }
        }

        String rn = actor.getRoleName();
        String rnTrim = (rn != null && !rn.isBlank()) ? rn.trim() : null;
        BigDecimal sal = actor.getSalary();

        if (actor.getMovies() == null) return;

        for (Movie m : actor.getMovies()) {
            if (m.getId() == null) continue;

            MovieCast mc = existingByMovieId.get(m.getId());

            if (mc == null) {
                MovieCast created = new MovieCast();
                created.setActor(actor);
                created.setMovie(m);
                created.setCreatedBy(actor.getCreatedBy());
                created.setRoleName(rnTrim != null ? rnTrim : "Unknown");
                created.setSalary(sal);
                movieCastRepository.save(created);
                continue;
            }

            boolean changed = false;

            if (rnTrim != null && !rnTrim.equals(mc.getRoleName())) {
                mc.setRoleName(rnTrim);
                changed = true;
            }

            if (sal != null && !sal.equals(mc.getSalary())) {
                mc.setSalary(sal);
                changed = true;
            }

            if (changed) {
                movieCastRepository.save(mc);
            }
        }
    }
}
