package com.orbe.cinema.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "saved_titles")
public class SavedTitle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String imdbId;

	@Column(nullable = false)
	private String title;

	private String yearLabel;

	private int releaseYear;

	private Integer runtimeMinutes;

	private String type;

	@Column(length = 1000)
	private String poster;

	@Column(columnDefinition = "TEXT")
	private String plot;

	@Column(length = 1000)
	private String director;

	@Column(length = 500)
	private String genre;

	private String imdbRating;

	private boolean includedInPlan;

	private double ratingSum;

	private int ratingCount;

	public void addRating(int score) {
		if (score < 1 || score > 5) {
			throw new IllegalArgumentException("La nota debe estar entre 1 y 5.");
		}
		this.ratingSum += score;
		this.ratingCount++;
	}

	public double average() {
		if (ratingCount <= 0) {
			return 0;
		}
		return ratingSum / ratingCount;
	}

	public Long getId() {
		return id;
	}

	public String getImdbId() {
		return imdbId;
	}

	public void setImdbId(String imdbId) {
		this.imdbId = imdbId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getYearLabel() {
		return yearLabel;
	}

	public void setYearLabel(String yearLabel) {
		this.yearLabel = yearLabel;
	}

	public int getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(int releaseYear) {
		this.releaseYear = releaseYear;
	}

	public Integer getRuntimeMinutes() {
		return runtimeMinutes;
	}

	public void setRuntimeMinutes(Integer runtimeMinutes) {
		this.runtimeMinutes = runtimeMinutes;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPoster() {
		return poster;
	}

	public void setPoster(String poster) {
		this.poster = poster;
	}

	public String getPlot() {
		return plot;
	}

	public void setPlot(String plot) {
		this.plot = plot;
	}

	public String getDirector() {
		return director;
	}

	public void setDirector(String director) {
		this.director = director;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getImdbRating() {
		return imdbRating;
	}

	public void setImdbRating(String imdbRating) {
		this.imdbRating = imdbRating;
	}

	public boolean isIncludedInPlan() {
		return includedInPlan;
	}

	public void setIncludedInPlan(boolean includedInPlan) {
		this.includedInPlan = includedInPlan;
	}

	public double getRatingSum() {
		return ratingSum;
	}

	public int getRatingCount() {
		return ratingCount;
	}
}
