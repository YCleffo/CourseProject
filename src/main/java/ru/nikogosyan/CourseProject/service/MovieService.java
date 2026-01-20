package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.repository.MovieRepository;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {
    private final MovieRepository movieRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<Movie> getAllMovies(Authentication authentication) {
        SecurityUtils.UserInfo userInfo = securityUtils.getUserInfo(authentication);
        log.info("Getting movies for user: {}, isAdmin: {}, isUser: {}",
                userInfo.username(), userInfo.isAdmin(), userInfo.isUser());

        return movieRepository.findAllWithGenres();
    }

    @Transactional(readOnly = true)
    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
    }

    @Transactional
    public Movie saveMovie(Movie movie, String username) {
        log.info("Saving movie: {} by user: {}", movie.getTitle(), username);
        movie.setCreatedBy(username);
        return movieRepository.save(movie);
    }

    @Transactional
    public void deleteMovie(Long id, Authentication authentication) {
        Movie movie = getMovieById(id);
        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isAdmin && !movie.getCreatedBy().equals(username)) {
            throw new RuntimeException("You don't have permission to delete this movie");
        }

        log.info("Deleting movie: {} by user: {}", id, username);
        movieRepository.deleteById(id);
    }

    @Transactional
    public Movie updateMovie(Long id, Movie updatedMovie, Authentication authentication) {
        Movie movie = getMovieById(id);
        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isAdmin && !movie.getCreatedBy().equals(username)) {
            throw new RuntimeException("You don't have permission to update this movie");
        }

        log.info("Updating movie: {} by user: {}", id, username);

        movie.setTitle(updatedMovie.getTitle());
        movie.setGenres(updatedMovie.getGenres());
        movie.setReleaseYear(updatedMovie.getReleaseYear());
        movie.setBoxOffice(updatedMovie.getBoxOffice());

        return movieRepository.save(movie);
    }
}