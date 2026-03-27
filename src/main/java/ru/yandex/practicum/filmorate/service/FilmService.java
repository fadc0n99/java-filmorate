package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.film.UpdateFilmDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.utils.ValidationUtils;

import java.util.*;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final FilmMapper filmMapper;
    private final FeedService feedService;
    private final ValidationUtils validationUtils;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       FilmMapper filmMapper,
                       ValidationUtils validationUtils,
                       FeedService feedService) {
        this.filmStorage = filmStorage;
        this.filmMapper = filmMapper;
        this.validationUtils = validationUtils;
        this.feedService = feedService;
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
        validationUtils.validateMpaRating(createFilmDto.getMpa());
        validationUtils.validateGenres(createFilmDto.getGenres());
        validationUtils.validateDirectors(createFilmDto.getDirectors());

        Film film = FilmMapper.toEntity(createFilmDto);
        validationUtils.validateMinFilmDate(film);

        film = filmStorage.save(film);
        return filmMapper.toDto(film);
    }

    @Transactional
    public FilmDto updateFilm(UpdateFilmDto updateFilmDto) {
        validationUtils.validateMpaRating(updateFilmDto.getMpa());
        validationUtils.validateGenres(updateFilmDto.getGenres());
        validationUtils.validateDirectors(updateFilmDto.getDirectors());

        Film updatedFilm = filmStorage.findFilmById(updateFilmDto.getId())
                .map(film -> FilmMapper.updateFilmFields(film, updateFilmDto))
                .orElseThrow(() -> new NotFoundException(ErrorMessages.filmNotFound(updateFilmDto.getId())));

        validationUtils.validateMinFilmDate(updatedFilm);
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
        validationUtils.validateFilmExists(filmId);
        filmStorage.delete(filmId);
        log.info("Film {} deleted successfully", filmId);
    }

    public List<FilmDto> getFilmsByDirector(Integer directorId, String sortBy) {
        validationUtils.validateDirectorExists(directorId);

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
        validationUtils.validateFilmExists(filmId);
        validationUtils.validateUserExists(userId);

        log.debug("Adding like. Film: {}, User: {}", filmId, userId);
        if (!filmStorage.isLikeExists(filmId, userId)) {
            filmStorage.addLike(filmId, userId);
        } else {
            log.warn("User {} already liked film {}", userId, filmId);
        }
        feedService.saveEvent(userId, EventType.LIKE, Operation.ADD, filmId);
    }

    public void removeFilmLike(Long filmId, Long userId) {
        validationUtils.validateFilmExists(filmId);
        validationUtils.validateUserExists(userId);

        log.debug("Removing like. Film: {}, User: {}", filmId, userId);

        if (!filmStorage.isLikeExists(filmId, userId)) {
            log.error("Cannot remove non-existent like. Film: {}, User: {}", filmId, userId);
            throw new NotFoundException(ErrorMessages.LIKE_NOT_EXISTS);
        }

        filmStorage.removeLike(filmId, userId);
        feedService.saveEvent(userId, EventType.LIKE, Operation.REMOVE, filmId);
    }

    public List<FilmDto> getCommonFilmsSortedByPopularity(Long userId, Long friendId) {
        log.debug("List of movies sorted by popularity");
        validationUtils.validateUserExists(userId);
        validationUtils.validateUserExists(friendId);

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
}