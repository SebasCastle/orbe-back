package com.orbe.cinema.domain;

public final class RecommendationFilter {

	public int classification(double average) {
		if (average <= 0) {
			return 0;
		}
		return (int) Math.round(average);
	}

	public String recommend(double average) {
		if (average >= 4) {
			return "Está entre los favoritos del momento";
		}
		if (average >= 2) {
			return "Bien evaluado actualmente";
		}
		return "Colócalo en tu lista para verlo después";
	}
}
