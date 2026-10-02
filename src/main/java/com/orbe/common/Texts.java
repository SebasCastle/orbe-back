package com.orbe.common;

public final class Texts {

	private Texts() {
	}

	public static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	public static String clean(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("N/A") || trimmed.equalsIgnoreCase("null")) {
			return null;
		}
		return trimmed;
	}
}
