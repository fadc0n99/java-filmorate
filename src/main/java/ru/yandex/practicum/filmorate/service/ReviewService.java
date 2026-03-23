package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.review.CreateReviewDto;
import ru.yandex.practicum.filmorate.dto.review.ResponseReviewDto;
import ru.yandex.practicum.filmorate.dto.review.UpdateReviewDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.ReviewMapper;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.VoteType;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;

    public ReviewService(ReviewStorage reviewStorage,
                         @Qualifier("userDbStorage") UserStorage userStorage,
                         @Qualifier("filmDbStorage") FilmStorage filmStorage) {
        this.reviewStorage = reviewStorage;
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
    }

    private static final int DEFAULT_REVIEW_LIMIT = 10;

    public ResponseReviewDto saveReview(CreateReviewDto reviewDto) {
        validateNoDuplicateReview(reviewDto.getUserId(), reviewDto.getFilmId());
        validateUserExists(reviewDto.getUserId());
        validateFilmExists(reviewDto.getFilmId());

        Review review = ReviewMapper.toEntity(reviewDto);
        review = reviewStorage.save(review);
        return ReviewMapper.toDto(review);
    }

    public ResponseReviewDto updateReview(UpdateReviewDto reviewDto) {
        Review review = reviewStorage.findById(reviewDto.getReviewId())
                .map(result -> ReviewMapper.updateFields(result, reviewDto))
                .orElseThrow(
                        () -> new NotFoundException(ErrorMessages.reviewNotFound(reviewDto.getReviewId()))
                );

        review = reviewStorage.update(review);
        return ReviewMapper.toDto(review);
    }

    public void deleteReview(Long id) {
        reviewStorage.delete(id);
    }

    public ResponseReviewDto findReviewById(Long id) {
        return reviewStorage.findById(id)
                .map(ReviewMapper::toDto)
                .orElseThrow(
                        () -> new NotFoundException(ErrorMessages.reviewNotFound(id))
                );
    }

    public List<ResponseReviewDto> findReviews(Long filmId, Integer count) {
        int limit = validateLimit(count);

        List<Review> reviewList = filmId == null ?
                reviewStorage.findAll(limit) :
                reviewStorage.findByFilmId(filmId, limit);

        return convertToDtos(reviewList);
    }

    private List<ResponseReviewDto> convertToDtos(List<Review> reviewList) {
        return reviewList.stream()
                .map(ReviewMapper::toDto)
                .toList();
    }

    private int validateLimit(Integer count) {
        return count == null ? DEFAULT_REVIEW_LIMIT : count;
    }

    public void addLike(Long id, Long userId) {
        processAddVote(id, userId, VoteType.LIKE);
    }

    public void addDislike(Long id, Long userId) {
        processAddVote(id, userId, VoteType.DISLIKE);
    }

    public void deleteLike(Long id, Long userId) {
        processDeleteVote(id, userId, VoteType.LIKE);
    }

    public void deleteDislike(Long id, Long userId) {
        processDeleteVote(id, userId, VoteType.DISLIKE);
    }

    private void processAddVote(Long id, Long userId, VoteType voteType) {
        validateReviewExists(id);

        Optional<VoteType> currentVote = reviewStorage.findUserVote(id, userId);
        if (currentVote.isPresent()) {
            if (currentVote.get() != voteType) {
                log.info("Changed {} to {} for review {} by user {}",
                        currentVote.get().name(), voteType.name(), id, userId);
            }
            reviewStorage.updateVote(id, userId, voteType);
        } else {
            reviewStorage.addVote(id, userId, voteType);
            log.info("Added {} to review {} by user {}", voteType.name(), id, userId);
        }

        reviewStorage.updateReviewUseful(id);
    }

    private void processDeleteVote(Long id, Long userId, VoteType voteType) {
        validateReviewExists(id);

        Optional<VoteType> currentVote = reviewStorage.findUserVote(id, userId);
        if (currentVote.isEmpty()) {
            throw new NotFoundException(ErrorMessages.voteNotFound(id, userId));
        }
        if (currentVote.get() != voteType) {
            throw new ValidationException(
                    ErrorMessages.notMatchReviewType(userId, id, currentVote.get().name(), voteType.name())
            );
        }

        reviewStorage.deleteVote(id, userId, voteType);
        log.info("Deleted like from review {} by user {}", id, userId);

        reviewStorage.updateReviewUseful(id);
    }

    private void validateFilmExists(long filmId) {
        if (!filmStorage.isExistById(filmId)) {
            throw new NotFoundException(ErrorMessages.filmNotFound(filmId));
        }
    }

    private void validateUserExists(long userId) {
        if (!userStorage.isExistById(userId)) {
            throw new NotFoundException(ErrorMessages.userNotFound(userId));
        }
    }

    private void validateReviewExists(long reviewId) {
        if (!reviewStorage.isExistById(reviewId)) {
            throw new NotFoundException(ErrorMessages.reviewNotFound(reviewId));
        }
    }

    private void validateNoDuplicateReview(long userId, long filmId) {
        if (reviewStorage.existsByUserAndFilm(userId, filmId)) {
            throw new ValidationException(ErrorMessages.REVIEW_ALREADY_EXISTS);
        }
    }
}