package com.orbe.cinema.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbe.cinema.model.SavedTitle;
import com.orbe.cinema.omdb.OmdbModels.OmdbTitle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainRulesTest {

	@Test
	void parsesRuntimeWithoutCuttingShortValues() {
		assertEquals(96, DurationParser.minutesOrNull("96 min"));
		assertEquals(127, DurationParser.minutesOrNull("127 min"));
		assertNull(DurationParser.minutesOrNull("N/A"));
		assertNull(DurationParser.minutesOrNull(null));
	}

	@Test
	void strictRuntimeKeepsTheOriginalMessage() {
		Errores error = assertThrows(Errores.class, () -> DurationParser.requireMinutes("N/A"));
		assertEquals("No pude convertir la duración, porque contiene un N/A", error.getMessage());
	}

	@Test
	void parsesTheFirstYearOfARange() {
		assertEquals(2009, YearParser.yearOrZero("2009"));
		assertEquals(2019, YearParser.yearOrZero("2019–"));
		assertEquals(2008, YearParser.yearOrZero("2008–2013"));
		assertEquals(0, YearParser.yearOrZero("N/A"));
	}

	@Test
	void ratingsStayBetweenOneAndFiveAndKeepTheExactAverage() {
		SavedTitle title = new SavedTitle();
		title.addRating(5);
		title.addRating(4);
		title.addRating(3);

		RecommendationFilter filter = new RecommendationFilter();
		assertEquals(4.0, title.average(), 0.0001);
		assertEquals(4, filter.classification(title.average()));
		assertEquals("Está entre los favoritos del momento", filter.recommend(title.average()));
		assertEquals("Bien evaluado actualmente", filter.recommend(3.5));
		assertEquals("Colócalo en tu lista para verlo después", filter.recommend(1.5));

		SavedTitle split = new SavedTitle();
		split.addRating(5);
		split.addRating(4);
		assertEquals(4.5, split.average(), 0.0001);
		assertEquals("Está entre los favoritos del momento", filter.recommend(split.average()));
		assertThrows(IllegalArgumentException.class, () -> split.addRating(6));
		assertThrows(IllegalArgumentException.class, () -> split.addRating(0));
	}

	@Test
	void watchTimeAddsEveryIncludedTitle() {
		WatchTimeCalculator calculator = new WatchTimeCalculator();
		calculator.include(180);
		calculator.include(200);
		calculator.include(10 * 10 * 50);
		assertEquals(5380, calculator.total());
	}

	@Test
	void readsOmdbPayload() throws Exception {
		ObjectMapper mapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		OmdbTitle title = mapper.readValue("""
				{"Title":"Up","Year":"2009","Runtime":"96 min","Genre":"Animation","Director":"Pete Docter","imdbID":"tt1049413","Type":"movie","Response":"True","Poster":"N/A","imdbRating":"8.3","Awards":"Won 2 Oscars"}
				""", OmdbTitle.class);
		assertEquals("Up", title.title());
		assertEquals("tt1049413", title.imdbId());
		assertEquals(96, DurationParser.minutesOrNull(title.runtime()));
	}
}
