package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.entity.MovieCast;
import ru.nikogosyan.CourseProject.repository.MovieCastRepository;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieCastService {

    private final MovieCastRepository movieCastRepository;
    private final MovieService movieService;
    private final ActorService actorService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<MovieCast> getCastByMovieIdForView(Long movieId, Authentication authentication) {
        movieService.getMovieForView(movieId, authentication);
        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);

        List<MovieCast> cast = movieCastRepository.findByMovieIdOrderByIdAsc(movieId);
        if (ui.isAdmin() || !ui.isUser()) return cast;

        return cast.stream()
                .filter(mc -> mc.getActor() != null && Objects.equals(mc.getActor().getCreatedBy(), ui.username()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovieCast> getCastByActorIdForView(Long actorId, Authentication authentication) {
        actorService.getActorForView(actorId, authentication);
        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);

        List<MovieCast> castings = movieCastRepository.findForActorWithMoviesOrdered(actorId);
        if (ui.isAdmin() || !ui.isUser()) return castings;

        return castings.stream()
                .filter(mc -> mc.getMovie() != null && Objects.equals(mc.getMovie().getCreatedBy(), ui.username()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, MovieCast> getFirstCastByMovieIds(List<Long> movieIds, Authentication authentication) {
        if (movieIds == null || movieIds.isEmpty()) return Map.of();

        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);
        List<MovieCast> all = movieCastRepository.findForMoviesWithActorsOrdered(movieIds);

        if (ui.isUser() && !ui.isAdmin()) {
            all = all.stream()
                    .filter(mc -> mc.getActor() != null && Objects.equals(mc.getActor().getCreatedBy(), ui.username()))
                    .toList();
        }

        Map<Long, MovieCast> result = new HashMap<>();
        for (MovieCast mc : all) {
            if (mc.getMovie() == null || mc.getMovie().getId() == null) continue;
            result.putIfAbsent(mc.getMovie().getId(), mc);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getMovieCountByActorIds(List<Long> actorIds, Authentication authentication) {
        if (actorIds == null || actorIds.isEmpty()) return Map.of();
        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);

        Map<Long, Long> count = new HashMap<>();
        for (Long actorId : actorIds) {
            List<MovieCast> items = movieCastRepository.findByActorIdOrderByIdAsc(actorId);
            if (ui.isUser() && !ui.isAdmin()) {
                items = items.stream()
                        .filter(mc -> mc.getMovie() != null && Objects.equals(mc.getMovie().getCreatedBy(), ui.username()))
                        .toList();
            }
            long c = items.stream()
                    .map(mc -> mc.getMovie() != null ? mc.getMovie().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            count.put(actorId, c);
        }
        return count;
    }

    @Transactional
    public MovieCast addCast(Long movieId, Long actorId, String roleName, BigDecimal salary, Authentication authentication) {
        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);
        if (ui.isReadOnly()) throw new RuntimeException("READONLY users cannot modify data");

        Movie movie = movieService.getMovieById(movieId);
        checkCanModifyMovie(movie, authentication);

        Actor actor = actorService.getActorById(actorId);
        if (!ui.isAdmin() && ui.isUser() && actor.getCreatedBy() != null && !actor.getCreatedBy().equals(ui.username())) {
            throw new RuntimeException("You dont have permission to use this actor");
        }

        if (roleName == null || roleName.isBlank()) {
            throw new RuntimeException("Role name is required");
        }

        MovieCast mc = new MovieCast();
        mc.setMovie(movie);
        mc.setActor(actor);
        mc.setRoleName(roleName.trim());
        mc.setSalary(salary);
        mc.setCreatedBy(ui.username());

        return movieCastRepository.save(mc);
    }

    @Transactional
    public void deleteCast(Long movieId, Long castId, Authentication authentication) {
        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);
        if (ui.isReadOnly()) throw new RuntimeException("READONLY users cannot modify data");

        Movie movie = movieService.getMovieById(movieId);
        checkCanModifyMovie(movie, authentication);

        MovieCast mc = movieCastRepository.findByIdAndMovieId(castId, movieId)
                .orElseThrow(() -> new RuntimeException("Cast not found"));

        movieCastRepository.delete(mc);
    }

    private void checkCanModifyMovie(Movie movie, Authentication authentication) {
        boolean isAdmin = securityUtils.isAdmin(authentication);
        String username = authentication.getName();
        if (!isAdmin && movie.getCreatedBy() != null && !movie.getCreatedBy().equals(username)) {
            throw new RuntimeException("You dont have permission to update this movie.");
        }
    }
}
