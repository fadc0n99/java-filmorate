package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.film.UpdateFilmDto;
import ru.yandex.practicum.filmorate.dto.genre.GenreRequestDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaRequestDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;
    private final UserStorage userStorage;
    private final DirectorStorage directorStorage;
    private final FilmMapper filmMapper;
    private final FeedService feedService; // <-- Добавлено

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       GenreStorage genreStorage,
                       MpaStorage mpaStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       DirectorStorage directorStorage,
                       FilmMapper filmMapper,
                       FeedService feedService) { // <-- Добавлено
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.directorStorage = directorStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
        this.filmMapper = filmMapper;
        this.feedService = feedService; // <-- Добавлено
    }

    public List<FilmDto> findAllFilms() {
        log.debug("Retrieving all films from storage");
        List<Film> films = filmStorage.findAll();
        return films.stream()
                .map(filmMapper::toDto)
                .toList();
    }

    @Transactional
    public FilmDto createFilm(CreateFilmDto createFilmDto) {
        log.debug("Starting add film: {}", createFilmDto);
        validateMpaRating(createFilmDto.getMpa());
        validateGenres(createFilmDto.getGenres());
        validateDirectors(createFilmDto.getDirectors());

        Film film = FilmMapper.toEntity(createFilmDto);
        validateReleaseDate(film);

        film = filmStorage.save(film);
        return filmMapper.toDto(film);
    }

    @Transactional
    public FilmDto updateFilm(UpdateFilmDto updateFilmDto) {
        validateMpaRating(updateFilmDto.getMpa());
        validateGenres(updateFilmDto.getGenres());
        validateDirectors(updateFilmDto.getDirectors());

        Film updatedFilm = filmStorage.findFilmById(updateFilmDto.getId())
                .map(film -> FilmMapper.updateFilmFields(film, updateFilmDto))
                .orElseThrow(() -> new NotFoundException(ErrorMessages.filmNotFound(updateFilmDto.getId())));

        validateReleaseDate(updatedFilm);
        updatedFilm = filmStorage.update(updatedFilm);

        return filmMapper.toDto(updatedFilm);
    }

    public FilmDto getFilm(Long id) {
        log.debug("Retrieving film by ID: {}", id);

        return filmStorage.findFilmById(id)
                .map(filmMapper::toDto)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.filmNotFound(id)));
    }

    // метод для удаления фильма
    @Transactional
    public void deleteFilm(Long filmId) {
        log.debug("Deleting film with ID: {}", filmId);
        validateFilmExists(filmId);
        filmStorage.delete(filmId);
        log.info("Film {} deleted successfully", filmId);
    }

    public List<FilmDto> getFilmsByDirector(Integer directorId, String sortBy) {
        validateDirectorsExists(directorId);

        log.debug("Retrieving films for director ID: {} sorted by {}", directorId, sortBy);
        List<Film> films = filmStorage.findAllByDirector(directorId, sortBy);

        return films.stream()
                .map(filmMapper::toDto)
                .toList();
    }

    public List<FilmDto> getPopularFilms(Integer count, Integer genreId, Integer year) {
        log.debug("Getting top {} popular films", count);
        List<Film> popularFilms = filmStorage.getPopularFilms(count, genreId, year);
        return popularFilms.stream()
                .map(filmMapper::toDto)
                .toList();
    }

    public void addFilmLike(Long filmId, Long userId) {
        validateFilmExists(filmId);
        validateUserExists(userId);

        log.debug("Adding like. Film: {}, User: {}", filmId, userId);
        if (!filmStorage.isLikeExists(filmId, userId)) {
            filmStorage.addLike(filmId, userId);
        } else {
            log.warn("User {} already liked film {}", userId, filmId);
        }
        feedService.logEvent(userId, EventType.LIKE, Operation.ADD, filmId);
    }

    public void removeFilmLike(Long filmId, Long userId) {
        validateFilmExists(filmId);
        validateUserExists(userId);

        log.debug("Removing like. Film: {}, User: {}", filmId, userId);

        if (!filmStorage.isLikeExists(filmId, userId)) {
            log.error("Cannot remove non-existent like. Film: {}, User: {}", filmId, userId);
            throw new NotFoundException(ErrorMessages.LIKE_NOT_EXISTS);
        }

        filmStorage.removeLike(filmId, userId);
        feedService.logEvent(userId, EventType.LIKE, Operation.REMOVE, filmId);
    }

    public List<FilmDto> getCommonFilmsSortedByPopularity(Long userId, Long friendId) {
        log.debug("List of movies sorted by popularity");
        validateUserExists(userId);
        validateUserExists(friendId);

        List<Film> commonFilms = filmStorage.getCommonFilms(userId, friendId);
        return commonFilms.stream()
                .map(filmMapper::toDto)
                .toList();
    }

    public List<FilmDto> searchFilms(String query, String by) {
        log.debug("Searching films by query: {} in fields: {}", query, by);
        List<Film> foundFilms = filmStorage.search(query, by);
        return foundFilms.stream()
                .map(filmMapper::toDto)
                .toList();
    }

    // --- Вспомогательные методы ---

    private void validateGenres(List<GenreRequestDto> genres) {
        if (genres != null && !genres.isEmpty()) {
            List<Long> ids = genres.stream().map(GenreRequestDto::getId).toList();
            validateGenreExists(ids);
        }
    }

    private void validateDirectors(List<Director> directors) {
        if (directors != null && !directors.isEmpty()) {
            List<Integer> ids = directors.stream().map(Director::getId).toList();
            validateDirectorsExists(ids);
        }
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException(ErrorMessages.releaseDateInvalid(MIN_RELEASE_DATE));
        }
    }

    private void validateMpaRating(MpaRequestDto mpaRequestDto) {
        if (mpaRequestDto == null) {
            throw new ValidationException(ErrorMessages.MPA_REQUIRED);
        }

        validateMpaExists(mpaRequestDto.getId());
    }

    private void validateFilmExists(Long filmId) {
        if (!filmStorage.isExistById(filmId)) {
            throw new NotFoundException(ErrorMessages.filmNotFound(filmId));
        }
    }

    private void validateUserExists(Long userId) {
        if (!userStorage.isExistById(userId)) {
            throw new NotFoundException(ErrorMessages.userNotFound(userId));
        }
    }

    private void validateMpaExists(Long mpaId) {
        if (!mpaStorage.isExistById(mpaId)) {
            throw new NotFoundException(ErrorMessages.mpaNotFound(mpaId));
        }
    }

    private void validateGenreExists(List<Long> genreIds) {
        if (!genreStorage.isExistByIds(genreIds)) {
            throw new NotFoundException(ErrorMessages.GENRES_NOT_FOUND);
        }
    }

    private void validateDirectorsExists(Integer directorId) {
        if (!directorStorage.isExistById(directorId)) {
            throw new NotFoundException(ErrorMessages.directorNotFound(directorId));
        }
    }

    private void validateDirectorsExists(List<Integer> directorsIds) {
        if (!directorStorage.isExistByIds(directorsIds)) {
            throw new NotFoundException(ErrorMessages.DIRECTORS_NOT_FOUND);
        }
    }
}