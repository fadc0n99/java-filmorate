package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.director.DirectorDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.DirectorMapper;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectorService {

    private final DirectorStorage directorStorage;

    public List<Director> findAll() {
        log.debug("Запрос на получение всех режиссеров");
        return directorStorage.findAll();
    }

    public Director findById(Integer id) {
        log.debug("Запрос на получение режиссера с id: {}", id);
        return directorStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Режиссер с id " + id + " не найден"));
    }

    public Director create(DirectorDto directorDto) {
        log.debug("Запрос на создание режиссера: {}", directorDto);

        Director director = DirectorMapper.toEntity(directorDto);

        return directorStorage.create(director);
    }

    public Director update(DirectorDto directorDto) {
        log.debug("Запрос на обновление режиссера: {}", directorDto);

        Director director = DirectorMapper.toEntity(directorDto);

        return directorStorage.update(director);
    }

    public void delete(Integer id) {
        log.debug("Запрос на удаление режиссера с id: {}", id);
        validateDirectorExists(id);
        directorStorage.delete(id);
    }

    public void validateDirectorExists(Integer id) {
        if (!directorStorage.isExistById(id)) {
            log.warn("Режиссер с id {} не найден", id);
            throw new NotFoundException("Режиссер с id " + id + " не найден");
        }
    }
}