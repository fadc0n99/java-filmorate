package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateFilmDto {
    @Positive
    private long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Integer duration;
    private MpaId mpa;
    private List<GenreId> genres;

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasReleaseDate() {
        return releaseDate != null;
    }

    public boolean hasDuration() {
        return duration != null && duration > 0;
    }

    public boolean hasMpa() {
        return mpa != null;
    }

    public boolean hasGenres() {
        return genres != null && !genres.isEmpty();
    }

}
