package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikogosyan.CourseProject.entity.Movie;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @EntityGraph(value = "Movie.genres", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT DISTINCT m FROM Movie m")
    List<Movie> findAllWithGenres();

    @EntityGraph(value = "Movie.genres", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT DISTINCT m FROM Movie m WHERE m.createdBy = :createdBy")
    List<Movie> findByCreatedByWithGenres(@Param("createdBy") String createdBy);

    @EntityGraph(value = "Movie.genres", type = EntityGraph.EntityGraphType.FETCH)
    Optional<Movie> findByIdAndCreatedBy(Long id, String createdBy);
}
