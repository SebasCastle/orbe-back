package com.orbe.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DotenvLoader {

	private DotenvLoader() {
	}

	public static void load() {
		Path[] candidates = {Path.of(".env"), Path.of("..", ".env")};
		for (Path candidate : candidates) {
			Path absolute = candidate.toAbsolutePath().normalize();
			if (!Files.exists(absolute) || absolute.getParent() == null) {
				continue;
			}
			try {
				Dotenv dotenv = Dotenv.configure()
						.directory(absolute.getParent().toString())
						.filename(absolute.getFileName().toString())
						.ignoreIfMalformed()
						.ignoreIfMissing()
						.load();
				dotenv.entries().forEach(entry -> {
					String key = entry.getKey();
					String value = entry.getValue();
					if (key == null || key.isBlank() || value == null || value.isBlank()) {
						return;
					}
					if (System.getenv(key) == null && System.getProperty(key) == null) {
						System.setProperty(key, value);
					}
				});
				return;
			} catch (RuntimeException exception) {
				System.err.println("No se pudo leer " + absolute + ": " + exception.getMessage());
			}
		}
	}
}
