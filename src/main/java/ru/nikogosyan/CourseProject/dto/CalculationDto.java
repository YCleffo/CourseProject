package ru.nikogosyan.CourseProject.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class CalculationDto {

    @NotNull
    private Long movieId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Production budget must be >= 0")
    private BigDecimal productionBudget = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal distributionFeePercentUi = new BigDecimal("50");

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal taxPercentUi = new BigDecimal("20");

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Marketing budget must be >= 0")
    private BigDecimal marketingBudget = BigDecimal.ZERO;

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
