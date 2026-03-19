package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.film.UpdateFilmDto;
import ru.yandex.practicum.filmorate.dto.genre.GenreRequestDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmMapper {

    public static Film toFilm(CreateFilmDto requestDto) {
        List<Long> genreIds = requestDto.getGenres() != null ?
                requestDto.getGenres().stream().map(GenreRequestDto::getId).toList() : new ArrayList<>();

        return Film.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .releaseDate(requestDto.getReleaseDate())
                .duration(requestDto.getDuration())
                .mpaId(requestDto.getMpa().getId())
                .genresIds(genreIds)
                .directors(requestDto.getDirectors() != null ? requestDto.getDirectors() : new ArrayList<>())
                .build();
    }

    public static FilmDto toDto(Film film, MpaDto mpaDto, List<Genre> genres) {
        return FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(mpaDto)
                .genres(genres)
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
            film.setMpaId(dto.getMpa().getId());
        }

        if (dto.getGenres() != null) {
            List<Long> genreIds = dto.getGenres().stream()
                    .map(GenreRequestDto::getId)
                    .toList();
            film.setGenresIds(genreIds);
        }

        if (dto.getDirectors() != null) {
            film.setDirectors(dto.getDirectors());
        }

        return film;
    }
}