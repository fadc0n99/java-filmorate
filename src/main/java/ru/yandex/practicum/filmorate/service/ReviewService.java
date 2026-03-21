package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.review.CreateReviewDto;
import ru.yandex.practicum.filmorate.dto.review.ResponseReviewDto;
import ru.yandex.practicum.filmorate.dto.review.UpdateReviewDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.ReviewMapper;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.VoteType;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewDbStorage reviewDbStorage;

    private static final int DEFAULT_REVIEW_LIMIT = 10;

    private static final String ERROR_REVIEW_NOT_FOUND_MESSAGE = "Review with ID %d not found";

    public ResponseReviewDto saveReview(CreateReviewDto reviewDto) {
        Review review = ReviewMapper.toReview(reviewDto);

        review = reviewDbStorage.save(review);

        return ReviewMapper.toDto(review);
    }

    public ResponseReviewDto updateReview(UpdateReviewDto reviewDto) {
        Review review = reviewDbStorage.findById(reviewDto.getReviewId())
                .map(result -> ReviewMapper.updateFields(result, reviewDto))
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_REVIEW_NOT_FOUND_MESSAGE, reviewDto.getReviewId())
                        )
                );

        review = reviewDbStorage.update(review);

        return ReviewMapper.toDto(review);
    }

    public void deleteReview(Long id) {
        reviewDbStorage.delete(id);
    }

    public ResponseReviewDto findReviewById(Long id) {
        return reviewDbStorage.findById(id)
                .map(ReviewMapper::toDto)
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(ERROR_REVIEW_NOT_FOUND_MESSAGE, id)
                        )
                );
    }

    public List<ResponseReviewDto> findReviews(Long filmId, Integer count) {
        int limit = validateLimit(count);

        List<Review> reviewList = filmId == null ?
                reviewDbStorage.findAll(limit) :
                reviewDbStorage.findByFilmId(filmId, limit);

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
        Optional<VoteType> currentVote = reviewDbStorage.findUserVote(id, userId);

        if (currentVote.isPresent()) {
            if (currentVote.get() == voteType) {
                throw new ValidationException(
                        String.format("User %d already %s review %d", userId, voteType.name(), id)
                );
            }

            reviewDbStorage.updateVote(id, userId, voteType);
            log.info("Changed {} to {} for review {} by user {}",
                    currentVote.get().name(), voteType.name(), id, userId);
        } else {
            reviewDbStorage.addVote(id, userId, voteType);
            log.info("Added {} to review {} by user {}", voteType.name(), id, userId);
        }

        reviewDbStorage.updateReviewUseful(id);
    }

    private void processDeleteVote(Long id, Long userId, VoteType voteType) {
        Optional<VoteType> currentVote = reviewDbStorage.findUserVote(id, userId);

        if (currentVote.isEmpty()) {
            throw new NotFoundException(
                    String.format("Vote not found for review %d and user %d", id, userId)
            );
        }
        if (currentVote.get() != voteType) {
            throw new ValidationException(
                    String.format("User %d has %s on review %d, not %s",
                            userId, currentVote.get().name(), id, voteType.name())
            );
        }

        reviewDbStorage.deleteVote(id, userId, voteType);
        log.info("Deleted like from review {} by user {}", id, userId);

        reviewDbStorage.updateReviewUseful(id);
    }
}