package ru.nikogosyan.CourseProject.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "actors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"movies"})
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "actor_movies",
            joinColumns = @JoinColumn(name = "actor_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private Set<Movie> movies = new HashSet<>();

    @Transient
    @NotNull(message = "Movie is required")
    private Set<Long> movieIds = new HashSet<>();

    @Column(nullable = false)
    @NotBlank(message = "Требуется имя актера")
    private String name;

    @Transient
    private String roleName;

    @Transient
    private BigDecimal salary;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PostLoad
    public void syncMovieIdsAfterLoad() {
        if (movies != null) {
            this.movieIds = movies.stream()
                    .map(Movie::getId)
                    .collect(Collectors.toSet());
        }
    }

    public void setMovies(Set<Movie> movies) {
        this.movies = movies;
        this.movieIds = (movies == null)
                ? new HashSet<>()
                : movies.stream().map(Movie::getId).collect(Collectors.toSet());
    }
}
