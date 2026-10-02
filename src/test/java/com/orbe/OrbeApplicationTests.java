package com.orbe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbe.cinema.omdb.OmdbClient;
import com.orbe.cinema.omdb.OmdbModels.OmdbSearch;
import com.orbe.cinema.omdb.OmdbModels.OmdbSearchItem;
import com.orbe.cinema.omdb.OmdbModels.OmdbTitle;
import com.orbe.cinema.repository.SavedTitleRepository;
import com.orbe.social.repository.CatalogRepository;
import com.orbe.social.repository.CommentRepository;
import com.orbe.social.repository.FavoriteRepository;
import com.orbe.social.repository.HistoryRepository;
import com.orbe.social.repository.PostLikeRepository;
import com.orbe.social.repository.PostRepository;
import com.orbe.social.repository.RatingRepository;
import com.orbe.social.repository.SavedItemRepository;
import com.orbe.social.repository.ShelfItemRepository;
import com.orbe.social.repository.ShelfRepository;
import com.orbe.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrbeApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private SavedTitleRepository titles;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private CatalogRepository catalogs;

	@Autowired
	private RatingRepository ratings;

	@Autowired
	private SavedItemRepository savedItems;

	@Autowired
	private FavoriteRepository favorites;

	@Autowired
	private HistoryRepository history;

	@Autowired
	private ShelfItemRepository shelfItems;

	@Autowired
	private ShelfRepository shelves;

	@Autowired
	private PostLikeRepository likes;

	@Autowired
	private CommentRepository comments;

	@Autowired
	private PostRepository posts;

	@MockBean
	private OmdbClient omdbClient;

	@BeforeEach
	void clean() {
		likes.deleteAll();
		comments.deleteAll();
		posts.deleteAll();
		history.deleteAll();
		shelfItems.deleteAll();
		shelves.deleteAll();
		favorites.deleteAll();
		ratings.deleteAll();
		savedItems.deleteAll();
		catalogs.deleteAll();
		titles.deleteAll();
		users.deleteAll();
	}

	@Test
	void healthIsOk() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void cinemaFlowPersistsRatingsAndWatchTime() throws Exception {
		String token = token("ana");
		when(omdbClient.search("up", 1)).thenReturn(new OmdbSearch(
				List.of(new OmdbSearchItem("Up", "2009", "tt1049413", "movie", "N/A")),
				"1",
				"True",
				null
		));
		when(omdbClient.details("tt1049413")).thenReturn(sample("96 min"));

		mockMvc.perform(get("/api/cinema/search").param("q", "up"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results[0].imdbId").value("tt1049413"))
				.andExpect(jsonPath("$.results[0].poster").value(nullValue()));

		mockMvc.perform(get("/api/cinema/titles/tt1049413"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Up"))
				.andExpect(jsonPath("$.runtimeMinutes").value(96))
				.andExpect(jsonPath("$.saved").value(false));

		String created = mockMvc.perform(post("/api/cinema/library")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"imdbId\":\"tt1049413\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.runtimeMinutes").value(96))
				.andReturn().getResponse().getContentAsString();
		long id = mapper.readTree(created).get("id").asLong();

		mockMvc.perform(post("/api/cinema/library")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"imdbId\":\"tt1049413\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));

		mockMvc.perform(post("/api/cinema/library/" + id + "/ratings")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"score\":5}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.classification").value(5))
				.andExpect(jsonPath("$.recommendation").value("Está entre los favoritos del momento"));

		mockMvc.perform(patch("/api/cinema/library/" + id)
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"includedInPlan\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.includedInPlan").value(true));

		mockMvc.perform(get("/api/cinema/library").param("sort", "year").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.totalMinutes").value(96));

		mockMvc.perform(delete("/api/cinema/library/" + id).header("Authorization", token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/cinema/library").header("Authorization", token))
				.andExpect(jsonPath("$.count").value(0))
				.andExpect(jsonPath("$.totalMinutes").value(0));
	}

	@Test
	void unknownDurationDoesNotBreakSearchOrAddWatchTime() throws Exception {
		String token = token("bea");
		when(omdbClient.details("tt0000001")).thenReturn(new OmdbTitle(
				"Sin duración", "2019–", "N/A", null, null, null, null, null, null,
				"N/A", null, "tt0000001", "series", null, null, "True", null
		));

		mockMvc.perform(get("/api/cinema/titles/tt0000001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releaseYear").value(2019))
				.andExpect(jsonPath("$.runtimeMinutes").value(nullValue()));

		mockMvc.perform(post("/api/cinema/library")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"imdbId\":\"tt0000001\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/cinema/library").header("Authorization", token))
				.andExpect(jsonPath("$.totalMinutes").value(0));
	}

	@Test
	void rejectsInvalidInput() throws Exception {
		String token = token("cara");
		mockMvc.perform(get("/api/cinema/search").param("q", " "))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/cinema/library")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"imdbId\":\"bad\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/cinema/library").param("sort", "nope").header("Authorization", token))
				.andExpect(status().isBadRequest());
		mockMvc.perform(delete("/api/cinema/library/999").header("Authorization", token))
				.andExpect(status().isNotFound());
	}

	@Test
	void aUserRatesEachTitleOnceAndTheAverageIncludesEveryone() throws Exception {
		String ana = token("ana");
		String luis = token("luis");
		String body = """
				{"kind":"MOVIE","externalId":"tt9","score":4,"opinion":"Me gustó","title":"Ejemplo","year":"2001"}
				""";

		mockMvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/ratings").header("Authorization", ana).contentType(MediaType.APPLICATION_JSON)
						.content(body.replace("\"score\":4", "\"score\":6")))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/ratings").header("Authorization", ana).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.votes").value(1))
				.andExpect(jsonPath("$.personalScore").value(4));

		mockMvc.perform(post("/api/ratings").header("Authorization", ana).contentType(MediaType.APPLICATION_JSON)
						.content(body.replace("\"score\":4", "\"score\":5")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personalScore").value(5))
				.andExpect(jsonPath("$.votes").value(1))
				.andExpect(jsonPath("$.average").value(5.0));

		mockMvc.perform(post("/api/ratings").header("Authorization", luis).contentType(MediaType.APPLICATION_JSON)
						.content(body.replace("Me gustó", "Correcta")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.votes").value(2))
				.andExpect(jsonPath("$.average").value(4.5))
				.andExpect(jsonPath("$.personalScore").value(4))
				.andExpect(jsonPath("$.recommendation").value("Está entre los favoritos del momento"));

		assertEquals(2, ratings.count());
	}

	@Test
	void usersCanDeleteOnlyTheirOwnPosts() throws Exception {
		String ana = token("ana");
		String luis = token("luis");
		String created = mockMvc.perform(post("/api/community/posts")
						.header("Authorization", ana)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Hola\",\"body\":\"Una idea\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long id = mapper.readTree(created).get("id").asLong();

		mockMvc.perform(delete("/api/community/posts/" + id).header("Authorization", luis))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/community/posts/" + id).header("Authorization", ana))
				.andExpect(status().isNoContent());
	}

	@Test
	void spotifyLoginReportsConfiguration() throws Exception {
		int status = mockMvc.perform(get("/api/spotify/login")).andReturn().getResponse().getStatus();
		assertTrue(status == 200 || status == 503);
	}

	private String token(String username) throws Exception {
		String response = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","email":"%s@orbe.test","password":"secreto123","displayName":"%s"}
								""".formatted(username, username, username)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + mapper.readTree(response).get("token").asText();
	}

	private OmdbTitle sample(String runtime) {
		return new OmdbTitle(
				"Up", "2009", runtime, "Animation, Adventure", "Pete Docter", "Bob Peterson",
				"Ed Asner", "Una casa vuela.", "English", "N/A", "8.3", "tt1049413",
				"movie", "PG", "29 May 2009", "True", null
		);
	}
}
