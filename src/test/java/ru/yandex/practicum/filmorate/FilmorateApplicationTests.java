package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmorateApplicationTests {
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    @Autowired
    public FilmorateApplicationTests(
            @Qualifier("userDbStorage") UserStorage userStorage,
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            GenreStorage genreStorage,
            MpaStorage mpaStorage) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    private User testUser;
    private Film testFilm;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .login("testLogin")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .friendsIds(Set.of())
                .build();

        testFilm = Film.builder()
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpaId(1L)
                .genresIds(List.of(1L))
                .likedUsersFilms(Set.of())
                .build();
    }

    @Test
    void testCreateUser() {
        User created = userStorage.save(testUser);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(created.getLogin()).isEqualTo(testUser.getLogin());
        assertThat(created.getName()).isEqualTo(testUser.getName());
        assertThat(created.getBirthday()).isEqualTo(testUser.getBirthday());
        assertThat(created.getFriendsIds()).isNotNull().isEmpty();
    }

    @Test
    void testUpdateUser() {
        User created = userStorage.save(testUser);

        User updatedUser = User.builder()
                .id(created.getId())
                .email("updated@test.com")
                .login("updatedLogin")
                .name("Updated Name")
                .birthday(LocalDate.of(1995, 5, 5))
                .friendsIds(Set.of())
                .build();

        User result = userStorage.update(updatedUser);

        assertThat(result.getId()).isEqualTo(created.getId());
        assertThat(result.getEmail()).isEqualTo("updated@test.com");
        assertThat(result.getLogin()).isEqualTo("updatedLogin");
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getBirthday()).isEqualTo(LocalDate.of(1995, 5, 5));
    }

    @Test
    void testUpdateUnknownUser() {
        User unknownUser = User.builder()
                .id(999L)
                .email("unknown@test.com")
                .login("unknown")
                .name("Unknown")
                .birthday(LocalDate.of(1990, 1, 1))
                .friendsIds(Set.of())
                .build();

        assertThrows(RuntimeException.class, () -> userStorage.update(unknownUser));
    }

    @Test
    void testGetAllUsers() {
        userStorage.save(testUser);
        userStorage.save(User.builder()
                .email("test2@test.com")
                .login("test2")
                .name("Test User 2")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());

        List<User> users = userStorage.findAll();

        assertThat(users).hasSize(2);
    }

    @Test
    void testIsUserExistById() {
        User created = userStorage.save(testUser);

        assertThat(userStorage.isExistById(created.getId())).isTrue();
        assertThat(userStorage.isExistById(999L)).isFalse();
    }

    @Test
    void testAddFriend() {
        User user1 = userStorage.save(testUser);
        User user2 = userStorage.save(User.builder()
                .email("friend@test.com")
                .login("friend")
                .name("Friend")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());

        userStorage.addFriendship(user1.getId(), user2.getId());

        Set<Long> friendsIds = userStorage.getUserFriendsIds(user1.getId());
        List<User> friends = userStorage.getUserFriends(user1.getId());

        assertThat(friendsIds).hasSize(1);
        assertThat(friendsIds).contains(user2.getId());
        assertThat(friends).hasSize(1);
        assertThat(friends.getFirst().getId()).isEqualTo(user2.getId());

        assertThat(userStorage.isFriend(user2.getId(), user1.getId())).isFalse();
    }

    @Test
    void testAddFriendWithUnknownId() {
        User user = userStorage.save(testUser);

        assertThrows(RuntimeException.class, () ->
                userStorage.addFriendship(user.getId(), 999L)
        );
    }

    @Test
    void testRemoveFriend() {
        User user1 = userStorage.save(testUser);
        User user2 = userStorage.save(User.builder()
                .email("friend@test.com")
                .login("friend")
                .name("Friend")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());

        userStorage.addFriendship(user1.getId(), user2.getId());
        userStorage.removeFriendship(user1.getId(), user2.getId());

        Set<Long> friendsIds = userStorage.getUserFriendsIds(user1.getId());
        assertThat(friendsIds).isEmpty();
    }

    @Test
    void testGetUserFriends() {
        User user1 = userStorage.save(testUser);
        User user2 = userStorage.save(User.builder()
                .email("friend1@test.com")
                .login("friend1")
                .name("Friend 1")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());
        User user3 = userStorage.save(User.builder()
                .email("friend2@test.com")
                .login("friend2")
                .name("Friend 2")
                .birthday(LocalDate.of(1996, 1, 1))
                .friendsIds(Set.of())
                .build());

        userStorage.addFriendship(user1.getId(), user2.getId());
        userStorage.addFriendship(user1.getId(), user3.getId());

        List<User> friends = userStorage.getUserFriends(user1.getId());
        Set<Long> friendsIds = userStorage.getUserFriendsIds(user1.getId());

        assertThat(friends).hasSize(2);
        assertThat(friendsIds).hasSize(2);
        assertThat(friends.stream().map(User::getId))
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
        assertThat(friendsIds)
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
    }

    @Test
    void testIsFriend() {
        User user1 = userStorage.save(testUser);
        User user2 = userStorage.save(User.builder()
                .email("friend@test.com")
                .login("friend")
                .name("Friend")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());

        userStorage.addFriendship(user1.getId(), user2.getId());

        assertThat(userStorage.isFriend(user1.getId(), user2.getId())).isTrue();
        assertThat(userStorage.isFriend(user2.getId(), user1.getId())).isFalse();
    }

    @Test
    void testGetCommonFriends() {
        User user1 = userStorage.save(testUser);
        User user2 = userStorage.save(User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User 2")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());
        User commonFriend = userStorage.save(User.builder()
                .email("common@test.com")
                .login("common")
                .name("Common Friend")
                .birthday(LocalDate.of(1996, 1, 1))
                .friendsIds(Set.of())
                .build());
        User user1Friend = userStorage.save(User.builder()
                .email("user1friend@test.com")
                .login("user1friend")
                .name("User 1 Friend")
                .birthday(LocalDate.of(1997, 1, 1))
                .friendsIds(Set.of())
                .build());

        userStorage.addFriendship(user1.getId(), commonFriend.getId());
        userStorage.addFriendship(user1.getId(), user1Friend.getId());
        userStorage.addFriendship(user2.getId(), commonFriend.getId());

        List<User> commonFriends = userStorage.getUsersCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends).hasSize(1);
        assertThat(commonFriends.getFirst().getId()).isEqualTo(commonFriend.getId());
    }

    @Test
    void testCreateFilm() {
        Film created = filmStorage.save(testFilm);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(testFilm.getName());
        assertThat(created.getDescription()).isEqualTo(testFilm.getDescription());
        assertThat(created.getReleaseDate()).isEqualTo(testFilm.getReleaseDate());
        assertThat(created.getDuration()).isEqualTo(testFilm.getDuration());
        assertThat(created.getMpaId()).isEqualTo(testFilm.getMpaId());
        assertThat(created.getGenresIds()).hasSize(1).containsExactly(1L);
        assertThat(created.getLikedUsersFilms()).isNotNull().isEmpty();
    }

    @Test
    void testCreateFilmWithMultipleGenres() {
        Film filmWithGenres = Film.builder()
                .name("Film with Genres")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpaId(1L)
                .genresIds(List.of(1L, 3L, 5L))
                .likedUsersFilms(Set.of())
                .build();

        Film created = filmStorage.save(filmWithGenres);

        assertThat(created.getGenresIds()).hasSize(3);
        assertThat(created.getGenresIds())
                .containsExactlyInAnyOrder(1L, 3L, 5L);
    }

    @Test
    void testCreateFilmWithTooLongDescription() {
        String longDescription = "A".repeat(201);
        Film filmWithLongDescription = Film.builder()
                .name("Film")
                .description(longDescription)
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpaId(1L)
                .genresIds(List.of())
                .likedUsersFilms(Set.of())
                .build();

        assertThrows(Exception.class, () -> filmStorage.save(filmWithLongDescription));
    }

    @Test
    void testCreateFilmWithNegativeDuration() {
        Film filmWithNegativeDuration = Film.builder()
                .name("Film")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(-120)
                .mpaId(1L)
                .genresIds(List.of())
                .likedUsersFilms(Set.of())
                .build();

        assertThrows(Exception.class, () -> filmStorage.save(filmWithNegativeDuration));
    }

    @Test
    void testCreateFilmWithInvalidMpa() {
        Film filmWithInvalidMpa = Film.builder()
                .name("Film")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpaId(10L)
                .genresIds(List.of())
                .likedUsersFilms(Set.of())
                .build();

        assertThrows(RuntimeException.class, () -> filmStorage.save(filmWithInvalidMpa));
    }

    @Test
    void testFindFilmById() {
        Film created = filmStorage.save(testFilm);
        Optional<Film> filmOptional = filmStorage.findFilmById(created.getId());

        assertThat(filmOptional)
                .isPresent()
                .hasValueSatisfying(film -> {
                    assertThat(film.getId()).isEqualTo(created.getId());
                    assertThat(film.getName()).isEqualTo(created.getName());
                    assertThat(film.getGenresIds()).hasSize(1);
                });
    }

    @Test
    void testUpdateFilm() {
        Film created = filmStorage.save(testFilm);

        Film updatedFilm = Film.builder()
                .id(created.getId())
                .name("Updated Film")
                .description("Updated Description")
                .releaseDate(LocalDate.of(2005, 5, 5))
                .duration(150)
                .mpaId(3L)
                .genresIds(List.of(2L, 4L))
                .likedUsersFilms(Set.of())
                .build();

        Film result = filmStorage.update(updatedFilm);

        assertThat(result.getId()).isEqualTo(created.getId());
        assertThat(result.getName()).isEqualTo("Updated Film");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getReleaseDate()).isEqualTo(LocalDate.of(2005, 5, 5));
        assertThat(result.getDuration()).isEqualTo(150);
        assertThat(result.getMpaId()).isEqualTo(3L);
        assertThat(result.getGenresIds()).hasSize(2).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    void testUpdateFilmGenres() {
        Film created = filmStorage.save(testFilm);

        Film updatedFilm = created.toBuilder()
                .genresIds(List.of(2L, 3L, 5L))
                .build();

        Film result = filmStorage.update(updatedFilm);

        assertThat(result.getGenresIds()).hasSize(3)
                .containsExactlyInAnyOrder(2L, 3L, 5L);
    }

    @Test
    void testUpdateFilmRemoveAllGenres() {
        Film created = filmStorage.save(testFilm);

        Film updatedFilm = created.toBuilder()
                .genresIds(List.of())
                .build();

        Film result = filmStorage.update(updatedFilm);

        assertThat(result.getGenresIds()).isEmpty();
    }

    @Test
    void testUpdateUnknownFilm() {
        Film unknownFilm = Film.builder()
                .id(999L)
                .name("Unknown Film")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpaId(1L)
                .genresIds(List.of())
                .likedUsersFilms(Set.of())
                .build();

        assertThrows(RuntimeException.class, () -> filmStorage.update(unknownFilm));
    }

    @Test
    void testGetAllFilms() {
        filmStorage.save(testFilm);
        filmStorage.save(Film.builder()
                .name("Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2005, 1, 1))
                .duration(150)
                .mpaId(2L)
                .genresIds(List.of(2L))
                .likedUsersFilms(Set.of())
                .build());

        List<Film> films = filmStorage.findAll();

        assertThat(films).hasSize(2);
        films.forEach(film -> assertThat(film.getGenresIds()).isNotNull());
    }

    @Test
    void testIsFilmExistById() {
        Film created = filmStorage.save(testFilm);

        assertThat(filmStorage.isExistById(created.getId())).isTrue();
        assertThat(filmStorage.isExistById(999L)).isFalse();
    }

    @Test
    void testAddLike() {
        User user = userStorage.save(testUser);
        Film film = filmStorage.save(testFilm);

        filmStorage.addLike(film.getId(), user.getId());

        assertThat(filmStorage.isLikeExists(film.getId(), user.getId())).isTrue();
    }

    @Test
    void testRemoveLike() {
        User user = userStorage.save(testUser);
        Film film = filmStorage.save(testFilm);

        filmStorage.addLike(film.getId(), user.getId());
        filmStorage.removeLike(film.getId(), user.getId());

        assertThat(filmStorage.isLikeExists(film.getId(), user.getId())).isFalse();
    }

    @Test
    void testIsLikeExists() {
        User user = userStorage.save(testUser);
        Film film = filmStorage.save(testFilm);

        assertThat(filmStorage.isLikeExists(film.getId(), user.getId())).isFalse();

        filmStorage.addLike(film.getId(), user.getId());

        assertThat(filmStorage.isLikeExists(film.getId(), user.getId())).isTrue();
    }

    @Test
    void testGetPopularFilms() {
        User user1 = userStorage.save(testUser);
        User user2 = userStorage.save(User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User 2")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());
        User user3 = userStorage.save(User.builder()
                .email("user3@test.com")
                .login("user3")
                .name("User 3")
                .birthday(LocalDate.of(1996, 1, 1))
                .friendsIds(Set.of())
                .build());

        Film film1 = filmStorage.save(Film.builder()
                .name("Film 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpaId(1L)
                .genresIds(List.of(1L))
                .likedUsersFilms(Set.of())
                .build());

        Film film2 = filmStorage.save(Film.builder()
                .name("Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(130)
                .mpaId(2L)
                .genresIds(List.of(2L))
                .likedUsersFilms(Set.of())
                .build());

        Film film3 = filmStorage.save(Film.builder()
                .name("Film 3")
                .description("Description 3")
                .releaseDate(LocalDate.of(2002, 1, 1))
                .duration(140)
                .mpaId(3L)
                .genresIds(List.of(3L))
                .likedUsersFilms(Set.of())
                .build());

        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user3.getId());

        filmStorage.addLike(film3.getId(), user1.getId());
        filmStorage.addLike(film3.getId(), user2.getId());

        filmStorage.addLike(film1.getId(), user1.getId());

        List<Film> popularFilms = filmStorage.getPopularFilms(3);

        assertThat(popularFilms).hasSize(3);

        assertThat(popularFilms.get(0).getId()).isEqualTo(film2.getId());
        assertThat(popularFilms.get(1).getId()).isEqualTo(film3.getId());
        assertThat(popularFilms.get(2).getId()).isEqualTo(film1.getId());
    }

    @Test
    void testGetСommonFilms() {
        User user1 = userStorage.save(testUser);
        User user2 = userStorage.save(User.builder()
                .email("user2@test.com")
                .login("user2")
                .name("User 2")
                .birthday(LocalDate.of(1995, 1, 1))
                .friendsIds(Set.of())
                .build());
        User user3 = userStorage.save(User.builder()
                .email("user3@test.com")
                .login("user3")
                .name("User 3")
                .birthday(LocalDate.of(1996, 1, 1))
                .friendsIds(Set.of())
                .build());

        Film film1 = filmStorage.save(Film.builder()
                .name("Film 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpaId(1L)
                .genresIds(List.of(1L))
                .likedUsersFilms(Set.of())
                .build());

        Film film2 = filmStorage.save(Film.builder()
                .name("Film 2")
                .description("Description 2")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(130)
                .mpaId(2L)
                .genresIds(List.of(2L))
                .likedUsersFilms(Set.of())
                .build());

        Film film3 = filmStorage.save(Film.builder()
                .name("Film 3")
                .description("Description 3")
                .releaseDate(LocalDate.of(2002, 1, 1))
                .duration(140)
                .mpaId(3L)
                .genresIds(List.of(3L))
                .likedUsersFilms(Set.of())
                .build());

        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user3.getId());

        filmStorage.addLike(film3.getId(), user1.getId());
        filmStorage.addLike(film3.getId(), user2.getId());

        filmStorage.addLike(film1.getId(), user1.getId());

        List<Film> commonFilms = filmStorage.getCommonFilms(user1.getId(), user2.getId());

        assertThat(commonFilms).hasSize(2);

        assertThat(commonFilms.get(0).getId()).isEqualTo(film2.getId());
        assertThat(commonFilms.get(1).getId()).isEqualTo(film3.getId());
    }


    @Test
    void testGetPopularFilmsWithLimit() {
        User user = userStorage.save(testUser);

        for (int i = 1; i <= 5; i++) {
            Film film = Film.builder()
                    .name("Film " + i)
                    .description("Description " + i)
                    .releaseDate(LocalDate.of(2000 + i, 1, 1))
                    .duration(100 + i)
                    .mpaId(1L)
                    .genresIds(List.of(1L))
                    .likedUsersFilms(Set.of())
                    .build();
            filmStorage.save(film);
            filmStorage.addLike(film.getId(), user.getId());
        }

        List<Film> popularFilms = filmStorage.getPopularFilms(3);

        assertThat(popularFilms).hasSize(3);
    }

    @Test
    void testGetMpaById() {
        Optional<Mpa> mpa = mpaStorage.findById(1L);

        assertThat(mpa).isPresent();
        assertThat(mpa.get().getId()).isEqualTo(1L);
    }

    @Test
    void testGetAllMpa() {
        List<Mpa> allMpa = mpaStorage.findAll();

        assertThat(allMpa).hasSize(5);

        assertThat(allMpa.stream().map(Mpa::getId))
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void testFindMpaByIdIn() {
        Set<Long> mpaIds = Set.of(1L, 3L, 5L);
        Set<Mpa> mpas = mpaStorage.findByIdIn(mpaIds);

        assertThat(mpas).hasSize(3);
        assertThat(mpas.stream().map(Mpa::getId))
                .containsExactlyInAnyOrder(1L, 3L, 5L);
    }

    @Test
    void testIsMpaExistById() {
        assertThat(mpaStorage.isExistById(1L)).isTrue();
        assertThat(mpaStorage.isExistById(999L)).isFalse();
    }

    @Test
    void testGetGenreById() {
        Optional<Genre> genre = genreStorage.findById(1L);

        assertThat(genre).isPresent();
        assertThat(genre.get().getId()).isEqualTo(1L);
        assertThat(genre.get().getName()).isEqualTo("Комедия");
    }

    @Test
    void testGetAllGenres() {
        List<Genre> allGenres = genreStorage.findAll();

        assertThat(allGenres).hasSize(6);

        assertThat(allGenres.stream().map(Genre::getName))
                .containsExactlyInAnyOrder(
                        "Комедия", "Драма", "Мультфильм",
                        "Триллер", "Документальный", "Боевик"
                );
    }

    @Test
    void testFindGenresByIds() {
        List<Long> genreIds = List.of(1L, 3L, 5L);
        List<Genre> genres = genreStorage.findGenresByIds(genreIds);

        assertThat(genres).hasSize(3);
        assertThat(genres.stream().map(Genre::getId))
                .containsExactlyInAnyOrder(1L, 3L, 5L);
    }

    @Test
    void testFindGenreIdsByIds() {
        List<Long> genreIds = List.of(1L, 3L, 5L);
        List<Long> foundIds = genreStorage.findGenreIdsByIds(genreIds);

        assertThat(foundIds).hasSize(3);
        assertThat(foundIds).containsExactlyInAnyOrder(1L, 3L, 5L);
    }

    @Test
    void testFindGenresByIdsWithInvalidId() {
        List<Long> genreIds = List.of(1L, 999L, 3L);
        List<Genre> genres = genreStorage.findGenresByIds(genreIds);

        assertThat(genres).hasSize(2);
        assertThat(genres.stream().map(Genre::getId))
                .containsExactlyInAnyOrder(1L, 3L);
    }
}