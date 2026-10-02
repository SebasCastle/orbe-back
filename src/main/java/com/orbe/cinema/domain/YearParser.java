package com.orbe.cinema.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YearParser {

	private static final Pattern YEAR = Pattern.compile("(\\d{4})");

	private YearParser() {
	}

	public static int yearOrZero(String year) {
		if (year == null) {
			return 0;
		}
		Matcher matcher = YEAR.matcher(year);
		if (!matcher.find()) {
			return 0;
		}
		return Integer.parseInt(matcher.group(1));
	}
}
