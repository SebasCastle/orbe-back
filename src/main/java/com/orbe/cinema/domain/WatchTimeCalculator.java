package com.orbe.cinema.domain;

public final class WatchTimeCalculator {

	private int total;

	public void include(int minutes) {
		this.total += minutes;
	}

	public int total() {
		return total;
	}
}
