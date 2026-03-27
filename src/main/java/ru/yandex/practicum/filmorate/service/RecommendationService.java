package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.utils.ValidationEntityUtils;

import java.util.List;

@Service
@Slf4j
public class RecommendationService {

    private final FilmStorage filmStorage;
    private final ValidationEntityUtils validationEntityUtils;

    @Autowired
    public RecommendationService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                                 ValidationEntityUtils validationEntityUtils) {
        this.filmStorage = filmStorage;
        this.validationEntityUtils = validationEntityUtils;
    }

    public List<FilmDto> getFilmRecommendationsByUserLikes(long userId) {
        validationEntityUtils.validateUserExists(userId);

        List<Film> recommendations = filmStorage.findRecommendationsByUserId(userId);
        log.debug("Found recommendations films: {}", recommendations);

        return recommendations.stream()
                .map(FilmMapper::toDto)
                .toList();
    }
}
