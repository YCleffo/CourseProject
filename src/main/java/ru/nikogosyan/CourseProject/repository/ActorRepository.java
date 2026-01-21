package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikogosyan.CourseProject.entity.Actor;

import java.util.Collection;
import java.util.List;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {
    List<Actor> findByCreatedBy(String createdBy);
    List<Actor> findByMovies_Id(Long movieId);
    List<Actor> findByMovies_IdIn(Collection<Long> movieIds);
}