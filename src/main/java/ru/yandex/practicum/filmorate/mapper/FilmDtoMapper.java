package ru.yandex.practicum.filmorate.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.service.MpaService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FilmDtoMapper {

    private final GenreService genreService;
    private final MpaService mpaService;

    public FilmDto toDto(Film film) {
        List<Genre> genres = genreService.findGenresByIds(film.getGenresIds());
        MpaDto mpaDto = mpaService.findMpaById(film.getMpaId());

        return FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(mpaDto)
                .genres(genres)
                .build();
    }

    public List<FilmDto> toDtos(List<Film> films) {
        List<Long> filmIds = extractFilmIds(films);

        Map<Long, List<Genre>> filmsGenres = genreService.getGenresByFilmIds(filmIds);
        Map<Long, MpaDto> filmsMpa = mpaService.getMpaByFilmIds(filmIds);

        return films.stream()
                .map(film -> {
                    List<Genre> genres = filmsGenres.getOrDefault(film.getId(), Collections.emptyList());
                    MpaDto mpaDto = filmsMpa.get(film.getId());

                    return FilmDto.builder()
                            .id(film.getId())
                            .name(film.getName())
                            .description(film.getDescription())
                            .releaseDate(film.getReleaseDate())
                            .duration(film.getDuration())
                            .mpa(mpaDto)
                            .genres(genres)
                            .build();
                })
                .toList();
    }

    private List<Long> extractFilmIds(List<Film> films) {
        return films.stream()
                .map(Film::getId)
                .toList();
    }
}
