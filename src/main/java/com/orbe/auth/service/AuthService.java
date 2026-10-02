package com.orbe.auth.service;

import com.orbe.auth.dto.AuthDtos.AuthResponse;
import com.orbe.auth.dto.AuthDtos.LoginRequest;
import com.orbe.auth.dto.AuthDtos.RegisterRequest;
import com.orbe.auth.dto.AuthDtos.UserProfile;
import com.orbe.auth.model.UserAccount;
import com.orbe.auth.repository.UserAccountRepository;
import com.orbe.common.ApiException;
import com.orbe.common.Texts;
import com.orbe.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

	private final UserAccountRepository users;
	private final PasswordEncoder passwords;
	private final JwtService tokens;

	public AuthService(UserAccountRepository users, PasswordEncoder passwords, JwtService tokens) {
		this.users = users;
		this.passwords = passwords;
		this.tokens = tokens;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		String username = request.username().trim();
		if (users.existsByEmailIgnoreCase(email) || users.existsByUsernameIgnoreCase(username)) {
			throw new ApiException(HttpStatus.CONFLICT, "Ese correo o usuario ya está registrado.");
		}
		UserAccount user = new UserAccount();
		user.setEmail(email);
		user.setUsername(username);
		user.setDisplayName(Texts.blank(request.displayName()) ? username : request.displayName().trim());
		user.setPasswordHash(passwords.encode(request.password()));
		user.setCreatedAt(Instant.now());
		return response(users.save(user));
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		UserAccount user = users.findByEmailIgnoreCase(request.email().trim())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));
		if (!passwords.matches(request.password(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
		}
		return response(user);
	}

	public static UserProfile profile(UserAccount user) {
		return new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName());
	}

	private AuthResponse response(UserAccount user) {
		return new AuthResponse(tokens.issue(user), profile(user));
	}
}
