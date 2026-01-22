package ru.nikogosyan.CourseProject.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class CalculationDto {

    @NotNull(message = "Выберите фильм")
    private Long movieId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Бюджет (производство) должен быть 0 или больше")
    private BigDecimal productionBudget = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Маркетинг должен быть 0 или больше")
    private BigDecimal marketingBudget = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Комиссия дистрибьютора должна быть от 0 до 100")
    @DecimalMax(value = "100.0", message = "Комиссия дистрибьютора должна быть от 0 до 100")
    private BigDecimal distributionFeePercentUi = new BigDecimal("50");

    @NotNull
    @DecimalMin(value = "0.0", message = "Налог должен быть от 0 до 100")
    @DecimalMax(value = "100.0", message = "Налог должен быть от 0 до 100")
    private BigDecimal taxPercentUi = new BigDecimal("20");

    public BigDecimal getDistributionFeePercent() {
        return pctToFraction(distributionFeePercentUi);
    }

    public BigDecimal getTaxPercent() {
        return pctToFraction(taxPercentUi);
    }

    private static BigDecimal pctToFraction(BigDecimal pct) {
        if (pct == null) return BigDecimal.ZERO;
        return pct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }
}
