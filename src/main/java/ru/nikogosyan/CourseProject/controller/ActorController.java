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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.service.ActorPhotoService;
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
    private final ActorPhotoService actorPhotoService;

    @GetMapping("{id}")
    public String actorDetails(@PathVariable Long id,
                               @RequestParam(value = "from", defaultValue = "actors") String from,
                               @RequestParam(value = "movieId", required = false) Long movieId,
                               Authentication authentication,
                               Model model) {

        Actor actor = actorService.getActorById(id);

        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READONLY") || role.equals("ROLE_READ_ONLY"));

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLEADMIN"));

        boolean canModify = !isReadOnly && (isAdmin || (actor.getCreatedBy() != null && actor.getCreatedBy().equals(authentication.getName())));

        model.addAttribute("page", "actors");
        model.addAttribute("actor", actor);
        model.addAttribute("photos", actorPhotoService.getPhotos(id));
        model.addAttribute("primaryPhotoPath", actorPhotoService.getPrimaryOrFirstPhotoPath(id));
        model.addAttribute("canModify", canModify);
        model.addAttribute("from", from);
        model.addAttribute("movieId", movieId);

        return "actor-details";
    }

    @PostMapping("{id}/photos")
    public String uploadActorPhoto(@PathVariable Long id,
                                   @RequestParam("imageFile") MultipartFile imageFile,
                                   @RequestParam(value = "from", defaultValue = "actors") String from,
                                   @RequestParam(value = "movieId", required = false) Long movieId,
                                   Authentication authentication,
                                   RedirectAttributes ra) {
        try {
            actorPhotoService.upload(id, imageFile, authentication);
            ra.addFlashAttribute("message", "Фото загружено");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actors/" + id + "?from=" + from + (movieId != null ? "&movieId=" + movieId : "");
    }

    @PostMapping("{id}/photos/{photoId}/primary")
    public String setPrimary(@PathVariable Long id,
                             @PathVariable Long photoId,
                             @RequestParam(value = "from", defaultValue = "actors") String from,
                             @RequestParam(value = "movieId", required = false) Long movieId,
                             Authentication authentication,
                             RedirectAttributes ra) {
        try {
            actorPhotoService.setPrimary(id, photoId, authentication);
            ra.addFlashAttribute("message", "Главное фото обновлено");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actors/" + id + "?from=" + from + (movieId != null ? "&movieId=" + movieId : "");
    }

    @PostMapping("{id}/photos/{photoId}/delete")
    public String deletePhoto(@PathVariable Long id,
                              @PathVariable Long photoId,
                              @RequestParam(value = "from", defaultValue = "actors") String from,
                              @RequestParam(value = "movieId", required = false) Long movieId,
                              Authentication authentication,
                              RedirectAttributes ra) {
        try {
            actorPhotoService.delete(id, photoId, authentication);
            ra.addFlashAttribute("message", "Фото удалено");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actors/" + id + "?from=" + from + (movieId != null ? "&movieId=" + movieId : "");
    }

    @GetMapping
    public String listActors(Authentication authentication, Model model) {
        List<Actor> actors = actorService.getAllActors(authentication);

        boolean isReadOnly = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals("ROLE_READONLY"));

        boolean canModify = !isReadOnly;

        List<Long> actorIds = actors.stream().map(Actor::getId).toList();
        model.addAttribute("primaryPhotoByActorId", actorPhotoService.getPrimaryOrFirstPhotoPaths(actorIds));

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