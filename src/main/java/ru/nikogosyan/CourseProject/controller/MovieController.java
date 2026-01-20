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
import org.springframework.web.multipart.MultipartFile;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.service.GenreService;
import ru.nikogosyan.CourseProject.service.MovieService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieService movieService;
    private final GenreService genreService;

    private static final Path UPLOAD_DIR = Paths.get("uploads/images");

    private static final Map<String, String> GENRE_TRANSLATIONS = Map.ofEntries(
            Map.entry("Action", "Боевик"),
            Map.entry("Adventure", "Приключения"),
            Map.entry("Comedy", "Комедия"),
            Map.entry("Drama", "Драма"),
            Map.entry("Fantasy", "Фэнтези"),
            Map.entry("Horror", "Ужасы"),
            Map.entry("Romance", "Мелодрама"),
            Map.entry("Sci-Fi", "Научная фантастика"),
            Map.entry("Thriller", "Триллер"),
            Map.entry("Crime", "Криминал"),
            Map.entry("Mystery", "Детектив"),
            Map.entry("Biography", "Биография"),
            Map.entry("History", "Исторический"),
            Map.entry("War", "Военный"),
            Map.entry("Western", "Вестерн"),
            Map.entry("Documentary", "Документальный"),
            Map.entry("Animation", "Анимация"),
            Map.entry("Family", "Семейный"),
            Map.entry("Musical", "Мюзикл"),
            Map.entry("Sport", "Спортивный"),
            Map.entry("Superhero", "Супергерои"),
            Map.entry("Anime", "Аниме")
    );

    @GetMapping
    public String listMovies(Authentication authentication, Model model) {
        List<Movie> movies = movieService.getAllMovies(authentication);

        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READ_ONLY"));

        model.addAttribute("page", "movies");
        model.addAttribute("movies", movies);
        model.addAttribute("canModify", !isReadOnly);

        return "movies-list";
    }

    @GetMapping("/new")
    public String newMovieForm(Authentication authentication, Model model) {
        checkModifyPermission(authentication);
        model.addAttribute("movie", new Movie());
        model.addAttribute("genres", genreService.getAllGenres());
        model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
        return "movie-form";
    }

    @PostMapping("/new")
    public String createMovie(@Valid @ModelAttribute Movie movie,
                              BindingResult result,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              Authentication authentication) throws IOException {

        if (result.hasErrors()) return "movie-form";

        checkModifyPermission(authentication);
        handleImageUpload(movie, imageFile);

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
        model.addAttribute("genres", genreService.getAllGenres());
        model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
        return "movie-form";
    }

    @PostMapping("/edit/{id}")
    public String updateMovie(@PathVariable Long id,
                              @Valid @ModelAttribute Movie movie,
                              BindingResult result,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              Authentication authentication) throws IOException {

        if (result.hasErrors()) return "movie-form";

        Movie existingMovie = movieService.getMovieById(id);

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isAdmin && !existingMovie.getCreatedBy().equals(authentication.getName())) {
            return "redirect:/movies";
        }

        handleImageUpload(movie, imageFile, existingMovie.getImagePath());

        movieService.updateMovie(id, movie, authentication);
        return "redirect:/movies";
    }

    @PostMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id, Authentication authentication) throws IOException {
        checkModifyPermission(authentication);

        Movie movie = movieService.getMovieById(id);

        if (movie.getImagePath() != null) {
            Path filePath = Paths.get(".").resolve(movie.getImagePath().substring(1));
            Files.deleteIfExists(filePath);
        }

        movieService.deleteMovie(id, authentication);
        return "redirect:/movies";
    }

    private void handleImageUpload(Movie movie, MultipartFile imageFile) throws IOException {
        handleImageUpload(movie, imageFile, null);
    }

    private void handleImageUpload(Movie movie, MultipartFile imageFile, String existingPath) throws IOException {
        if (!Files.exists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            Files.copy(imageFile.getInputStream(), UPLOAD_DIR.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            movie.setImagePath("/uploads/images/" + fileName);
        } else if (existingPath != null) {
            movie.setImagePath(existingPath);
        }
    }

    private void checkModifyPermission(Authentication authentication) {
        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READ_ONLY"));

        if (isReadOnly) throw new RuntimeException("READ_ONLY users cannot modify data");
    }
}
