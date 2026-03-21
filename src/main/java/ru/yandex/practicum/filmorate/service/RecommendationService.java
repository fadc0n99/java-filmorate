package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationService {

    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    // TODO избавиться от зависимости, когда будет отдельный конвертер для ДТО
    private final FilmService filmService;

    @Autowired
    public RecommendationService(@Qualifier("userDbStorage") UserStorage userStorage,
                                 @Qualifier("filmDbStorage") FilmStorage filmStorage,
                                 FilmService filmService) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
        this.filmService = filmService;
    }

    public List<FilmDto> getFilmRecommendationsByUserLikes(long userId) {
        if (!userStorage.isExistById(userId)) {
            throw new NotFoundException("user with id " + userId + " not found");
        }

        List<Long> userLikes = filmStorage.findUserLikedFilmIds(userId);
        List<Long> recommendations = null;

        if (!userLikes.isEmpty()) {
            Map<Long, List<Long>> otherUsersLikes = filmStorage.findAllUsersLikedFilmIds();

            if (!otherUsersLikes.isEmpty()) {
                recommendations = findMostSimilarLikeIds(otherUsersLikes, userId, userLikes);
            }
        }

        return filmService.convertToDtos(filmStorage.findFilmsByIds(recommendations));

    }

    private List<Long> findMostSimilarLikeIds(
            Map<Long, List<Long>> otherUsersLikes,
            Long userId,
            List<Long> userLikes) {

        List<Map.Entry<List<Long>, Long>> sortedOtherLikesByIntersections = otherUsersLikes.entrySet()
                .stream()
                .filter(entry -> !Objects.equals(entry.getKey(), userId))
                .map(entry -> Map.entry(entry.getValue(), getLikeIntersections(entry.getValue(), userLikes)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .toList();

        for (Map.Entry<List<Long>, Long> otherLikesEntry : sortedOtherLikesByIntersections) {
            List<Long> likeList = otherLikesEntry.getKey();

            List<Long> recommendations = likeList.stream()
                    .filter(filmId -> !userLikes.contains(filmId))
                    .collect(Collectors.toList());

            if (!recommendations.isEmpty()) {
                return recommendations;
            }
        }

        return Collections.emptyList();
    }

    private long getLikeIntersections(List<Long> otherUserLikes, List<Long> userLikeFilmIds) {
        return otherUserLikes.stream()
                .filter(userLikeFilmIds::contains)
                .count();
    }
}
