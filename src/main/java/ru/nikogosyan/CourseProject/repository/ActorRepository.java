package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikogosyan.CourseProject.entity.Actor;

import java.util.Collection;
import java.util.List;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {

    List<Actor> findByCreatedBy(String createdBy);

    // было: findByMovieId(Long movieId)
    List<Actor> findByMovie_Id(Long movieId);

    // было: findByMovieIdIn(Collection<Long> movieIds)
    List<Actor> findByMovie_IdIn(Collection<Long> movieIds);
}
