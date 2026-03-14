package ru.yandex.practicum.filmorate.dto.genre;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenreRequestDto {
    @Positive
    private Long id;

    @JsonCreator
    public GenreRequestDto(@JsonProperty("id") Long id) {
        this.id = id;
    }

    public static GenreRequestDto of(long id) {
        return GenreRequestDto.builder().id(id).build();
    }
}
