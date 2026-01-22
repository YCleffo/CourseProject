package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikogosyan.CourseProject.entity.CalculationLog;

import java.util.List;

@Repository
public interface CalculationLogRepository extends JpaRepository<CalculationLog, Long> {
    void deleteByMovieId(Long movieId);
    void deleteByMovieIdAndCreatedBy(Long movieId, String createdBy);

    List<CalculationLog> findByMovieIdOrderByCreatedAtDesc(Long movieId);
    List<CalculationLog> findByMovieIdAndCreatedByOrderByCreatedAtDesc(Long movieId, String createdBy);
}
