package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MpaId {
    @Positive
    private Long id;

    @JsonCreator
    public MpaId(@JsonProperty("id") Long id) {
        this.id = id;
    }

    public static MpaId of(long id) {
        return MpaId.builder().id(id).build();
    }
}
