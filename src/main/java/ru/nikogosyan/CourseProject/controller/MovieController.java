package ru.nikogosyan.CourseProject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.service.MovieService;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public String listMovies(Authentication authentication, Model model) {
        List<Movie> movies = movieService.getAllMovies(authentication);

        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READ_ONLY"));

        boolean canModify = !isReadOnly;

        model.addAttribute("page", "movies");
        model.addAttribute("movies", movies);
        model.addAttribute("canModify", canModify);

        return "movies-list";
    }

    @GetMapping("/new")
    public String newMovieForm(Authentication authentication, Model model) {
        checkModifyPermission(authentication);
        model.addAttribute("movie", new Movie());
        return "movie-form";
    }

    @PostMapping("/new")
    public String createMovie(@Valid @ModelAttribute Movie movie,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            return "movie-form";
        }

        checkModifyPermission(authentication);
        movieService.saveMovie(movie, authentication.getName());
        return "redirect:/movies";
    }

    @GetMapping("/edit/{id}")
    public String editMovieForm(@PathVariable Long id,
                                Authentication authentication,
                                Model model) {
        checkModifyPermission(authentication);
        Movie movie = movieService.getMovieById(id);
        model.addAttribute("movie", movie);
        return "movie-form";
    }

    @PostMapping("/edit/{id}")
    public String updateMovie(@PathVariable Long id,
                              @Valid @ModelAttribute Movie movie,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            return "movie-form";
        }

        Movie existingMovie = movieService.getMovieById(id);
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isAdmin && !existingMovie.getCreatedBy().equals(authentication.getName())) {
            model.addAttribute("error", "You can only edit your own movies");
            return "redirect:/movies";
        }

        movieService.updateMovie(id, movie, authentication);
        return "redirect:/movies";
    }

    @PostMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id, Authentication authentication) {
        checkModifyPermission(authentication);
        movieService.deleteMovie(id, authentication);
        return "redirect:/movies";
    }

    private void checkModifyPermission(Authentication authentication) {
        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READ_ONLY"));

        if (isReadOnly) {
            throw new RuntimeException("READ_ONLY users cannot modify data");
        }
    }
}