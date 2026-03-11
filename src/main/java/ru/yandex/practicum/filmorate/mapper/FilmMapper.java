package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaRequestDto;
import ru.yandex.practicum.filmorate.dto.film.UpdateFilmDto;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmMapper {

    public static Film mapToFilm(CreateFilmDto requestDto) {
        return Film.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .releaseDate(requestDto.getReleaseDate())
                .duration(requestDto.getDuration())
                .mpaRequestDto(requestDto.getMpa())
                .genresId(requestDto.getGenres())
                .build();
    }

    public static FilmDto mapToFilmDto(Film film, Map<Long, Mpa> mpaMap, Map<Long, Genre> genreMap) {
        Mpa fullMpa = mpaMap.get(film.getMpaRequestDto().getId());
        MpaDto mpaDto = MpaMapper.mpaToMpaDto(fullMpa);

        List<Genre> fullGenres = film.getGenresId().stream()
                .map(genreId -> genreMap.get(genreId.getId()))
                .filter(Objects::nonNull)
                .toList();

        return FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(mpaDto)
                .genres(fullGenres)
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
            film.setMpaRequestDto(MpaRequestDto.of(dto.getMpa().getId()));
        }

        if (dto.hasGenres()) {
            film.setGenresId(dto.getGenres());
        }

        return film;
    }
}
