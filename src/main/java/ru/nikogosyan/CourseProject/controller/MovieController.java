package ru.nikogosyan.CourseProject.controller;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.nikogosyan.CourseProject.entity.Genre;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.service.ActorService;
import ru.nikogosyan.CourseProject.service.GenreService;
import ru.nikogosyan.CourseProject.service.MovieService;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieService movieService;
    private final GenreService genreService;
    private final ActorService actorService;
    private final SecurityUtils securityUtils;

    private static final Path UPLOAD_DIR = Paths.get("uploads/images");

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(UPLOAD_DIR)) {
                Files.createDirectories(UPLOAD_DIR);
                log.info("Created upload directory {}", UPLOAD_DIR.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", e.getMessage(), e);
        }
    }

    private static final Map<String, String> GENRE_TRANSLATIONS = Map.ofEntries(
            Map.entry("Action", "Боевик"),
            Map.entry("Adventure", "Приключения"),
            Map.entry("Comedy", "Комедия"),
            Map.entry("Drama", "Драма"),
            Map.entry("Fantasy", "Фэнтези"),
            Map.entry("Horror", "Ужасы"),
            Map.entry("Romance", "Романтика"),
            Map.entry("Sci-Fi", "Научная фантастика"),
            Map.entry("Thriller", "Триллер"),
            Map.entry("Crime", "Криминал"),
            Map.entry("Mystery", "Детектив"),
            Map.entry("Biography", "Биография"),
            Map.entry("History", "История"),
            Map.entry("War", "Военный"),
            Map.entry("Western", "Вестерн"),
            Map.entry("Documentary", "Документальный"),
            Map.entry("Animation", "Анимация"),
            Map.entry("Family", "Семейный"),
            Map.entry("Musical", "Мюзикл"),
            Map.entry("Sport", "Спорт"),
            Map.entry("Superhero", "Супергеройский"),
            Map.entry("Anime", "Аниме")
    );

    @GetMapping
    public String listMovies(Authentication authentication, Model model) {
        List<Movie> movies = movieService.getAllMovies(authentication);

        boolean isReadOnly = securityUtils.isReadOnly(authentication);

        model.addAttribute("page", "movies");
        model.addAttribute("movies", movies);
        model.addAttribute("canModify", !isReadOnly);
        model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);

        for (Movie movie : movies) {
            String genresString = movie.getGenres().stream()
                    .map(Genre::getName)
                    .map(genreName -> GENRE_TRANSLATIONS.getOrDefault(genreName, genreName))
                    .collect(Collectors.joining(", "));
            movie.setGenresString(genresString);
        }

        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        model.addAttribute("firstActorByMovieId", actorService.getFirstActorsByMovieIds(movieIds));

        return "movies-list";
    }

    @GetMapping("/{id}")
    public String movieDetails(@PathVariable Long id,
                               @RequestParam(required = false, defaultValue = "movies") String from,
                               Authentication authentication,
                               Model model) {

        Movie movie = movieService.getMovieForView(id, authentication);

        boolean isReadOnly = securityUtils.isReadOnly(authentication);

        model.addAttribute("page", "movies");
        model.addAttribute("movie", movie);
        model.addAttribute("actors", actorService.getActorsByMovieIdForView(id, authentication));
        model.addAttribute("canModify", !isReadOnly);
        model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
        model.addAttribute("from", from);

        return "movie-details";
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

        if (result.hasErrors()) {
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        }

        try {
            checkModifyPermission(authentication);

            if (imageFile != null && !imageFile.isEmpty()) {
                if (imageFile.getSize() > 50L * 1024 * 1024) {
                    model.addAttribute("error", "File too large. Maximum size is 50MB.");
                    model.addAttribute("genres", genreService.getAllGenres());
                    model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
                    return "movie-form";
                }

                String contentType = imageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image")) {
                    model.addAttribute("error", "Only image files are allowed.");
                    model.addAttribute("genres", genreService.getAllGenres());
                    model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
                    return "movie-form";
                }

                handleImageUpload(movie, imageFile, null);
            }

            if (movie.getGenreIds() != null && !movie.getGenreIds().isEmpty()) {
                movie.setGenres(genreService.getGenresByIds(movie.getGenreIds()));
            }

            movieService.saveMovie(movie, authentication.getName());
            return "redirect:/movies";

        } catch (IOException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("genres", genreService.getAllGenres());
            model.addAttribute("genreTranslations", GENRE_TRANSLATIONS);
            return "movie-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editMovieForm(@PathVariable Long id, Authentication authentication, Model model) {
        checkModifyPermission(authentication);

        Movie movie = movieService.getMovieById(id);

        Set<Long> ids = movie.getGenres().stream().map(Genre::getId).collect(Collectors.toSet());
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

            boolean isAdmin = securityUtils.isAdmin(authentication);
            if (!isAdmin && !Objects.equals(existingMovie.getCreatedBy(), authentication.getName())) {
                model.addAttribute("error", "You don't have permission to update this movie.");
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
            model.addAttribute("error", e.getMessage());
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
            Path filePath = Paths.get("..").resolve(movie.getImagePath().substring(1));
            Files.deleteIfExists(filePath);
        }

        movieService.deleteMovie(id, authentication);
        return "redirect:/movies";
    }

    private void checkModifyPermission(Authentication authentication) {
        if (securityUtils.isReadOnly(authentication)) {
            throw new RuntimeException("READ_ONLY users cannot modify data");
        }
    }

    private void handleImageUpload(Movie movie, MultipartFile imageFile, String existingPath) throws IOException {
        if (!Files.exists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String extension = getExtension(imageFile);
            String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + extension;

            Path filePath = UPLOAD_DIR.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            movie.setImagePath("/uploads/images/" + fileName);

            if (existingPath != null && !existingPath.isEmpty()) {
                try {
                    Path oldFile = Paths.get("..").resolve(existingPath.substring(1));
                    Files.deleteIfExists(oldFile);
                } catch (IOException e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }
        } else if (existingPath != null) {
            movie.setImagePath(existingPath);
        }
    }

    private static @NonNull String getExtension(MultipartFile imageFile) throws IOException {
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image")) {
            throw new IOException("Only image files are allowed.");
        }

        if (imageFile.getSize() > 10L * 1024 * 1024) {
            throw new IOException("File too large. Maximum size is 10MB.");
        }

        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("Invalid file name.");
        }

        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
