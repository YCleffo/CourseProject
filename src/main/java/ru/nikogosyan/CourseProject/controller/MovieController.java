package ru.nikogosyan.CourseProject.controller;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.nikogosyan.CourseProject.entity.Genre;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.service.GenreService;
import ru.nikogosyan.CourseProject.service.MovieService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieService movieService;
    private final GenreService genreService;

    private static final Path UPLOAD_DIR = Paths.get("uploads/images");

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(UPLOAD_DIR)) {
                Files.createDirectories(UPLOAD_DIR);
                log.info("Created upload directory: {}", UPLOAD_DIR.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory", e);
        }
    }

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

        for (Movie movie : movies) {
            String genresString = movie.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.joining(", "));
            model.addAttribute("genresString_" + movie.getId(), genresString);
        }

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
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              Authentication authentication,
                              Model model) {

        log.info("Creating movie: {}", movie.getTitle());
        log.info("Image file present: {}, size: {}",
                imageFile != null && !imageFile.isEmpty(),
                imageFile != null ? imageFile.getSize() : 0);

        if (result.hasErrors()) {
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        }

        try {
            checkModifyPermission(authentication);

            if (imageFile != null && !imageFile.isEmpty()) {
                // Проверка размера файла
                if (imageFile.getSize() > 50 * 1024 * 1024) { // 50 MB
                    model.addAttribute("error", "Размер файла не должен превышать 50MB");
                    model.addAttribute("genres", genreService.getAllGenres());
                    model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
                    return "movie-form";
                }

                // Проверка типа файла
                String contentType = imageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    model.addAttribute("error", "Файл должен быть изображением");
                    model.addAttribute("genres", genreService.getAllGenres());
                    model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
                    return "movie-form";
                }

                handleImageUpload(movie, imageFile);
                log.info("Image uploaded successfully: {}", movie.getImagePath());
            }

            if (movie.getGenreIds() != null && !movie.getGenreIds().isEmpty()) {
                movie.setGenres(genreService.getGenresByIds(movie.getGenreIds()));
            }

            movieService.saveMovie(movie, authentication.getName());
            log.info("Movie saved successfully: {}", movie.getId());
            return "redirect:/movies";

        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке изображения: " + e.getMessage());
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        } catch (Exception e) {
            log.error("Error creating movie: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка при создании фильма: " + e.getMessage());
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editMovieForm(@PathVariable Long id,
                                Authentication authentication,
                                Model model) {
        checkModifyPermission(authentication);
        Movie movie = movieService.getMovieById(id);

        Set<Long> ids = movie.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        movie.setGenreIds(ids);

        model.addAttribute("movie", movie);
        model.addAttribute("genres", genreService.getAllGenres());
        model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
        return "movie-form";
    }

    @PostMapping("/edit/{id}")
    public String updateMovie(@PathVariable Long id,
                              @Valid @ModelAttribute Movie movie,
                              BindingResult result,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              Authentication authentication,
                              Model model) {

        if (result.hasErrors()) {
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        }

        try {
            Movie existingMovie = movieService.getMovieById(id);

            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(Objects::nonNull)
                    .anyMatch(role -> role.equals("ROLE_ADMIN"));

            if (!isAdmin && !existingMovie.getCreatedBy().equals(authentication.getName())) {
                model.addAttribute("error", "У вас нет прав на редактирование этого фильма");
                return "redirect:/movies";
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                handleImageUpload(movie, imageFile, existingMovie.getImagePath());
            } else {
                movie.setImagePath(existingMovie.getImagePath());
            }

            if (movie.getGenreIds() != null && !movie.getGenreIds().isEmpty()) {
                movie.setGenres(genreService.getGenresByIds(movie.getGenreIds()));
            }

            movieService.updateMovie(id, movie, authentication);
            return "redirect:/movies";

        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке изображения: " + e.getMessage());
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        }
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

    private void handleImageUpload(Movie movie, MultipartFile imageFile, String existingPath) throws IOException {
        if (!Files.exists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String extension = getString(imageFile);
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

            Path filePath = UPLOAD_DIR.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            movie.setImagePath("/uploads/images/" + fileName);

            if (existingPath != null && !existingPath.isEmpty()) {
                try {
                    Path oldFile = Paths.get(".").resolve(existingPath.substring(1));
                    Files.deleteIfExists(oldFile);
                } catch (IOException e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }
        } else if (existingPath != null) {
            movie.setImagePath(existingPath);
        }
    }

    private static @NonNull String getString(MultipartFile imageFile) throws IOException {
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Файл должен быть изображением");
        }

        if (imageFile.getSize() > 10 * 1024 * 1024) {
            throw new IOException("Размер файла не должен превышать 10MB");
        }

        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("Некорректное имя файла");
        }

        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private void handleImageUpload(Movie movie, MultipartFile imageFile) throws IOException {
        handleImageUpload(movie, imageFile, null);
    }

    private void checkModifyPermission(Authentication authentication) {
        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READ_ONLY"));

        if (isReadOnly) throw new RuntimeException("READ_ONLY users cannot modify data");
    }
}
