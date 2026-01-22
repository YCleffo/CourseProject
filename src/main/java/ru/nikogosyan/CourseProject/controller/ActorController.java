package ru.nikogosyan.CourseProject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.util.List;

@Controller
@RequestMapping("/actors")
@RequiredArgsConstructor
@Slf4j
public class ActorController {

    private final ActorService actorService;
    private final MovieService movieService;
    private final ActorPhotoService actorPhotoService;
    private final SecurityUtils securityUtils;

    @GetMapping("/{id}")
    public String actorDetails(
            @PathVariable Long id,
            @RequestParam(value = "from", defaultValue = "actors") String from,
            @RequestParam(value = "movieId", required = false) Long movieId,
            Authentication authentication,
            Model model
    ) {
        Actor actor = actorService.getActorForView(id, authentication);

        if (movieId != null) {
            actorService.applyRoleForMovie(actor, movieId);
        }

        boolean isReadOnly = securityUtils.isReadOnly(authentication);
        boolean canModify = !isReadOnly;

        model.addAttribute("page", "actors");
        model.addAttribute("actor", actor);
        model.addAttribute("photos", actorPhotoService.getPhotos(id));
        model.addAttribute("primaryPhotoPath", actorPhotoService.getPrimaryOrFirstPhotoPath(id));
        model.addAttribute("canModify", canModify);
        model.addAttribute("from", from);
        model.addAttribute("movieId", movieId);

        return "actor-details";
    }

    @GetMapping
    public String listActors(Authentication authentication, Model model) {
        List<Actor> actors = actorService.getAllActors(authentication);
        boolean isReadOnly = securityUtils.isReadOnly(authentication);
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
    public String createActor(
            @Valid @ModelAttribute("actor") Actor actor,
            BindingResult result,
            Authentication authentication,
            Model model
    ) {
        if (result.hasErrors()) {
            model.addAttribute("movies", movieService.getAllMovies(authentication));
            return "actor-form";
        }
        checkModifyPermission(authentication);
        Actor saved = actorService.saveActor(actor, authentication);
        return "redirect:/actors/" + saved.getId();
    }

    @GetMapping("/edit/{id}")
    public String editActorForm(@PathVariable Long id, Authentication authentication, Model model) {
        checkModifyPermission(authentication);
        Actor actor = actorService.getActorForView(id, authentication);

        model.addAttribute("actor", actor);
        model.addAttribute("movies", movieService.getAllMovies(authentication));
        model.addAttribute("photos", actorPhotoService.getPhotos(id));
        model.addAttribute("primaryPhotoPath", actorPhotoService.getPrimaryOrFirstPhotoPath(id));

        return "actor-form";
    }

    @PostMapping("/edit/{id}")
    public String updateActor(
            @PathVariable Long id,
            @Valid @ModelAttribute("actor") Actor actor,
            BindingResult result,
            Authentication authentication,
            Model model
    ) {
        if (result.hasErrors()) {
            actor.setId(id);
            model.addAttribute("movies", movieService.getAllMovies(authentication));
            return "actor-form";
        }
        checkModifyPermission(authentication);
        Actor saved = actorService.updateActor(id, actor, authentication);
        return "redirect:/actors/" + saved.getId();
    }

    @PostMapping("/delete/{id}")
    public String deleteActor(@PathVariable Long id, Authentication authentication) {
        checkModifyPermission(authentication);
        actorService.deleteActor(id, authentication);
        return "redirect:/actors";
    }

    @PostMapping("/{id}/photos")
    public String uploadActorPhoto(
            @PathVariable Long id,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "from", defaultValue = "actors") String from,
            @RequestParam(value = "movieId", required = false) Long movieId,
            Authentication authentication,
            RedirectAttributes ra
    ) {
        try {
            actorPhotoService.upload(id, imageFile, authentication);
            ra.addFlashAttribute("message", "Фотография успешно загружена!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        if ("edit".equals(from)) return "redirect:/actors/edit/" + id;
        return "redirect:/actors/" + id + "?from=" + from + (movieId != null ? "&movieId=" + movieId : "");
    }

    @PostMapping("/{id}/photos/{photoId}/primary")
    public String setPrimary(
            @PathVariable Long id,
            @PathVariable Long photoId,
            @RequestParam(value = "from", defaultValue = "actors") String from,
            @RequestParam(value = "movieId", required = false) Long movieId,
            Authentication authentication,
            RedirectAttributes ra
    ) {
        try {
            actorPhotoService.setPrimary(id, photoId, authentication);
            ra.addFlashAttribute("message", "Обновлена основная фотография!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        if ("edit".equals(from)) return "redirect:/actors/edit/" + id;
        return "redirect:/actors/" + id + "?from=" + from + (movieId != null ? "&movieId=" + movieId : "");
    }

    @PostMapping("/{id}/photos/{photoId}/delete")
    public String deletePhoto(
            @PathVariable Long id,
            @PathVariable Long photoId,
            @RequestParam(value = "from", defaultValue = "actors") String from,
            @RequestParam(value = "movieId", required = false) Long movieId,
            Authentication authentication,
            RedirectAttributes ra
    ) {
        try {
            actorPhotoService.delete(id, photoId, authentication);
            ra.addFlashAttribute("message", "Фотография удалена!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        if ("edit".equals(from)) return "redirect:/actors/edit/" + id;
        return "redirect:/actors/" + id + "?from=" + from + (movieId != null ? "&movieId=" + movieId : "");
    }

    @PostMapping("/{id}/photos/primary-upload")
    public String uploadPrimaryActorPhoto(
            @PathVariable Long id,
            @RequestParam("imageFile") MultipartFile imageFile,
            Authentication authentication,
            RedirectAttributes ra
    ) {
        try {
            actorPhotoService.uploadPrimary(id, imageFile, authentication);
            ra.addFlashAttribute("message", "Загружена основная фотография!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actors/edit/" + id;
    }

    private void checkModifyPermission(Authentication authentication) {
        if (securityUtils.isReadOnly(authentication)) {
            throw new RuntimeException("Пользователи, доступные только для чтения, не могут изменять данные");
        }
    }
}
