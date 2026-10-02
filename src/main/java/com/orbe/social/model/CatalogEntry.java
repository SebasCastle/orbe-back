package com.orbe.social.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "catalog_entries", uniqueConstraints = @UniqueConstraint(name = "uk_catalog_kind_external", columnNames = {"kind", "external_id"}))
public class CatalogEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private ContentKind kind;

	@Column(name = "external_id", nullable = false, length = 80)
	private String externalId;

	@Column(nullable = false, length = 300)
	private String title;

	@Column(length = 40)
	private String yearLabel;

	private int releaseYear;

	@Column(length = 300)
	private String creator;

	@Column(length = 1000)
	private String imageUrl;

	@Column(length = 4000)
	private String synopsis;

	@Column(length = 80)
	private String category;

	private Integer runtimeMinutes;

	@Column(length = 1000)
	private String previewUrl;

	@Column(length = 20)
	private String sourceRating;

	@Column(length = 40)
	private String subtype;

	public Long getId() {
		return id;
	}

	public ContentKind getKind() {
		return kind;
	}

	public void setKind(ContentKind kind) {
		this.kind = kind;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
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

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getSynopsis() {
		return synopsis;
	}

	public void setSynopsis(String synopsis) {
		this.synopsis = synopsis;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Integer getRuntimeMinutes() {
		return runtimeMinutes;
	}

	public void setRuntimeMinutes(Integer runtimeMinutes) {
		this.runtimeMinutes = runtimeMinutes;
	}

	public String getPreviewUrl() {
		return previewUrl;
	}

	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}

	public String getSourceRating() {
		return sourceRating;
	}

	public void setSourceRating(String sourceRating) {
		this.sourceRating = sourceRating;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}
}
