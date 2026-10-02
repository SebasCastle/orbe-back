package com.orbe.cinema.domain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

	private static final Pattern NUMBER = Pattern.compile("(\\d+)");

	private DurationParser() {
	}

	public static Integer minutesOrNull(String runtime) {
		if (runtime == null || runtime.isBlank() || runtime.toUpperCase(Locale.ROOT).contains("N/A")) {
			return null;
		}
		Matcher matcher = NUMBER.matcher(runtime);
		if (!matcher.find()) {
			return null;
		}
		return Integer.parseInt(matcher.group(1));
	}

	public static int requireMinutes(String runtime) {
		Integer minutes = minutesOrNull(runtime);
		if (minutes == null) {
			throw new Errores("No pude convertir la duración, porque contiene un N/A");
		}
		return minutes;
	}
}
