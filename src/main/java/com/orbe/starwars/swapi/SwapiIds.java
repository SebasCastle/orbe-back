package com.orbe.starwars.swapi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SwapiIds {

	private static final Pattern TRAILING_ID = Pattern.compile("(\\d+)\\D*$");

	private SwapiIds() {
	}

	public static int fromUrl(String url) {
		if (url == null || url.isBlank()) {
			return 0;
		}
		Matcher matcher = TRAILING_ID.matcher(url.trim());
		if (!matcher.find()) {
			return 0;
		}
		try {
			return Integer.parseInt(matcher.group(1));
		} catch (NumberFormatException exception) {
			return 0;
		}
	}
}
