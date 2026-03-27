package ru.yandex.practicum.filmorate.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.genre.GenreRequestDto;
import ru.yandex.practicum.filmorate.dto.mpa.MpaRequestDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Component
public class ValidationUtils {

    private final FilmStorage filmStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;
    private final UserStorage userStorage;
    private final DirectorStorage directorStorage;
    private final ReviewStorage reviewStorage;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public ValidationUtils(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       GenreStorage genreStorage,
                       MpaStorage mpaStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                           ReviewStorage reviewStorage,
                       DirectorStorage directorStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.directorStorage = directorStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
        this.reviewStorage = reviewStorage;
    }

    public void validateMinFilmDate(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException(ErrorMessages.releaseDateInvalid(MIN_RELEASE_DATE));
        }
    }

    public void validateMpaRating(MpaRequestDto mpaRequestDto) {
        if (mpaRequestDto == null) {
            throw new ValidationException(ErrorMessages.MPA_REQUIRED);
        }

        validateMpaExists(mpaRequestDto.getId());
    }

    public void validateFilmExists(Long filmId) {
        if (!filmStorage.isExistById(filmId)) {
            throw new NotFoundException(ErrorMessages.filmNotFound(filmId));
        }
    }

    public void validateUserExists(Long userId) {
        if (!userStorage.isExistById(userId)) {
            throw new NotFoundException(ErrorMessages.userNotFound(userId));
        }
    }

    public void validateMpaExists(Long mpaId) {
        if (!mpaStorage.isExistById(mpaId)) {
            throw new NotFoundException(ErrorMessages.mpaNotFound(mpaId));
        }
    }

    public void validateGenreExists(List<Long> genreIds) {
        if (!genreStorage.isExistByIds(genreIds)) {
            throw new NotFoundException(ErrorMessages.GENRES_NOT_FOUND);
        }
    }

    public void validateDirectorExists(Integer directorId) {
        if (!directorStorage.isExistById(directorId)) {
            throw new NotFoundException(ErrorMessages.directorNotFound(directorId));
        }
    }

    public void validateDirectorsExists(List<Integer> directorsIds) {
        if (!directorStorage.isExistByIds(directorsIds)) {
            throw new NotFoundException(ErrorMessages.DIRECTORS_NOT_FOUND);
        }
    }

    public void validateGenres(List<GenreRequestDto> genres) {
        if (genres != null && !genres.isEmpty()) {
            List<Long> ids = genres.stream()
                    .map(GenreRequestDto::getId)
                    .toList();
            validateGenreExists(ids);
        }
    }

    public void validateDirectors(List<Director> directors) {
        if (directors != null && !directors.isEmpty()) {
            List<Integer> ids = directors.stream()
                    .map(Director::getId)
                    .toList();
            validateDirectorsExists(ids);
        }
    }

    public void validateNoDuplicateReview(long userId, long filmId) {
        if (reviewStorage.existsByUserAndFilm(userId, filmId)) {
            throw new ValidationException(ErrorMessages.REVIEW_ALREADY_EXISTS);
        }
    }

    public void validateReviewExists(long reviewId) {
        if (!reviewStorage.isExistById(reviewId)) {
            throw new NotFoundException(ErrorMessages.reviewNotFound(reviewId));
        }
    }
}
