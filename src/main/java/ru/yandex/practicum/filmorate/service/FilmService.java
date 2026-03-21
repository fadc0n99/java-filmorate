package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
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
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    // TODO избавиться от зависимости сервисов друг от друг
    private final GenreService genreService;
    private final MpaService mpaService;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       GenreService genreService,
                       MpaService mpaService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
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

        return convertToDtos(films);
    }

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

    public FilmDto updateFilm(UpdateFilmDto updateFilmDto) {
        requireValidFilm(updateFilmDto.getId());
        validateMpaRating(updateFilmDto.getMpa());

        List<GenreRequestDto> processedGenres = processGenres(updateFilmDto.getGenres());
        updateFilmDto.setGenres(processedGenres);

        Film updatedFilm = filmStorage.findFilmById(updateFilmDto.getId())
                .map(film -> FilmMapper.updateFilmFields(film, updateFilmDto))
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_FILM_NOT_FOUND_MESSAGE, updateFilmDto.getId())
                        )
                );

        validateReleaseDate(updatedFilm);

        updatedFilm = filmStorage.update(updatedFilm);

        return convertToDto(updatedFilm);
    }

    private List<GenreRequestDto> processGenres(List<GenreRequestDto> genres) {
        if (genres == null || genres.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> requestGenreIds = genres.stream()
                .map(GenreRequestDto::getId)
                .toList();

        if (!genreService.isGenresExist(requestGenreIds)) {
            throw new NotFoundException("One or more genres not found");
        }

        return genres;
    }

    public FilmDto getFilm(long id) {
        requireValidFilm(id);

        log.debug("Retrieving film by ID: {}", id);

        return filmStorage.findFilmById(id)
                .map(this::convertToDto)
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_FILM_NOT_FOUND_MESSAGE, id)
                        )
                );
    }

    // TODO вынести в отдельный конвертер из сервиса
    public FilmDto convertToDto(Film film) {
        List<Genre> genres = genreService.findGenresByIds(film.getGenresIds());
        MpaDto mpaDto = mpaService.findMpaById(film.getMpaId());

        return FilmMapper.toDto(film, mpaDto, genres);
    }

    // TODO вынести в отдельный конвертер из сервиса
    public List<FilmDto> convertToDtos(List<Film> films) {
        List<Long> filmIds = extractFilmIds(films);

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

    private List<Long> extractFilmIds(List<Film> films) {
        return films.stream()
                .map(Film::getId)
                .toList();
    }

    public void addFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);

        log.debug("Adding like. Film: {}, User: {}", filmId, userId);

        if (userStorage.isExistById(userId) && isLikeExists(filmId, userId)) {
            log.error("User {} already liked film {}", userId, filmId);
            throw new ValidationException("User has already liked this film");
        }

        filmStorage.addLike(filmId, userId);
    }

    public void removeFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);

        log.debug("Removing like. Film: {}, User: {}", filmId, userId);

        if (userStorage.isExistById(userId) && !isLikeExists(filmId, userId)) {
            log.error("Cannot remove non-existent like. Film: {}, User: {}", filmId, userId);
            throw new ValidationException("User hasn't liked this film");
        }

        filmStorage.removeLike(filmId, userId);
    }

    private boolean isFilmExists(long filmId) {
        return filmStorage.isExistById(filmId);
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException(ERROR_RELEASE_DATE_MESSAGE);
        }
    }

    private boolean isLikeExists(long filmId, long userId) {
        return filmStorage.isLikeExists(filmId, userId);
    }

    public List<FilmDto> getPopularFilms(long count) {
        log.debug("Getting top {} popular films", count);

        List<Film> popularFilms = filmStorage.getPopularFilms(count);
        return convertToDtos(popularFilms);
    }

    public List<FilmDto> getCommonFilmsSortedByPopularity(Long userId, Long friendId) {
        log.debug("list of movies sorted by popularity");
        userService.requireValidUser(userId);
        userService.requireValidUser(friendId);


        List<Film> popularFilms = filmStorage.getCommonFilms(userId, friendId);
        return convertToDtos(popularFilms);
    }

    public void requireValidFilm(long filmId) {
        if (filmId <= 0) {
            throw new ValidationException(ERROR_INVALID_ID_MESSAGE);
        }
        if (!isFilmExists(filmId)) {
            throw new NotFoundException(String.format(ERROR_FILM_NOT_FOUND_MESSAGE, filmId));
        }
    }

    private void validateMpaRating(MpaRequestDto mpaRequestDto) {
        if (mpaRequestDto == null) {
            throw new ValidationException("MPA rating is required for film");
        }

        if (!mpaService.isMpaExist(mpaRequestDto.getId())) {
            throw new NotFoundException(
                    String.format("MPA rating with ID %d not found", mpaRequestDto.getId())
            );
        }
    }
}
