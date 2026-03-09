package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.Set;

/**
 * Film.
 */
@Data
@EqualsAndHashCode(of = { "id" })
@Builder(toBuilder = true)
public class Film {
    private Long id;
    @NotNull
    @NotBlank
    private String name;
    @Length(max = 200)
    private String description;
    @NotNull
    private LocalDate releaseDate;
    @Positive
    private Integer duration;
    private Set<Long> likedUsersFilms;
}
