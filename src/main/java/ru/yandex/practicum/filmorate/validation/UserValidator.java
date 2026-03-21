package ru.yandex.practicum.filmorate.validation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

@Component
public class UserValidator extends BaseEntityValidator {

    private final UserStorage userStorage;

    public UserValidator(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Override
    protected boolean exists(Long id) {
        return userStorage.isExistById(id);
    }

    @Override
    protected String getEntityName() {
        return "User";
    }
}
