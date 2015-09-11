package core;

import java.util.Objects;

/**
 * TODO Add some meaningful class description...
 */
public enum Case {
	FIRST_CASE("FC"), LOWER_CASE("LC"), MIXED_CASE("MC"), UPPER_CASE("UC");

	private final String tag;

	Case(String tag) {
		Objects.requireNonNull(tag);
		tag = tag.trim();
		if (tag.isEmpty()) {
			throw new IllegalArgumentException("'tag' is empty");
		}

		this.tag = tag;
	}

	public static Case detect(String text) {
		Objects.requireNonNull(text);
		text = text.trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException("'text' is empty");
		}

		if (text.toLowerCase().equals(text)) {
			return LOWER_CASE;
		}
		if (text.toUpperCase().equals(text)) {
			return UPPER_CASE;
		}
		if (text.substring(0, 1).toUpperCase().equals(text.substring(0, 1)) &&
				text.substring(1).toLowerCase().equals(text.substring(1))) {
			return FIRST_CASE;
		}
		return MIXED_CASE;
	}

	@Override
	public String toString() {
		return tag;
	}

}
