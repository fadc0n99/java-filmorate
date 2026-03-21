package ru.yandex.practicum.filmorate.validation;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

@Slf4j
public abstract class BaseEntityValidator {

    protected abstract boolean exists(Long id);

    protected abstract String getEntityName();

    public void validateExists(Long id) {
        if (!exists(id)) {
            throw new NotFoundException(
                    String.format("%s with ID %d does not exist", getEntityName(), id)
            );
        }
    }
}
