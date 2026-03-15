package ru.yandex.practicum.filmorate.storage.mpa;

import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MpaStorage {
    List<Mpa> findAll();

    Optional<Mpa> findById(long id);

    Set<Mpa> findByIdIn(Set<Long> mpaIds);

    boolean isExistById(Long mpaId);

    Map<Long, Mpa> findMpasByFilmIds(List<Long> filmIds);
}
