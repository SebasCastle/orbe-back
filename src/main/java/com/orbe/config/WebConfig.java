package com.orbe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.LinkedHashSet;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final OrbeProperties properties;

	public WebConfig(OrbeProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		LinkedHashSet<String> origins = new LinkedHashSet<>();
		origins.add("http://localhost:5173");
		origins.add("http://127.0.0.1:5173");
		if (properties.frontendUrl() != null && !properties.frontendUrl().isBlank()) {
			String origin = properties.frontendUrl().trim();
			if (origin.endsWith("/")) {
				origin = origin.substring(0, origin.length() - 1);
			}
			origins.add(origin);
		}
		registry.addMapping("/api/**")
				.allowedOrigins(origins.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
