package com.orbe.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;

	public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
		this.jwtAuthFilter = jwtAuthFilter;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) -> write(response, HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar."))
						.accessDeniedHandler((request, response, exception) -> write(response, HttpStatus.FORBIDDEN, "No tienes permiso para hacer eso.")))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/auth/**", "/api/health", "/error").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/cinema/search", "/api/cinema/titles/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/starwars/films", "/api/starwars/films/*").permitAll()
						.requestMatchers("/api/spotify/login", "/api/login", "/api/spotify/callback", "/api/get-user-code").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/books/**", "/api/music/**", "/api/catalog/**", "/api/community/**", "/api/ratings").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	private void write(HttpServletResponse response, HttpStatus status, String message) throws java.io.IOException {
		response.setStatus(status.value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"message\":\"" + message + "\",\"status\":" + status.value() + "}");
	}
}
