package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
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

    public Director create(Director director) {
        log.debug("Запрос на создание режиссера: {}", director);
        return directorStorage.create(director);
    }

    public Director update(Director director) {
        log.debug("Запрос на обновление режиссера: {}", director);
        return directorStorage.update(director);
    }

    public void delete(Integer id) {
        log.debug("Запрос на удаление режиссера с id: {}", id);
        requireExists(id);
        directorStorage.delete(id);
    }

    public void requireExists(Integer id) {
        if (!directorStorage.isExistById(id)) {
            log.warn("Режиссер с id {} не найден", id);
            throw new NotFoundException("Режиссер с id " + id + " не найден");
        }
    }
}