package ru.nikogosyan.CourseProject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.service.ActorService;
import ru.nikogosyan.CourseProject.service.MovieService;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/actors")
@RequiredArgsConstructor
@Slf4j
public class ActorController {

    private final ActorService actorService;
    private final MovieService movieService;

    @GetMapping
    public String listActors(Authentication authentication, Model model) {
        List<Actor> actors = actorService.getAllActors(authentication);

        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READ_ONLY"));

        boolean canModify = !isReadOnly;

        model.addAttribute("page", "actors");
        model.addAttribute("actors", actors);
        model.addAttribute("canModify", canModify);
        return "actors-list";
    }

    @GetMapping("/new")
    public String newActorForm(Authentication authentication, Model model) {
        checkModifyPermission(authentication);
        model.addAttribute("actor", new Actor());
        model.addAttribute("movies", movieService.getAllMovies(authentication));
        return "actor-form";
    }

    @PostMapping("/new")
    public String createActor(@Valid @ModelAttribute Actor actor,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("movies", movieService.getAllMovies(authentication));
            return "actor-form";
        }

        checkModifyPermission(authentication);
        actorService.saveActor(actor, authentication.getName());
        return "redirect:/actors";
    }

    @GetMapping("/edit/{id}")
    public String editActorForm(@PathVariable Long id,
                                Authentication authentication,
                                Model model) {
        checkModifyPermission(authentication);
        Actor actor = actorService.getActorById(id);
        model.addAttribute("actor", actor);
        model.addAttribute("movies", movieService.getAllMovies(authentication));
        return "actor-form";
    }

    @PostMapping("/edit/{id}")
    public String updateActor(@PathVariable Long id,
                              @Valid @ModelAttribute Actor actor,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("movies", movieService.getAllMovies(authentication));
            return "actor-form";
        }

        checkModifyPermission(authentication);
        actorService.updateActor(id, actor, authentication);
        return "redirect:/actors";
    }

    @PostMapping("/delete/{id}")
    public String deleteActor(@PathVariable Long id, Authentication authentication) {
        checkModifyPermission(authentication);
        actorService.deleteActor(id, authentication);
        return "redirect:/actors";
    }

    private void checkModifyPermission(Authentication authentication) {
        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READ_ONLY"));

        if (isReadOnly) {
            throw new RuntimeException("READ_ONLY users cannot modify data");
        }
    }
}