package core;

import java.io.Serializable;
import java.util.Objects;

/**
 * TODO Add some meaningful class description...
 */
public class Term implements Serializable {

	private static final long serialVersionUID = 6707760926463804873L;

	private final String text;

	private final Case casing;

	public Term(String text) {
		Objects.requireNonNull(text);
		text = text.trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException("'text' is empty");
		}

		this.casing = Case.detect(text);
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public Case getCase() {
		return casing;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Term term = (Term) o;
		if (!text.equals(term.text)) return false;
		return casing == term.casing;
	}

	@Override
	public int hashCode() {
		int result = text.hashCode();
		result = 31 * result + casing.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return casing + "_" + text;
	}

}
