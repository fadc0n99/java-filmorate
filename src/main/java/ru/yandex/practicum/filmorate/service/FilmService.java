package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.film.UpdateFilmDto;
import ru.yandex.practicum.filmorate.dto.genre.GenreRequestDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaRequestDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final GenreService genreService;
    private final MpaService mpaService;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       UserService userService,
                       GenreService genreService,
                       MpaService mpaService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.mpaService = mpaService;
        this.genreService = genreService;
    }

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final String ERROR_INVALID_ID_MESSAGE = "Invalid film ID: must be greater than 0";
    private static final String ERROR_FILM_NOT_FOUND_MESSAGE = "Film with ID %d not found";
    private static final String ERROR_RELEASE_DATE_MESSAGE =
            String.format("Release date must be after %s", MIN_RELEASE_DATE);

    public List<FilmDto> findAllFilms() {
        log.debug("Retrieving all films from storage");

        List<Film> films = filmStorage.findAll();

        return mapToFilmDtos(films);
    }

    public FilmDto createFilm(CreateFilmDto createFilmDto) {
        log.debug("Starting add film: {}", createFilmDto);

        validateMpaRating(createFilmDto.getMpa());

        List<GenreRequestDto> processedGenres = processGenres(createFilmDto.getGenres());
        createFilmDto.setGenres(processedGenres);

        Film film = FilmMapper.mapToFilm(createFilmDto);

        validateReleaseDate(film);

        film = filmStorage.save(film);
        return mapToFilmDtos(List.of(film)).getFirst();
    }

    public FilmDto updateFilm(UpdateFilmDto updateFilmDto) {
        requireValidFilm(updateFilmDto.getId());
        validateMpaRating(updateFilmDto.getMpa());

        updateFilmDto.setGenres(processGenres(updateFilmDto.getGenres()));

        Film updatedFilm = filmStorage.findFilmById(updateFilmDto.getId())
                .map(film -> FilmMapper.updateFilmFields(film, updateFilmDto))
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_FILM_NOT_FOUND_MESSAGE, updateFilmDto.getId())
                        )
                );

        validateReleaseDate(updatedFilm);

        updatedFilm = filmStorage.update(updatedFilm);

        return mapToFilmDtos(List.of(updatedFilm)).getFirst();
    }

    public FilmDto getFilm(long id) {
        requireValidFilm(id);

        log.debug("Retrieving film by ID: {}", id);

        return filmStorage.findFilmById(id)
                .map(film -> mapToFilmDtos(List.of(film)))
                .map(List::getFirst)
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_FILM_NOT_FOUND_MESSAGE, id)
                        )
                );
    }

    private Map<Long, Mpa> loadFilmsMpa(List<Film> films) {
        Set<Long> mpaIds = films.stream()
                .map(film -> film.getMpaRequestDto().getId())
                .collect(Collectors.toSet());

        Set<Mpa> mpas = mpaService.findMpaByIds(mpaIds);
        return mpas.stream()
                .collect(Collectors.toMap(
                        Mpa::getId,
                        Function.identity()
                ));
    }

    private Map<Long, Genre> loadFilmsGenres(List<Film> films) {
        Set<Long> genreIds = films.stream()
                .flatMap(film -> film.getGenresId().stream())
                .map(GenreRequestDto::getId)
                .collect(Collectors.toSet());

        Set<Genre> genres = genreService.findGenresByIds(genreIds);
        return genres.stream()
                .collect(Collectors.toMap(
                        Genre::getId,
                        Function.identity()
                ));
    }

    private List<FilmDto> mapToFilmDtos(List<Film> films) {
        Map<Long, Genre> genreMap = loadFilmsGenres(films);
        Map<Long, Mpa> mpaMapDtos = loadFilmsMpa(films);

        return films.stream()
                .map(film -> FilmMapper.mapToFilmDto(film, mpaMapDtos, genreMap))
                .toList();
    }

    public void addFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);
        userService.requireValidUser(userId);

        log.debug("Adding like. Film: {}, User: {}", filmId, userId);

        if (isLikeExists(filmId, userId)) {
            log.error("User {} already liked film {}", userId, filmId);
            throw new ValidationException("User has already liked this film");
        }

        filmStorage.addLike(filmId, userId);
    }

    public void removeFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);
        userService.requireValidUser(userId);

        log.debug("Removing like. Film: {}, User: {}", filmId, userId);

        if (!isLikeExists(filmId, userId)) {
            log.error("Cannot remove non-existent like. Film: {}, User: {}", filmId, userId);
            throw new ValidationException("User hasn't liked this film");
        }

        filmStorage.removeLike(filmId, userId);
    }

    public void requireValidFilm(long filmId) {
        if (filmId <= 0) {
            throw new ValidationException(ERROR_INVALID_ID_MESSAGE);
        }
        if (!isFilmExists(filmId)) {
            throw new NotFoundException(String.format(ERROR_FILM_NOT_FOUND_MESSAGE, filmId));
        }
    }

    public boolean isFilmExists(long filmId) {
        return filmStorage.isExistById(filmId);
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException(ERROR_RELEASE_DATE_MESSAGE);
        }
    }

    public boolean isLikeExists(long filmId, long userId) {
        return filmStorage.isLikeExists(filmId, userId);
    }

    public List<FilmDto> getPopularFilms(long count) {
        log.debug("Getting top {} popular films", count);

        List<Film> popularFilms = filmStorage.getPopularFilms(count);
        return mapToFilmDtos(popularFilms);
    }

    private void validateMpaRating(MpaRequestDto mpaRequestDto) {
        if (mpaRequestDto == null) {
            throw new ValidationException("MPA rating is required for film");
        }

        if (!filmStorage.isMpaExist(mpaRequestDto.getId())) {
            throw new NotFoundException(
                    String.format("MPA rating with ID %d not found", mpaRequestDto.getId())
            );
        }
    }

    private List<GenreRequestDto> processGenres(List<GenreRequestDto> genres) {
        if (genres == null || genres.isEmpty()) {
            return Collections.emptyList();
        }

        if (!filmStorage.isGenresExist(genres)) {
            throw new NotFoundException("One or more genres not found");
        }

        return new ArrayList<>(new HashSet<>(genres));
    }
}
