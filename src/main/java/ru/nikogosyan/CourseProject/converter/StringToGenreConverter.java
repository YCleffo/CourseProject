package ru.nikogosyan.CourseProject.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.nikogosyan.CourseProject.entity.Genre;
import ru.nikogosyan.CourseProject.repository.GenreRepository;

@Component
public class StringToGenreConverter implements Converter<String, Genre> {

    private final GenreRepository genreRepository;

    public StringToGenreConverter(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Override
    public Genre convert(String source) {
        if (source == null || source.isEmpty()) return null;
        Long id = Long.valueOf(source);
        return genreRepository.findById(id).orElse(null);
    }
}
