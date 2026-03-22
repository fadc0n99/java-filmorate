package ru.yandex.practicum.filmorate.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewValidator extends BaseEntityValidator {

    private final ReviewDbStorage reviewDbStorage;

    private static final String ERROR_REVIEW_ALREADY_EXISTS =
            "User %d already reviewed film %d";

    @Override
    protected boolean exists(Long id) {
        return reviewDbStorage.isExistById(id);
    }

    @Override
    protected String getEntityName() {
        return "Review";
    }

    public void validateNotExistsByUserAndFilm(Long userId, Long filmId) {
        if (reviewDbStorage.existsByUserAndFilm(userId, filmId)) {
            log.warn("User {} already reviewed film {}", userId, filmId);
            throw new ValidationException(
                    String.format(ERROR_REVIEW_ALREADY_EXISTS, userId, filmId)
            );
        }
    }
}
