package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.nikogosyan.CourseProject.entity.Movie;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    @EntityGraph(value = "Movie.genres", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT DISTINCT m FROM Movie m")
    List<Movie> findAllWithGenres();
}