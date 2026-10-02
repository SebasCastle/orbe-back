package com.orbe.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record UserProfile(Long id, String username, String email, String displayName) {
	}

	public record AuthResponse(String token, UserProfile user) {
	}

	public record RegisterRequest(
			@NotBlank(message = "Elige un nombre de usuario.")
			@Pattern(regexp = "[A-Za-z0-9_]{3,24}", message = "El usuario debe tener 3 a 24 letras, números o guion bajo.")
			String username,
			@NotBlank(message = "Escribe tu correo.")
			@Email(message = "El correo no es válido.")
			@Size(max = 180, message = "El correo es demasiado largo.")
			String email,
			@NotBlank(message = "Elige una contraseña.")
			@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
			String password,
			@Size(max = 80, message = "El nombre es demasiado largo.")
			String displayName
	) {
	}

	public record LoginRequest(
			@NotBlank(message = "Escribe tu correo.")
			@Email(message = "El correo no es válido.")
			String email,
			@NotBlank(message = "Escribe tu contraseña.")
			String password
	) {
	}
}
