package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nikogosyan.CourseProject.entity.Genre;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}
