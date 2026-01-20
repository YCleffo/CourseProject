package ru.nikogosyan.CourseProject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CalculationResultDto {
    private BigDecimal boxOffice;
    private BigDecimal actorsSalary;

    private BigDecimal studioRevenue;
    private BigDecimal profitBeforeTax;
    private BigDecimal taxAmount;
    private BigDecimal netProfit;

    private BigDecimal roi;
}
