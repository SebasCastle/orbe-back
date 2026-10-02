package com.orbe.cinema.service;

import com.orbe.cinema.domain.DurationParser;
import com.orbe.cinema.domain.RecommendationFilter;
import com.orbe.cinema.domain.YearParser;
import com.orbe.cinema.dto.CinemaDtos.LibraryItem;
import com.orbe.cinema.dto.CinemaDtos.SearchItem;
import com.orbe.cinema.dto.CinemaDtos.TitleDetail;
import com.orbe.cinema.model.SavedTitle;
import com.orbe.cinema.omdb.OmdbModels.OmdbSearchItem;
import com.orbe.cinema.omdb.OmdbModels.OmdbTitle;
import com.orbe.common.Texts;
import org.springframework.stereotype.Component;

@Component
public class CinemaMapper {

	private final RecommendationFilter recommendations = new RecommendationFilter();

	public SearchItem searchItem(OmdbSearchItem item) {
		return new SearchItem(
				item.imdbId(),
				Texts.clean(item.title()),
				Texts.clean(item.year()),
				item.type() == null ? "movie" : item.type(),
				Texts.clean(item.poster())
		);
	}

	public SavedTitle newTitle(OmdbTitle remote) {
		SavedTitle title = new SavedTitle();
		copyRemote(remote, title);
		return title;
	}

	public TitleDetail detail(OmdbTitle remote, SavedTitle saved) {
		Integer runtime = DurationParser.minutesOrNull(remote.runtime());
		if (saved == null) {
			return new TitleDetail(
					remote.imdbId(),
					Texts.clean(remote.title()),
					Texts.clean(remote.year()),
					YearParser.yearOrZero(remote.year()),
					runtime,
					Texts.clean(remote.rated()),
					Texts.clean(remote.released()),
					Texts.clean(remote.genre()),
					Texts.clean(remote.director()),
					Texts.clean(remote.writer()),
					Texts.clean(remote.actors()),
					Texts.clean(remote.plot()),
					Texts.clean(remote.language()),
					Texts.clean(remote.poster()),
					Texts.clean(remote.imdbRating()),
					remote.type() == null ? "movie" : remote.type(),
					false,
					null,
					0,
					0,
					0,
					null,
					false,
					null
			);
		}
		int classification = recommendations.classification(saved.average());
		return new TitleDetail(
				remote.imdbId(),
				Texts.clean(remote.title()),
				Texts.clean(remote.year()),
				YearParser.yearOrZero(remote.year()),
				runtime,
				Texts.clean(remote.rated()),
				Texts.clean(remote.released()),
				Texts.clean(remote.genre()),
				Texts.clean(remote.director()),
				Texts.clean(remote.writer()),
				Texts.clean(remote.actors()),
				Texts.clean(remote.plot()),
				Texts.clean(remote.language()),
				Texts.clean(remote.poster()),
				Texts.clean(remote.imdbRating()),
				remote.type() == null ? "movie" : remote.type(),
				true,
				saved.getId(),
				saved.getRatingCount(),
				saved.average(),
				classification,
				recommendations.recommend(saved.average()),
				saved.isIncludedInPlan(),
				null
		);
	}

	public TitleDetail socialize(TitleDetail detail, boolean saved, Long libraryId, int votes, double average, Integer personal, boolean plan) {
		int classification = votes <= 0 ? 0 : recommendations.classification(average);
		String recommendation = votes <= 0 ? null : recommendations.recommend(average);
		return new TitleDetail(
				detail.imdbId(),
				detail.title(),
				detail.year(),
				detail.releaseYear(),
				detail.runtimeMinutes(),
				detail.rated(),
				detail.released(),
				detail.genre(),
				detail.director(),
				detail.writer(),
				detail.actors(),
				detail.plot(),
				detail.language(),
				detail.poster(),
				detail.imdbRating(),
				detail.type(),
				saved,
				libraryId,
				votes,
				average,
				classification,
				recommendation,
				plan,
				personal
		);
	}

	public TitleDetail detailFromSaved(SavedTitle saved) {
		int classification = recommendations.classification(saved.average());
		return new TitleDetail(
				saved.getImdbId(),
				saved.getTitle(),
				saved.getYearLabel(),
				saved.getReleaseYear(),
				saved.getRuntimeMinutes(),
				null,
				null,
				saved.getGenre(),
				saved.getDirector(),
				null,
				null,
				saved.getPlot(),
				null,
				saved.getPoster(),
				saved.getImdbRating(),
				saved.getType(),
				true,
				saved.getId(),
				saved.getRatingCount(),
				saved.average(),
				classification,
				recommendations.recommend(saved.average()),
				saved.isIncludedInPlan(),
				null
		);
	}

	public LibraryItem item(SavedTitle saved) {
		int classification = recommendations.classification(saved.average());
		return new LibraryItem(
				saved.getId(),
				saved.getImdbId(),
				saved.getTitle(),
				saved.getYearLabel(),
				saved.getReleaseYear(),
				saved.getRuntimeMinutes(),
				saved.getType(),
				saved.getPoster(),
				saved.getPlot(),
				saved.getDirector(),
				saved.getGenre(),
				saved.getImdbRating(),
				saved.isIncludedInPlan(),
				saved.getRatingCount(),
				saved.average(),
				classification,
				recommendations.recommend(saved.average()),
				null
		);
	}

	private void copyRemote(OmdbTitle remote, SavedTitle title) {
		title.setImdbId(remote.imdbId());
		title.setTitle(Texts.clean(remote.title()) == null ? "Sin título" : Texts.clean(remote.title()));
		title.setYearLabel(Texts.clean(remote.year()));
		title.setReleaseYear(YearParser.yearOrZero(remote.year()));
		title.setRuntimeMinutes(DurationParser.minutesOrNull(remote.runtime()));
		title.setType(remote.type() == null ? "movie" : remote.type());
		title.setPoster(Texts.clean(remote.poster()));
		title.setPlot(Texts.clean(remote.plot()));
		title.setDirector(Texts.clean(remote.director()));
		title.setGenre(Texts.clean(remote.genre()));
		title.setImdbRating(Texts.clean(remote.imdbRating()));
	}
}
