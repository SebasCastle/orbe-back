package com.orbe.social.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "shelf_items", uniqueConstraints = @UniqueConstraint(name = "uk_shelf_catalog", columnNames = {"shelf_id", "catalog_id"}))
public class ShelfItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "shelf_id", nullable = false)
	private Shelf shelf;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "catalog_id", nullable = false)
	private CatalogEntry catalog;

	private Instant addedAt = Instant.now();

	public Long getId() {
		return id;
	}

	public Shelf getShelf() {
		return shelf;
	}

	public void setShelf(Shelf shelf) {
		this.shelf = shelf;
	}

	public CatalogEntry getCatalog() {
		return catalog;
	}

	public void setCatalog(CatalogEntry catalog) {
		this.catalog = catalog;
	}

	public Instant getAddedAt() {
		return addedAt;
	}

	public void setAddedAt(Instant addedAt) {
		this.addedAt = addedAt;
	}
}
