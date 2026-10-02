package com.orbe.auth.controller;

import com.orbe.auth.dto.AuthDtos.AuthResponse;
import com.orbe.auth.dto.AuthDtos.LoginRequest;
import com.orbe.auth.dto.AuthDtos.RegisterRequest;
import com.orbe.auth.dto.AuthDtos.UserProfile;
import com.orbe.auth.service.AuthService;
import com.orbe.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService auth;
	private final CurrentUser currentUser;

	public AuthController(AuthService auth, CurrentUser currentUser) {
		this.auth = auth;
		this.currentUser = currentUser;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return auth.login(request);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public UserProfile me() {
		return AuthService.profile(currentUser.require());
	}
}
