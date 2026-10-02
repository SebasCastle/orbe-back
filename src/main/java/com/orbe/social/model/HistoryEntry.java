package com.orbe.social.model;

import com.orbe.auth.model.UserAccount;
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
@Table(name = "history_entries", uniqueConstraints = @UniqueConstraint(name = "uk_history_user_catalog", columnNames = {"user_id", "catalog_id"}))
public class HistoryEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "catalog_id", nullable = false)
	private CatalogEntry catalog;

	private Instant viewedAt = Instant.now();

	public Long getId() {
		return id;
	}

	public UserAccount getUser() {
		return user;
	}

	public void setUser(UserAccount user) {
		this.user = user;
	}

	public CatalogEntry getCatalog() {
		return catalog;
	}

	public void setCatalog(CatalogEntry catalog) {
		this.catalog = catalog;
	}

	public Instant getViewedAt() {
		return viewedAt;
	}

	public void setViewedAt(Instant viewedAt) {
		this.viewedAt = viewedAt;
	}
}
