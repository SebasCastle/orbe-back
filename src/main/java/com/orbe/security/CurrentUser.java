package com.orbe.security;

import com.orbe.auth.model.UserAccount;
import com.orbe.auth.repository.UserAccountRepository;
import com.orbe.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUser {

	private final UserAccountRepository users;

	public CurrentUser(UserAccountRepository users) {
		this.users = users;
	}

	public Optional<UserAccount> optional() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount principal) || principal.getId() == null) {
			return Optional.empty();
		}
		return Optional.of(users.getReferenceById(principal.getId()));
	}

	public UserAccount require() {
		return optional().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar."));
	}
}
