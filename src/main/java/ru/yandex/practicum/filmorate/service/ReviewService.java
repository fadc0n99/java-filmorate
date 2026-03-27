package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.review.CreateReviewDto;
import ru.yandex.practicum.filmorate.dto.review.ResponseReviewDto;
import ru.yandex.practicum.filmorate.dto.review.UpdateReviewDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.ReviewMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.VoteType;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.utils.ValidationUtils;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final FeedService feedService;
    private final ValidationUtils validationUtils;

    public ReviewService(ReviewStorage reviewStorage,
                         ValidationUtils validationUtils,
                         FeedService feedService) {
        this.reviewStorage = reviewStorage;
        this.validationUtils = validationUtils;
        this.feedService = feedService;
    }

    private static final int DEFAULT_REVIEW_LIMIT = 10;

    @Transactional
    public ResponseReviewDto saveReview(CreateReviewDto reviewDto) {
        validationUtils.validateNoDuplicateReview(reviewDto.getUserId(), reviewDto.getFilmId());
        validationUtils.validateUserExists(reviewDto.getUserId());
        validationUtils.validateFilmExists(reviewDto.getFilmId());

        Review review = ReviewMapper.toEntity(reviewDto);
        review = reviewStorage.save(review);

        feedService.saveEvent(
                review.getUserId(),
                EventType.REVIEW,
                Operation.ADD,
                review.getReviewId()
        );

        return ReviewMapper.toDto(review);
    }

    @Transactional
    public ResponseReviewDto updateReview(UpdateReviewDto reviewDto) {
        Review review = reviewStorage.findById(reviewDto.getReviewId())
                .map(result -> ReviewMapper.updateFields(result, reviewDto))
                .orElseThrow(
                        () -> new NotFoundException(ErrorMessages.reviewNotFound(reviewDto.getReviewId()))
                );

        review = reviewStorage.update(review);

        feedService.saveEvent(
                review.getUserId(),
                EventType.REVIEW,
                Operation.UPDATE,
                review.getReviewId()
        );

        return ReviewMapper.toDto(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.reviewNotFound(id)));

        reviewStorage.delete(id);

        feedService.saveEvent(
                review.getUserId(),
                EventType.REVIEW,
                Operation.REMOVE,
                id
        );
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
        validationUtils.validateReviewExists(id);

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
        validationUtils.validateReviewExists(id);

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
}

