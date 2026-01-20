package ru.nikogosyan.CourseProject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "calculationlogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movieid", nullable = false)
    private Movie movie;

    @Column(name = "createdby", length = 50)
    private String createdBy;

    @Column(name = "createdat", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "boxoffice", precision = 15, scale = 2)
    private BigDecimal boxOffice;

    @Column(name = "productionbudget", precision = 15, scale = 2)
    private BigDecimal productionBudget;

    @Column(name = "marketingbudget", precision = 15, scale = 2)
    private BigDecimal marketingBudget;

    @Column(name = "distributionfeepercent", precision = 5, scale = 4)
    private BigDecimal distributionFeePercent;

    @Column(name = "taxpercent", precision = 5, scale = 4)
    private BigDecimal taxPercent;

    @Column(name = "actorssalary", precision = 15, scale = 2)
    private BigDecimal actorsSalary;

    @Column(name = "studiorevenue", precision = 15, scale = 2)
    private BigDecimal studioRevenue;

    @Column(name = "profitbeforetax", precision = 15, scale = 2)
    private BigDecimal profitBeforeTax;

    @Column(name = "taxamount", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "netprofit", precision = 15, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "roi", precision = 18, scale = 6)
    private BigDecimal roi;
}
