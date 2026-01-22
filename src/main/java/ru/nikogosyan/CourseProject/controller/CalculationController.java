package ru.nikogosyan.CourseProject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.nikogosyan.CourseProject.dto.CalculationDto;
import ru.nikogosyan.CourseProject.dto.CalculationResultDto;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.service.ActorService;
import ru.nikogosyan.CourseProject.service.CalculationService;
import ru.nikogosyan.CourseProject.service.MovieService;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/calculation")
@RequiredArgsConstructor
public class CalculationController {

    private final MovieService movieService;
    private final ActorService actorService;
    private final CalculationService calculationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String calculationPage(@RequestParam(value = "movieId", required = false) Long movieId,
                                  Authentication authentication,
                                  Model model) {

        boolean canModify = !securityUtils.isReadOnly(authentication);
        model.addAttribute("canModify", canModify);

        model.addAttribute("page", "calculation");
        model.addAttribute("movies", movieService.getAllMovies(authentication));

        CalculationDto form = new CalculationDto();
        if (movieId != null) {
            form.setMovieId(movieId);
        }
        model.addAttribute("calc", form);

        if (movieId != null) {
            model.addAttribute("logs", calculationService.getLogsForMovie(movieId, authentication));
        }

        if (movieId != null) {
            Movie movie = movieService.getMovieForView(movieId, authentication);
            List<Actor> actors = actorService.getActorsByMovieIdForView(movieId, authentication);

            BigDecimal actorsSalary = actors.stream()
                    .map(Actor::getSalary)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            form.setProductionBudget(movie.getBudget() != null ? movie.getBudget() : BigDecimal.ZERO);

            model.addAttribute("selectedMovie", movie);
            model.addAttribute("selectedActorsSalary", actorsSalary);
        }

        return "calculation";
    }

    @PostMapping("/profit")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    public String calculateV2(@Valid @ModelAttribute("calc") CalculationDto calc,
                              BindingResult bindingResult,
                              Authentication authentication,
                              Model model) {
        boolean canModify = !securityUtils.isReadOnly(authentication);
        model.addAttribute("canModify", canModify);

        model.addAttribute("page", "calculation");
        model.addAttribute("movies", movieService.getAllMovies(authentication));

        if (bindingResult.hasErrors()) {
            return "calculation";
        }

        Movie movie = movieService.getMovieForView(calc.getMovieId(), authentication);

        List<Actor> actors = actorService.getActorsByMovieIdForView(movie.getId(), authentication);
        BigDecimal actorsSalary = actors.stream()
                .map(Actor::getSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CalculationResultDto result = calculationService.calculate(movie, actorsSalary, calc);

        if (securityUtils.isReadOnly(authentication)) {
            throw new RuntimeException("Пользователи, доступные только для чтения, не могут изменять данные");
        }

        calculationService.saveLog(movie, authentication, calc, result);

        model.addAttribute("movie", movie);
        model.addAttribute("actors", actors);
        model.addAttribute("result", result);

        model.addAttribute("logs", calculationService.getLogsForMovie(movie.getId(), authentication));

        return "calculation";
    }

    @PostMapping("/clear")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String clear(@RequestParam Long movieId, Authentication auth, RedirectAttributes ra) {
        calculationService.clearLogs(movieId, auth);
        ra.addFlashAttribute("message", "История расчётов очищена");
        return "redirect:/calculation?movieId=" + movieId;
    }
}
