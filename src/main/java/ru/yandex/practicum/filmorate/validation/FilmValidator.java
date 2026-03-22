package ru.yandex.practicum.filmorate.validation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

@Component
public class FilmValidator extends BaseEntityValidator {

    private final FilmStorage filmStorage;

    public FilmValidator(@Qualifier("filmDbStorage") FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    @Override
    protected boolean exists(Long id) {
        return filmStorage.isExistById(id);
    }

    @Override
    protected String getEntityName() {
        return "Film";
    }
}
