package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.review.CreateReviewDto;
import ru.yandex.practicum.filmorate.dto.review.ResponseReviewDto;
import ru.yandex.practicum.filmorate.dto.review.UpdateReviewDto;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ResponseReviewDto> saveReview(@Valid @RequestBody CreateReviewDto reviewDto) {
        log.debug("Request received: POST /reviews - save review. Review data: {}", reviewDto);

        ResponseReviewDto responseReviewDto = reviewService.saveReview(reviewDto);

        log.info("Review created successfully. ID: {}, Film ID: {}",
                responseReviewDto.getReviewId(), responseReviewDto.getFilmId());

        return new ResponseEntity<>(responseReviewDto, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<ResponseReviewDto> updateReview(@Valid @RequestBody UpdateReviewDto reviewDto) {
        log.debug("Request received: PUT /reviews - update review. Review ID: {}, Review data: {}",
                reviewDto.getReviewId(), reviewDto);

        ResponseReviewDto responseReviewDto = reviewService.updateReview(reviewDto);

        log.info("Review updated successfully. ID: {}, Film ID: {}",
                responseReviewDto.getReviewId(), responseReviewDto.getFilmId());

        return new ResponseEntity<>(responseReviewDto, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable @NotNull @Positive Long id) {
        log.debug("Request received: DELETE /reviews/{} - delete review", id);

        reviewService.deleteReview(id);

        log.info("Review deleted successfully. ID {}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseReviewDto> getReview(@PathVariable @NotNull @Positive Long id) {
        log.debug("Request received: GET /reviews/{} - get review details", id);

        ResponseReviewDto responseReviewDto = reviewService.findReviewById(id);

        log.info("Review retrieved successfully. ID: {}, Film ID: {}",
                id, responseReviewDto.getFilmId());

        return new ResponseEntity<>(responseReviewDto, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ResponseReviewDto>> getReviewList(
            @RequestParam(required = false) @Positive Long filmId,
            @RequestParam(required = false) @Positive Integer count) {
        log.debug("Request received: GET /reviews - get review list");

        List<ResponseReviewDto> responseReviewDtoList = reviewService.findReviews(filmId, count);

        log.info("Retrieved {} reviews", responseReviewDtoList.size());

        return new ResponseEntity<>(responseReviewDtoList, HttpStatus.OK);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addReviewLike(
            @PathVariable @NotNull @Positive Long id,
            @PathVariable @NotNull @Positive Long userId) {
        log.debug("Request received: PUT /reviews/{}/like/{} - add like", id, userId);

        reviewService.addLike(id, userId);

        log.info("Like added successfully. Review ID: {}, User ID: {}", id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteReviewLike(
            @PathVariable @NotNull @Positive Long id,
            @PathVariable @NotNull @Positive Long userId) {
        log.info("Request received: DELETE /reviews/{}/like/{} - remove like", id, userId);

        reviewService.deleteLike(id, userId);

        log.info("Like removed successfully. Review ID: {}, User ID: {}", id, userId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addReviewDislike(
            @PathVariable @NotNull @Positive Long id,
            @PathVariable @NotNull @Positive Long userId) {
        log.debug("Request received: PUT /reviews/{}/dislike/{} - add dislike", id, userId);

        reviewService.addDislike(id, userId);

        log.info("Dislike added successfully. Review ID: {}, User ID: {}", id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void deleteReviewDislike(
            @PathVariable @NotNull @Positive Long id,
            @PathVariable @NotNull @Positive Long userId) {
        log.debug("Request received: DELETE /reviews/{}/dislike/{} - remove dislike", id, userId);

        reviewService.deleteDislike(id, userId);

        log.info("Dislike removed successfully. Review ID: {}, User ID: {}", id, userId);
    }
}