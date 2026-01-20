package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.repository.ActorRepository;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorService {

    private final ActorRepository actorRepository;

    @Transactional(readOnly = true)
    public List<Actor> getAllActors(Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        boolean isUser = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_USER"));

        log.info("Getting actors for user: {}, isAdmin: {}, isUser: {}", username, isAdmin, isUser);

        return actorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Actor getActorById(Long id) {
        return actorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actor not found"));
    }

    @Transactional(readOnly = true)
    public List<Actor> getActorsByMovieId(Long movieId) {
        return actorRepository.findByMovieId(movieId);
    }

    @Transactional
    public Actor saveActor(Actor actor, String username) {
        log.info("Saving actor: {} by user: {}", actor.getName(), username);
        actor.setCreatedBy(username);
        return actorRepository.save(actor);
    }

    @Transactional
    public void deleteActor(Long id, Authentication authentication) {
        Actor actor = getActorById(id);
        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isAdmin && !actor.getCreatedBy().equals(username)) {
            throw new RuntimeException("You don't have permission to delete this actor");
        }

        log.info("Deleting actor: {} by user: {}", id, username);
        actorRepository.deleteById(id);
    }

    @Transactional
    public Actor updateActor(Long id, Actor updatedActor, Authentication authentication) {
        Actor actor = getActorById(id);
        String username = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isAdmin && !actor.getCreatedBy().equals(username)) {
            throw new RuntimeException("You don't have permission to update this actor");
        }

        log.info("Updating actor: {} by user: {}", id, username);

        actor.setName(updatedActor.getName());
        actor.setRoleName(updatedActor.getRoleName());
        actor.setSalary(updatedActor.getSalary());
        actor.setMovie(updatedActor.getMovie());

        return actorRepository.save(actor);
    }
}