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
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaRequestDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final GenreService genreService;
    private final MpaService mpaService;
    private final DirectorService directorService;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final String ERROR_INVALID_ID_MESSAGE = "Invalid film ID: must be greater than 0";
    private static final String ERROR_FILM_NOT_FOUND_MESSAGE = "Film with ID %d not found";

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       UserService userService,
                       GenreService genreService,
                       MpaService mpaService,
                       DirectorService directorService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.genreService = genreService;
        this.mpaService = mpaService;
        this.directorService = directorService;
    }

    public List<FilmDto> findAllFilms() {
        log.debug("Retrieving all films from storage");
        List<Film> films = filmStorage.findAll();
        return convertToDtos(films);
    }

    @Transactional
    public FilmDto createFilm(CreateFilmDto createFilmDto) {
        log.debug("Starting add film: {}", createFilmDto);
        validateMpaRating(createFilmDto.getMpa());

        List<GenreRequestDto> processedGenres = processGenres(createFilmDto.getGenres());
        createFilmDto.setGenres(processedGenres);

        Film film = FilmMapper.toFilm(createFilmDto);
        validateReleaseDate(film);

        film = filmStorage.save(film);
        return convertToDto(film);
    }

    @Transactional
    public FilmDto updateFilm(UpdateFilmDto updateFilmDto) {
        requireValidFilm(updateFilmDto.getId());
        validateMpaRating(updateFilmDto.getMpa());

        List<GenreRequestDto> processedGenres = processGenres(updateFilmDto.getGenres());
        updateFilmDto.setGenres(processedGenres);

        Film updatedFilm = filmStorage.findFilmById(updateFilmDto.getId())
                .map(film -> FilmMapper.updateFilmFields(film, updateFilmDto))
                .orElseThrow(() -> new NotFoundException(String.format(ERROR_FILM_NOT_FOUND_MESSAGE, updateFilmDto.getId())));

        validateReleaseDate(updatedFilm);
        updatedFilm = filmStorage.update(updatedFilm);

        return convertToDto(updatedFilm);
    }

    public FilmDto getFilm(long id) {
        requireValidFilm(id);
        return filmStorage.findFilmById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new NotFoundException(String.format(ERROR_FILM_NOT_FOUND_MESSAGE, id)));
    }

    public List<FilmDto> getFilmsByDirector(Integer directorId, String sortBy) {
        directorService.requireExists(directorId);

        log.debug("Retrieving films for director ID: {} sorted by {}", directorId, sortBy);
        List<Film> films = filmStorage.findAllByDirector(directorId, sortBy);

        return convertToDtos(films);
    }

    public List<FilmDto> getPopularFilms(long count) {
        log.debug("Getting top {} popular films", count);
        List<Film> popularFilms = filmStorage.getPopularFilms(count);
        return convertToDtos(popularFilms);
    }

    public void addFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);
        userService.requireValidUser(userId);
        if (isLikeExists(filmId, userId)) {
            throw new ValidationException("User has already liked this film");
        }
        filmStorage.addLike(filmId, userId);
    }

    public void removeFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);
        userService.requireValidUser(userId);
        if (!isLikeExists(filmId, userId)) {
            throw new ValidationException("User hasn't liked this film");
        }
        filmStorage.removeLike(filmId, userId);
    }

    // --- Вспомогательные методы ---

    private FilmDto convertToDto(Film film) {
        List<Genre> genres = genreService.findGenresByIds(film.getGenresIds());
        MpaDto mpaDto = mpaService.findMpaById(film.getMpaId());
        return FilmMapper.toDto(film, mpaDto, genres);
    }

    private List<FilmDto> convertToDtos(List<Film> films) {
        if (films.isEmpty()) return Collections.emptyList();

        List<Long> filmIds = films.stream().map(Film::getId).toList();
        Map<Long, List<Genre>> filmsGenres = genreService.getGenresByFilmIds(filmIds);
        Map<Long, MpaDto> filmsMpa = mpaService.getMpaByFilmIds(filmIds);

        return films.stream()
                .map(film -> {
                    List<Genre> genres = filmsGenres.getOrDefault(film.getId(), Collections.emptyList());
                    MpaDto mpaDto = filmsMpa.get(film.getId());
                    return FilmMapper.toDto(film, mpaDto, genres);
                })
                .toList();
    }

    private List<GenreRequestDto> processGenres(List<GenreRequestDto> genres) {
        if (genres == null || genres.isEmpty()) return Collections.emptyList();
        List<Long> ids = genres.stream().map(GenreRequestDto::getId).toList();
        if (!genreService.isGenresExist(ids)) {
            throw new NotFoundException("One or more genres not found");
        }
        return genres;
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Release date must be after " + MIN_RELEASE_DATE);
        }
    }

    private void validateMpaRating(MpaRequestDto mpaRequestDto) {
        if (mpaRequestDto == null) throw new ValidationException("MPA rating is required");
        if (!mpaService.isMpaExist(mpaRequestDto.getId())) {
            throw new NotFoundException("MPA rating not found");
        }
    }

    public void requireValidFilm(long filmId) {
        if (filmId <= 0) throw new ValidationException(ERROR_INVALID_ID_MESSAGE);
        if (!filmStorage.isExistById(filmId)) {
            throw new NotFoundException(String.format(ERROR_FILM_NOT_FOUND_MESSAGE, filmId));
        }
    }

    private boolean isLikeExists(long filmId, long userId) {
        return filmStorage.isLikeExists(filmId, userId);
    }
}