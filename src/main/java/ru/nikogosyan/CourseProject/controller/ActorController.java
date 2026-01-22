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
import ru.nikogosyan.CourseProject.entity.MovieCast;
import ru.nikogosyan.CourseProject.service.ActorPhotoService;
import ru.nikogosyan.CourseProject.service.ActorService;
import ru.nikogosyan.CourseProject.service.MovieCastService;
import ru.nikogosyan.CourseProject.service.MovieService;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/actors")
@RequiredArgsConstructor
@Slf4j
public class ActorController {

    private static final String VIEW_ACTOR_DETAILS = "actor-details";
    private static final String VIEW_ACTORS_LIST = "actors-list";
    private static final String VIEW_ACTOR_FORM = "actor-form";

    private final ActorService actorService;
    private final MovieService movieService;
    private final ActorPhotoService actorPhotoService;
    private final SecurityUtils securityUtils;
    private final MovieCastService movieCastService;

    @GetMapping("/{id}")
    public String actorDetails(
            @PathVariable Long id,
            @RequestParam(value = "from", defaultValue = "actors") String from,
            @RequestParam(value = "movieId", required = false) Long movieId,
            Authentication authentication,
            Model model
    ) {
        Actor actor = actorService.getActorForView(id, authentication);

        List<MovieCast> castings = movieCastService.getCastByActorIdForView(id, authentication);
        model.addAttribute("castings", castings);

        if (movieId != null) {
            actorService.applyRoleForMovie(actor, movieId);
        }

        boolean canModify = !securityUtils.isReadOnly(authentication);

        model.addAttribute("page", "actors");
        model.addAttribute("actor", actor);
        model.addAttribute("photos", actorPhotoService.getPhotos(id));
        model.addAttribute("primaryPhotoPath", actorPhotoService.getPrimaryOrFirstPhotoPath(id));
        model.addAttribute("canModify", canModify);
        model.addAttribute("from", from);
        model.addAttribute("movieId", movieId);

        return VIEW_ACTOR_DETAILS;
    }

    @GetMapping
    public String listActors(Authentication authentication, Model model) {
        List<Actor> actors = actorService.getAllActors(authentication);
        boolean canModify = !securityUtils.isReadOnly(authentication);

        List<Long> actorIds = actors.stream().map(Actor::getId).toList();

        Map<Long, Long> movieCountByActorId =
                movieCastService.getMovieCountByActorIds(actorIds, authentication);
        model.addAttribute("movieCountByActorId", movieCountByActorId);

        model.addAttribute("primaryPhotoByActorId", actorPhotoService.getPrimaryOrFirstPhotoPaths(actorIds));
        model.addAttribute("page", "actors");
        model.addAttribute("actors", actors);
        model.addAttribute("canModify", canModify);

        return VIEW_ACTORS_LIST;
    }

    @GetMapping("/new")
    public String newActorForm(Authentication authentication, Model model) {
        checkModifyPermission(authentication);

        model.addAttribute("actor", new Actor());
        model.addAttribute("movies", movieService.getAllMovies(authentication));

        return VIEW_ACTOR_FORM;
    }

    @PostMapping("/new")
    public String createActor(
            @Valid @ModelAttribute("actor") Actor actor,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes ra
    ) {
        if (result.hasErrors()) {
            model.addAttribute("movies", movieService.getAllMovies(authentication));
            return VIEW_ACTOR_FORM;
        }

        checkModifyPermission(authentication);

        Actor saved = actorService.saveActor(actor, authentication);

        ra.addAttribute("id", saved.getId());
        return "redirect:/actors/{id}";
    }

    @GetMapping("/edit/{id}")
    public String editActorForm(@PathVariable Long id, Authentication authentication, Model model) {
        checkModifyPermission(authentication);

        Actor actor = actorService.getActorForView(id, authentication);

        model.addAttribute("actor", actor);
        model.addAttribute("movies", movieService.getAllMovies(authentication));
        model.addAttribute("photos", actorPhotoService.getPhotos(id));
        model.addAttribute("primaryPhotoPath", actorPhotoService.getPrimaryOrFirstPhotoPath(id));

        return VIEW_ACTOR_FORM;
    }

    @PostMapping("/edit/{id}")
    public String updateActor(
            @PathVariable Long id,
            @Valid @ModelAttribute("actor") Actor actor,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes ra
    ) {
        if (result.hasErrors()) {
            actor.setId(id);
            model.addAttribute("movies", movieService.getAllMovies(authentication));
            return VIEW_ACTOR_FORM;
        }

        checkModifyPermission(authentication);

        Actor saved = actorService.updateActor(id, actor, authentication);

        ra.addAttribute("id", saved.getId());
        return "redirect:/actors/{id}";
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

        return redirectBack(id, from, movieId, ra);
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

        return redirectBack(id, from, movieId, ra);
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

        return redirectBack(id, from, movieId, ra);
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

        ra.addAttribute("id", id);
        return "redirect:/actors/edit/{id}";
    }

    /**
     * Единая логика возврата на страницу:
     * - если from=edit -> на /actors/edit/{id}
     * - иначе -> на /actors/{id} + query params (from, movieId) через RedirectAttributes
     */
    private String redirectBack(Long actorId, String from, Long movieId, RedirectAttributes ra) {
        if ("edit".equals(from)) {
            ra.addAttribute("id", actorId);
            return "redirect:/actors/edit/{id}";
        }

        ra.addAttribute("id", actorId);
        ra.addAttribute("from", from);
        if (movieId != null) {
            ra.addAttribute("movieId", movieId);
        }
        return "redirect:/actors/{id}";
    }

    private void checkModifyPermission(Authentication authentication) {
        if (securityUtils.isReadOnly(authentication)) {
            throw new RuntimeException("Пользователи, доступные только для чтения, не могут изменять данные");
        }
    }
}
