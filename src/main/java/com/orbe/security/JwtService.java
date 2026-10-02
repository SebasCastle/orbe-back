package com.orbe.security;

import com.orbe.auth.model.UserAccount;
import com.orbe.common.ApiException;
import com.orbe.config.OrbeProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

	private final OrbeProperties properties;

	public JwtService(OrbeProperties properties) {
		this.properties = properties;
	}

	public String issue(UserAccount user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("uid", user.getId())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(60L * 60 * 24 * 7)))
				.signWith(key())
				.compact();
	}

	public String email(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(key())
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return claims.getSubject();
	}

	private SecretKey key() {
		String secret = properties.jwt() == null ? null : properties.jwt().secret();
		if (secret == null || secret.trim().length() < 32) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Falta JWT_SECRET en el entorno.");
		}
		return Keys.hmacShaKeyFor(secret.trim().getBytes(StandardCharsets.UTF_8));
	}
}
