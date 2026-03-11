package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.MpaId;
import ru.yandex.practicum.filmorate.dto.UpdateFilmDto;
import ru.yandex.practicum.filmorate.model.Film;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmMapper {

    public static Film mapToFilm(CreateFilmDto requestDto) {
        return Film.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .releaseDate(requestDto.getReleaseDate())
                .duration(requestDto.getDuration())
                .mpaId(requestDto.getMpa())
                .genresId(requestDto.getGenres())
                .build();
    }

    public static FilmDto mapToFilmDto(Film film) {
        return FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(MpaId.of(film.getMpaId().getId()))
                .genres(film.getGenresId())
                .build();
    }

    public static Film updateFilmFields(Film film, UpdateFilmDto dto) {
        if (dto.hasName()) {
            film.setName(dto.getName());
        }

        if (dto.hasDescription()) {
            film.setDescription(dto.getDescription());
        }

        if (dto.hasReleaseDate()) {
            film.setReleaseDate(dto.getReleaseDate());
        }

        if (dto.hasDuration()) {
            film.setDuration(dto.getDuration());
        }

        if (dto.hasMpa()) {
            film.setMpaId(MpaId.of(dto.getMpa().getId()));
        }

        if (dto.hasGenres()) {
            film.setGenresId(dto.getGenres());
        }

        return film;
    }
}
