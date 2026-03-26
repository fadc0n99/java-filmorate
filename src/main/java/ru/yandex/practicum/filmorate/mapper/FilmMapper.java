package ru.yandex.practicum.filmorate.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.film.UpdateFilmDto;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public final class FilmMapper {

    private final MpaMapper mpaMapper;

    public static Film toEntity(CreateFilmDto requestDto) {
        List<Genre> genres = requestDto.getGenres() != null ?
                requestDto.getGenres().stream()
                        .map(genreDto -> Genre.builder()
                                .id(genreDto.getId())
                                .build())
                        .toList() :
                new ArrayList<>();

        return Film.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .releaseDate(requestDto.getReleaseDate())
                .duration(requestDto.getDuration())
                .mpa(Mpa.builder()
                        .id(requestDto.getMpa().getId())
                        .build())
                .genres(genres)
                .directors(requestDto.getDirectors() != null ? requestDto.getDirectors() : new ArrayList<>())
                .build();
    }

    public FilmDto toDto(Film film) {
        return FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(mpaMapper.toDto(film.getMpa()))
                .genres(film.getGenres())
                .directors(film.getDirectors())
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
            Mpa mpa = Mpa.builder()
                    .id(dto.getMpa().getId())
                    .build();
            film.setMpa(mpa);
        }

        if (dto.hasGenres()) {
            List<Genre> genres = dto.getGenres().stream()
                    .map(genreDto -> Genre.builder()
                            .id(genreDto.getId())
                            .build())
                    .toList();
            film.setGenres(genres);
        } else {
            film.setGenres(Collections.emptyList());
        }

        if (dto.hasDirectors()) {
            film.setDirectors(dto.getDirectors());
        } else {
            film.setDirectors(Collections.emptyList());
        }

        return film;
    }
}