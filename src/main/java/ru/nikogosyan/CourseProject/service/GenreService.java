package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nikogosyan.CourseProject.entity.Genre;
import ru.nikogosyan.CourseProject.repository.GenreRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }
}
