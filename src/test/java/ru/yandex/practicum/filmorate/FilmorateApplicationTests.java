package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilmorateApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {

	}

	@Test
	void test_emptyRequest() throws Exception {
		mockMvc.perform(post("/users")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createUser_missingEmail() throws Exception {
		User user = User.builder()
				.email(null)
				.login("test")
				.birthday(LocalDate.of(1990, 1, 1))
				.build();

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(user)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createUser_invalidEmail() throws Exception {
		User user = User.builder()
				.email("invalid-email")
				.login("test")
				.birthday(LocalDate.of(1990, 1, 1))
				.build();

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(user)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createUser_emptyLogin() throws Exception {
		User user = User.builder()
				.email("test@example.com")
				.login("")
				.birthday(LocalDate.of(1990, 1, 1))
				.build();

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(user)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createUser_loginWithSpace() throws Exception {
		User user = User.builder()
				.email("test@example.com")
				.login("test user")
				.birthday(LocalDate.of(1990, 1, 1))
				.build();

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(user)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createUser_futureBirthday() throws Exception {
		User user = User.builder()
				.email("test@example.com")
				.login("test")
				.birthday(LocalDate.now().plusDays(1))
				.build();

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(user)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createFilm_missingName() throws Exception {
		Film film = Film.builder()
				.name(null)
				.description("Norm film")
				.releaseDate(LocalDate.of(2000, 1, 1))
				.duration(120)
				.build();

		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createFilm_emptyName() throws Exception {
		Film film = Film.builder()
				.name("")
				.description("Norm film")
				.releaseDate(LocalDate.of(2000, 1, 1))
				.duration(120)
				.build();

		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createFilm_pastReleaseDate_succeeds() throws Exception {
		Film film = Film.builder()
				.name("Super Film")
				.releaseDate(LocalDate.of(1895, 12, 28))  // MIN_RELEASE_DATE
				.duration(120)
				.build();

		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isCreated());
	}

	@Test
	void test_createFilm_beforeMinReleaseDate() throws Exception {
		Film film = Film.builder()
				.name("Plohoy Film")
				.releaseDate(LocalDate.of(1895, 12, 27))  // раньше MIN
				.duration(120)
				.build();

		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createFilm_negativeDuration() throws Exception {
		Film film = Film.builder()
				.name("Film")
				.releaseDate(LocalDate.of(2000, 1, 1))
				.duration(-10)
				.build();

		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void test_createFilm_zeroDuration() throws Exception {
		Film film = Film.builder()
				.name("Film")
				.releaseDate(LocalDate.of(2000, 1, 1))
				.duration(0)
				.build();

		mockMvc.perform(post("/films")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(film)))
				.andExpect(status().isBadRequest());
	}
}
