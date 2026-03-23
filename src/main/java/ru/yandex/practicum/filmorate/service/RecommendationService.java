package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
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

    private final FilmStorage filmStorage;
    private final FilmMapper filmMapper;
    private final UserStorage userStorage;

    @Autowired
    public RecommendationService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                                 FilmMapper filmMapper,
                                 @Qualifier("userDbStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.filmMapper = filmMapper;
        this.userStorage = userStorage;
    }

    public List<FilmDto> getFilmRecommendationsByUserLikes(long userId) {
        validateUserExists(userId);

        List<Long> userLikes = filmStorage.findUserLikedFilmIds(userId);
        log.debug("Target user with id: {} has {} likes: {}", userId, userLikes.size(), userLikes);

        List<Long> recommendations = Collections.emptyList();

        if (!userLikes.isEmpty()) {
            Map<Long, List<Long>> otherUsersLikes = filmStorage.findAllUsersLikedFilmIds();

            if (!otherUsersLikes.isEmpty()) {
                recommendations = findMostSimilarLikeIds(otherUsersLikes, userId, userLikes);
            } else {
                log.warn("No other users with likes found for user id: {}", userId);
            }
        }

        return filmStorage.findFilmsByIds(recommendations)
                .stream()
                .map(filmMapper::toDto)
                .toList();

    }

    private List<Long> findMostSimilarLikeIds(
            Map<Long, List<Long>> otherUsersLikes,
            Long userId,
            List<Long> userLikes) {

        List<Map.Entry<List<Long>, Long>> sortedOtherLikesByIntersections = otherUsersLikes.entrySet()
                .stream()
                .filter(entry -> !Objects.equals(entry.getKey(), userId))
                .map(entry -> Map.entry(entry.getValue(), getLikeCountIntersections(entry.getValue(), userLikes)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .toList();

        for (Map.Entry<List<Long>, Long> otherLikesEntry : sortedOtherLikesByIntersections) {
            List<Long> likeList = otherLikesEntry.getKey();
            long intersectionCount = otherLikesEntry.getValue();
            log.debug("Checking user with {} common likes. Has likes: {}", intersectionCount, likeList);

            List<Long> recommendations = likeList.stream()
                    .filter(filmId -> !userLikes.contains(filmId))
                    .collect(Collectors.toList());

            if (!recommendations.isEmpty()) {
                log.debug("Found recommendations likes: {}", recommendations);
                return recommendations;
            }
        }

        log.debug("No recommendations found for userId: {}", userId);
        return Collections.emptyList();
    }

    private long getLikeCountIntersections(List<Long> otherUserLikes, List<Long> userLikeFilmIds) {
        return otherUserLikes.stream()
                .filter(userLikeFilmIds::contains)
                .count();
    }

    private void validateUserExists(long userId) {
        if (!userStorage.isExistById(userId)) {
            throw new NotFoundException(ErrorMessages.userNotFound(userId));
        }
    }
}
