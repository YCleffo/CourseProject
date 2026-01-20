package ru.nikogosyan.CourseProject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.service.ActorService;
import ru.nikogosyan.CourseProject.service.MovieService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/calculation")
@RequiredArgsConstructor
public class CalculationController {

    private final MovieService movieService;
    private final ActorService actorService;

    @GetMapping
    public String calculationPage(Authentication authentication, Model model) {
        model.addAttribute("page", "calculation");
        model.addAttribute("movies", movieService.getAllMovies(authentication));
        return "calculation";
    }

    @PostMapping("/profit")
    public String calculateProfit(@RequestParam Long movieId,
                                  @RequestParam(defaultValue = "0.1") Double profitPercent,
                                  Authentication authentication,
                                  Model model) {
        var movie = movieService.getMovieForView(movieId, authentication);
        List<Actor> actors = actorService.getActorsByMovieIdForView(movieId, authentication);

        BigDecimal totalActorsSalary = actors.stream()
                .map(Actor::getSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal boxOffice = movie.getBoxOffice() != null ? movie.getBoxOffice() : BigDecimal.ZERO;
        BigDecimal profit = boxOffice.multiply(BigDecimal.valueOf(profitPercent));
        BigDecimal netProfit = profit.subtract(totalActorsSalary);

        model.addAttribute("movie", movie);
        model.addAttribute("actors", actors);
        model.addAttribute("totalActorsSalary", totalActorsSalary);
        model.addAttribute("grossProfit", profit);
        model.addAttribute("netProfit", netProfit);
        model.addAttribute("profitPercent", profitPercent * 100);
        model.addAttribute("movies", movieService.getAllMovies(authentication));

        return "calculation";
    }
}