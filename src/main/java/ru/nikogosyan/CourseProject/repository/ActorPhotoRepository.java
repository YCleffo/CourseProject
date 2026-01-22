package ru.nikogosyan.CourseProject.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikogosyan.CourseProject.entity.ActorPhoto;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActorPhotoRepository extends JpaRepository<ActorPhoto, Long> {

    List<ActorPhoto> findByActorIdOrderByIsPrimaryDescCreatedAtAscIdAsc(Long actorId);

    Optional<ActorPhoto> findFirstByActorIdOrderByIsPrimaryDescCreatedAtAscIdAsc(Long actorId);

    Optional<ActorPhoto> findByIdAndActorId(Long id, Long actorId);

    boolean existsByActorIdAndIsPrimaryTrue(Long actorId);

    @Modifying
    @Query("update ActorPhoto p set p.isPrimary=false where p.actor.id=:actorId")
    void clearPrimary(@Param("actorId") Long actorId);

    @Query("""
        select p from ActorPhoto p
        where p.actor.id in :actorIds
        order by p.actor.id, p.isPrimary desc, p.createdAt asc, p.id asc
    """)

    List<ActorPhoto> findForActorsOrdered(@Param("actorIds") List<Long> actorIds);
}
