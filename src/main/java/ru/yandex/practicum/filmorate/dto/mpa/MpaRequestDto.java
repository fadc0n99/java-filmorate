package ru.yandex.practicum.filmorate.dto.mpa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MpaRequestDto {
    @Positive
    private Long id;

    @JsonCreator
    public MpaRequestDto(@JsonProperty("id") Long id) {
        this.id = id;
    }

    public static MpaRequestDto of(long id) {
        return MpaRequestDto.builder().id(id).build();
    }
}
