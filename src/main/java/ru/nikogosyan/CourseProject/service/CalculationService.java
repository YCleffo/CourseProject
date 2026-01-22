package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikogosyan.CourseProject.dto.CalculationDto;
import ru.nikogosyan.CourseProject.dto.CalculationResultDto;
import ru.nikogosyan.CourseProject.entity.CalculationLog;
import ru.nikogosyan.CourseProject.entity.Movie;
import ru.nikogosyan.CourseProject.repository.CalculationLogRepository;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final CalculationLogRepository calculationLogRepository;
    private final SecurityUtils securityUtils;

    public CalculationResultDto calculate(Movie movie,
                                          BigDecimal actorsSalary,
                                          CalculationDto form) {

        BigDecimal boxOffice = nz(movie.getBoxOffice());
        BigDecimal distribution = nz(form.getDistributionFeePercent());
        BigDecimal taxPercent = nz(form.getTaxPercent());

        BigDecimal studioRevenue = boxOffice.multiply(BigDecimal.ONE.subtract(distribution));
        BigDecimal costs = nz(form.getProductionBudget())
                .add(nz(form.getMarketingBudget()))
                .add(nz(actorsSalary));

        BigDecimal profitBeforeTax = studioRevenue.subtract(costs);

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (profitBeforeTax.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = profitBeforeTax.multiply(taxPercent);
        }

        BigDecimal netProfit = profitBeforeTax.subtract(taxAmount);

        BigDecimal invested = nz(form.getProductionBudget())
                .add(nz(form.getMarketingBudget()))
                .add(nz(actorsSalary));

        BigDecimal roi = BigDecimal.ZERO;
        if (invested.compareTo(BigDecimal.ZERO) > 0) {
            roi = netProfit.divide(invested, 6, RoundingMode.HALF_UP);
        }

        return new CalculationResultDto(
                scale2(boxOffice),
                scale2(actorsSalary),
                scale2(studioRevenue),
                scale2(profitBeforeTax),
                scale2(taxAmount),
                scale2(netProfit),
                roi
        );
    }

    @Transactional
    public void clearLogs(Long movieId, Authentication authentication) {
        var ui = securityUtils.getUserInfo(authentication);
        if (ui.isReadOnly()) throw new RuntimeException("Пользователи, доступные только для чтения, не могут изменять данные");
        if (ui.isAdmin()) calculationLogRepository.deleteByMovieId(movieId);
        else calculationLogRepository.deleteByMovieIdAndCreatedBy(movieId, ui.username());
    }

    @Transactional
    public CalculationLog saveLog(Movie movie,
                                  Authentication authentication,
                                  CalculationDto form,
                                  CalculationResultDto result) {

        String username = (authentication != null) ? authentication.getName() : null;

        CalculationLog log = new CalculationLog();
        log.setMovie(movie);
        log.setCreatedBy(username);

        log.setBoxOffice(result.getBoxOffice());

        log.setProductionBudget(scale2(form.getProductionBudget()));
        log.setMarketingBudget(scale2(form.getMarketingBudget()));
        log.setDistributionFeePercent(form.getDistributionFeePercent());
        log.setTaxPercent(form.getTaxPercent());

        log.setActorsSalary(result.getActorsSalary());
        log.setStudioRevenue(result.getStudioRevenue());
        log.setProfitBeforeTax(result.getProfitBeforeTax());
        log.setTaxAmount(result.getTaxAmount());
        log.setNetProfit(result.getNetProfit());
        log.setRoi(result.getRoi());

        return calculationLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<CalculationLog> getLogsForMovie(Long movieId, Authentication authentication) {
        SecurityUtils.UserInfo ui = securityUtils.getUserInfo(authentication);

        if (ui.isAdmin()) {
            return calculationLogRepository.findByMovieIdOrderByCreatedAtDesc(movieId);
        }

        return calculationLogRepository.findByMovieIdAndCreatedByOrderByCreatedAtDesc(movieId, ui.username());
    }

    private static BigDecimal nz(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }

    private static BigDecimal scale2(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP);
    }
}
