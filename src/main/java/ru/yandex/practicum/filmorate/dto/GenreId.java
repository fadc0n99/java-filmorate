package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenreId {
    @Positive
    private Long id;

    @JsonCreator
    public GenreId(@JsonProperty("id") Long id) {
        this.id = id;
    }

    public static GenreId of(long id) {
        return GenreId.builder().id(id).build();
    }
}
