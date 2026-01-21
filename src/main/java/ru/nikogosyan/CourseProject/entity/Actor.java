package ru.nikogosyan.CourseProject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "actors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "movie")
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Transient
    @NotNull(message = "Movie is required")
    private Long movieId;

    @Column(nullable = false)
    @NotBlank(message = "Actor name is required")
    private String name;

    @Column(name = "role_name")
    private String roleName;

    @Column(precision = 15, scale = 2)
    @DecimalMin(value = "0.0", message = "Salary must be positive")
    private BigDecimal salary;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PostLoad
    public void syncMovieIdAfterLoad() {
        if (this.movie != null) {
            this.movieId = this.movie.getId();
        }
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
        this.movieId = (movie != null ? movie.getId() : null);
    }
}
