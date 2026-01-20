package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nikogosyan.CourseProject.entity.Genre;
import ru.nikogosyan.CourseProject.repository.GenreRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }

    public Set<Genre> getGenresByIds(Set<Long> ids) {
        return new HashSet<>(genreRepository.findAllById(ids));
    }
}
