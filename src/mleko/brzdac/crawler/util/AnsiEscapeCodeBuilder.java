/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler.util;

/**
 *
 * @author mleko
 */
public class AnsiEscapeCodeBuilder {

	private String sequence = "";

	public enum Color {

		BLACK, RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE, DEFAULT
	};

	public String build() {
		String s = "\u001B[" + sequence + "m";
		sequence = "";
		return s;
	}

	public AnsiEscapeCodeBuilder reset() {
		append("0");
		return this;
	}

	public AnsiEscapeCodeBuilder foregroundColor(Color color, boolean bright) {
		append("3" + colorToCode(color));
		append(bright ? "1" : "22");
		return this;
	}

	public AnsiEscapeCodeBuilder backgroundColor(Color color, boolean bright) {
		append("4" + colorToCode(color));
		append(bright ? "1" : "22");
		return this;
	}

	private void append(String c) {
		if (sequence.length() > 0) {
			sequence += ";";
		}
		sequence += c;
	}

	private String colorToCode(Color color) {
		switch (color) {
			case BLACK: return "0";
			case RED: return "1";
			case GREEN: return "2";
			case YELLOW: return "3";
			case BLUE: return "4";
			case PURPLE: return "5";
			case CYAN: return "6";
			case WHITE: return "7";
			case DEFAULT: return "9";
		}
		return "";
	}
}
