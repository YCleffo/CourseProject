package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikogosyan.CourseProject.entity.MovieCast;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieCastRepository extends JpaRepository<MovieCast, Long> {

    @EntityGraph(attributePaths = {"actor"})
    List<MovieCast> findByMovieIdOrderByIdAsc(Long movieId);

    @EntityGraph(attributePaths = {"movie"})
    List<MovieCast> findByActorIdOrderByIdAsc(Long actorId);

    Optional<MovieCast> findByIdAndMovieId(Long id, Long movieId);

    Optional<MovieCast> findByActorIdAndMovieId(Long actorId, Long movieId);

    void deleteByActorId(Long actorId);

    @Query("""
           select mc
           from MovieCast mc
           join fetch mc.movie m
           where mc.actor.id = :actorId
           order by m.id asc, mc.id asc
           """)
    List<MovieCast> findForActorWithMoviesOrdered(@Param("actorId") Long actorId);

    @Query("""
           select mc
           from MovieCast mc
           join fetch mc.movie m
           join fetch mc.actor a
           where m.id in :movieIds
           order by m.id asc, mc.id asc
           """)
    List<MovieCast> findForMoviesWithActorsOrdered(@Param("movieIds") List<Long> movieIds);
}
