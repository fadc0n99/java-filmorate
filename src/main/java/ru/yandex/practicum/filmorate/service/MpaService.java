package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.MpaMapper;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpaService {

    private final MpaStorage mpaStorage;
    private final MpaMapper mpaMapper;

    private static final String MPA_NOT_FOUND_MESSAGE = "MPA rating with ID %d not found";

    public List<MpaDto> findAllMpa() {
        return mpaStorage.findAll()
                .stream()
                .map(mpaMapper::toDto)
                .toList();
    }

    public MpaDto findMpaById(long id) {
        return mpaStorage.findById(id)
                .map(mpaMapper::toDto)
                .orElseThrow(() -> new NotFoundException(String.format(MPA_NOT_FOUND_MESSAGE, id)));
    }

    public Set<Mpa> findMpaByIds(Set<Long> mpaIds) {
        return new HashSet<>(mpaStorage.findByIdIn(mpaIds));
    }

    public boolean isMpaExist(Long id) {
        return mpaStorage.isExistById(id);
    }

    public Map<Long, MpaDto> getMpaByFilmIds(List<Long> filmIds) {
        Map<Long, Mpa> mpaMap = mpaStorage.findMpasByFilmIds(filmIds);

        return mpaMap.entrySet()
                .stream()
                .collect(Collectors.toMap(
                   Map.Entry::getKey,
                   entry -> mpaMapper.toDto(entry.getValue())
                ));
    }
}
