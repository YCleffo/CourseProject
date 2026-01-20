package ru.nikogosyan.CourseProject.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CalculationDto {
    private Long movieId;
    private Double profitPercent = 0.1;
    private BigDecimal totalActorsSalary;
    private BigDecimal grossProfit;
    private BigDecimal netProfit;
}