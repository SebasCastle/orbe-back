package com.orbe.spotify.repository;

import com.orbe.spotify.model.SpotifyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotifyAccountRepository extends JpaRepository<SpotifyAccount, Long> {

	Optional<SpotifyAccount> findByRefId(String refId);
}
