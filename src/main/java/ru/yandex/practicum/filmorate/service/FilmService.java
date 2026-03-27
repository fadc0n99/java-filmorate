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
import ru.yandex.practicum.filmorate.utils.ValidationEntityUtils;

import java.util.*;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final FeedService feedService;
    private final ValidationEntityUtils validationEntityUtils;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       ValidationEntityUtils validationEntityUtils,
                       FeedService feedService) {
        this.filmStorage = filmStorage;
        this.validationEntityUtils = validationEntityUtils;
        this.feedService = feedService;
    }

    public List<FilmDto> findAllFilms() {
        log.debug("Retrieving all films from storage");
        List<Film> films = filmStorage.findAll();
        return films.stream()
                .map(FilmMapper::toDto)
                .toList();
    }

    @Transactional
    public FilmDto createFilm(CreateFilmDto createFilmDto) {
        log.debug("Starting add film: {}", createFilmDto);
        validationEntityUtils.validateMpaRating(createFilmDto.getMpa());
        validationEntityUtils.validateGenres(createFilmDto.getGenres());
        validationEntityUtils.validateDirectors(createFilmDto.getDirectors());

        Film film = FilmMapper.toEntity(createFilmDto);
        validationEntityUtils.validateMinFilmDate(film);

        film = filmStorage.save(film);
        return FilmMapper.toDto(film);
    }

    @Transactional
    public FilmDto updateFilm(UpdateFilmDto updateFilmDto) {
        validationEntityUtils.validateMpaRating(updateFilmDto.getMpa());
        validationEntityUtils.validateGenres(updateFilmDto.getGenres());
        validationEntityUtils.validateDirectors(updateFilmDto.getDirectors());

        Film updatedFilm = filmStorage.findFilmById(updateFilmDto.getId())
                .map(film -> FilmMapper.updateFilmFields(film, updateFilmDto))
                .orElseThrow(() -> new NotFoundException(ErrorMessages.filmNotFound(updateFilmDto.getId())));

        validationEntityUtils.validateMinFilmDate(updatedFilm);
        updatedFilm = filmStorage.update(updatedFilm);

        return FilmMapper.toDto(updatedFilm);
    }

    public FilmDto getFilm(Long id) {
        log.debug("Retrieving film by ID: {}", id);

        return filmStorage.findFilmById(id)
                .map(FilmMapper::toDto)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.filmNotFound(id)));
    }

    @Transactional
    public void deleteFilm(Long filmId) {
        log.debug("Deleting film with ID: {}", filmId);
        validationEntityUtils.validateFilmExists(filmId);
        filmStorage.delete(filmId);
        log.info("Film {} deleted successfully", filmId);
    }

    public List<FilmDto> getFilmsByDirector(Integer directorId, String sortBy) {
        validationEntityUtils.validateDirectorExists(directorId);

        log.debug("Retrieving films for director ID: {} sorted by {}", directorId, sortBy);
        List<Film> films = filmStorage.findAllByDirector(directorId, sortBy);

        return films.stream()
                .map(FilmMapper::toDto)
                .toList();
    }

    public List<FilmDto> getPopularFilms(Integer count, Integer genreId, Integer year) {
        log.debug("Getting top {} popular films", count);
        List<Film> popularFilms = filmStorage.getPopularFilms(count, genreId, year);
        return popularFilms.stream()
                .map(FilmMapper::toDto)
                .toList();
    }

    public void addFilmLike(Long filmId, Long userId) {
        validationEntityUtils.validateFilmExists(filmId);
        validationEntityUtils.validateUserExists(userId);

        log.debug("Adding like. Film: {}, User: {}", filmId, userId);
        if (!filmStorage.isLikeExists(filmId, userId)) {
            filmStorage.addLike(filmId, userId);
        } else {
            log.warn("User {} already liked film {}", userId, filmId);
        }
        feedService.saveEvent(userId, EventType.LIKE, Operation.ADD, filmId);
    }

    public void removeFilmLike(Long filmId, Long userId) {
        validationEntityUtils.validateFilmExists(filmId);
        validationEntityUtils.validateUserExists(userId);

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
        validationEntityUtils.validateUserExists(userId);
        validationEntityUtils.validateUserExists(friendId);

        List<Film> commonFilms = filmStorage.getCommonFilms(userId, friendId);
        return commonFilms.stream()
                .map(FilmMapper::toDto)
                .toList();
    }

    public List<FilmDto> searchFilms(String query, String by) {
        log.debug("Searching films by query: {} in fields: {}", query, by);
        List<Film> foundFilms = filmStorage.search(query, by);
        return foundFilms.stream()
                .map(FilmMapper::toDto)
                .toList();
    }
}