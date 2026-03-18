package ru.yandex.practicum.filmorate.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReviewDto {
    @NotBlank
    @Size(min = 1, max = 200)
    private String content;
    @NotNull
    private Boolean isPositive;
    @NotNull
    @Positive
    private Long userId;
    @NotNull
    @Positive
    private Long filmId;
}
