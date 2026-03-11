package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CreateFilmDto {
    @NotNull
    @NotBlank
    private String name;

    @NotNull
    @NotBlank
    @Length(max = 200)
    private String description;

    @NotNull
    private LocalDate releaseDate;

    @Positive
    private Integer duration;

    private MpaId mpa;
    private List<GenreId> genres;
}
