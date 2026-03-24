package ru.yandex.practicum.filmorate.dto.film;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import ru.yandex.practicum.filmorate.dto.genre.GenreRequestDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaRequestDto;
import ru.yandex.practicum.filmorate.model.Director;

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

    private MpaRequestDto mpa;
    private List<GenreRequestDto> genres;

    private List<Director> directors;
}
