package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film save(Film newFilm) {
        long newId = generateId();
        newFilm.setId(newId);
        films.put(newId, newFilm);
        log.debug("Фильм сохранен в памяти: {}", newFilm);
        return newFilm;
    }

    @Override
    public Film update(Film newFilm) {
        if (!films.containsKey(newFilm.getId())) {
            throw new NotFoundException("Фильм с ID " + newFilm.getId() + " не найден");
        }
        films.put(newFilm.getId(), newFilm);
        log.debug("Фильм обновлен в памяти: {}", newFilm);
        return newFilm;
    }

    @Override
    public Optional<Film> findFilmById(long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public boolean isExistById(long filmId) {
        return films.containsKey(filmId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        Film film = films.get(filmId);
        if (film == null) {
            log.warn("Film with ID {} not found, cannot remove like from user {}", filmId, userId);
            return;
        }

        Set<Long> likes = film.getLikedUsersFilms();
        if (likes != null) {
            likes.remove(userId);
        }
    }

    public Set<Long> getLikes(Long id) {
        Film film = films.get(id);
        if (film == null) {
            return Collections.emptySet();
        }

        Set<Long> likes = film.getLikedUsersFilms();
        return likes != null ? likes : Collections.emptySet();
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return new ArrayList<>();
    }

    @Override
    public List<Film> findFilmsByIds(List<Long> filmIds) {
        return List.of();
    }

    @Override
    public List<Long> findUserLikedFilmIds(long userId) {
        return List.of();
    }

    @Override
    public Map<Long, List<Long>> findAllUsersLikedFilmIds() {
        return Map.of();
    }

    @Override
    public void addLike(long filmId, long userId) {
        Film film = films.get(filmId);
        if (film == null) {
            throw new NotFoundException("Film with ID " + filmId + " not found");
        }
        if (film.getLikedUsersFilms() == null) {
            film.setLikedUsersFilms(new HashSet<>());
        }
        film.getLikedUsersFilms().add(userId);
    }

    @Override
    public boolean isLikeExists(long filmId, long userId) {
        Film film = films.get(filmId);
        return film != null && film.getLikedUsersFilms() != null && film.getLikedUsersFilms().contains(userId);
    }

    @Override
    public List<Film> getPopularFilms(long count) {
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(getLikesCount(f2), getLikesCount(f1)))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> findAllByDirector(long directorId, String sortBy) {
        List<Film> directorFilms = films.values().stream()
                .filter(film -> film.getDirectors() != null &&
                        film.getDirectors().stream().anyMatch(d -> d.getId() == directorId))
                .collect(Collectors.toCollection(ArrayList::new));

        if ("year".equalsIgnoreCase(sortBy)) {
            directorFilms.sort(Comparator.comparing(Film::getReleaseDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("likes".equalsIgnoreCase(sortBy)) {
            directorFilms.sort((f1, f2) -> Integer.compare(getLikesCount(f2), getLikesCount(f1)));
        }
        return directorFilms;
    }

    private int getLikesCount(Film film) {
        return film.getLikedUsersFilms() != null ? film.getLikedUsersFilms().size() : 0;
    }

    private Long generateId() {
        long currentId = films.keySet().stream().mapToLong(id -> id).max().orElse(0);
        return ++currentId;
    }
}