package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.UpdateFilmDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage, UserService userService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
    }

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final String ERROR_INVALID_ID_MESSAGE = "Invalid film ID: must be greater than 0";
    private static final String ERROR_FILM_NOT_FOUND_MESSAGE = "Film with ID %d not found";
    private static final String ERROR_RELEASE_DATE_MESSAGE =
            String.format("Release date must be after %s", MIN_RELEASE_DATE);

    public List<FilmDto> findAllFilms() {
        log.debug("Retrieving all films from storage");

        return filmStorage.findAll()
                .stream()
                .map(FilmMapper::mapToFilmDto)
                .collect(Collectors.toList());
    }

    public FilmDto createFilm(CreateFilmDto createFilmDto) {
        Film film = FilmMapper.mapToFilm(createFilmDto);

        validateReleaseDate(film);

        if (film.getMpaId() != null && !filmStorage.isMpaExist(film.getMpaId().getId())) {
            throw new NotFoundException("MPA rating with ID " + film.getMpaId().getId() + " not found");
        }
        if (film.getGenresId() != null && !filmStorage.isGenresExist(film.getGenresId())) {
            throw new NotFoundException("One or more genres not found");
        }

        log.info("film: {}", film);

        film = filmStorage.save(film);

        return FilmMapper.mapToFilmDto(film);
    }

    public FilmDto updateFilm(UpdateFilmDto updateFilmDto) {
        requireValidFilm(updateFilmDto.getId());

        log.debug("Starting update film. ID: {}, Name: {}", updateFilmDto.getId(), updateFilmDto.getName());

        Film updatedFilm = filmStorage.findFilmById(updateFilmDto.getId())
                .map(film -> FilmMapper.updateFilmFields(film, updateFilmDto))
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_FILM_NOT_FOUND_MESSAGE, updateFilmDto.getId())
                        )
                );

        validateReleaseDate(updatedFilm);
        updatedFilm = filmStorage.update(updatedFilm);

        return FilmMapper.mapToFilmDto(updatedFilm);
    }

    public FilmDto getFilm(long id) {
        requireValidFilm(id);

        log.debug("Retrieving film by ID: {}", id);

        return filmStorage.findFilmById(id)
                .map(FilmMapper::mapToFilmDto)
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_FILM_NOT_FOUND_MESSAGE, id)
                        )
                );
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

        return filmStorage.getPopularFilms(count)
                .stream()
                .map(FilmMapper::mapToFilmDto)
                .toList();
    }
}
