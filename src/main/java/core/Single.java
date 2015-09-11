package core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add some meaningful class description...
 */
public class Single implements Serializable {

	private static final long serialVersionUID = 8119506048407056509L;

	private static final Logger logger = LoggerFactory.getLogger(Single.class);

	private final Map<String, Map<Term, Long>> memory;

	private Single(Builder builder) {
		Objects.requireNonNull(builder);

		memory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		memory.putAll(builder.memory);
	}

	public static Single load(Path path) {
		Objects.requireNonNull(path);

		Single result = null;
		try {
			InputStream stream = Files.newInputStream(path);
			ObjectInputStream output = new ObjectInputStream(stream);
			result = (Single) output.readObject();
			output.close();
			stream.close();
		} catch (ClassNotFoundException | IOException e) {
			logger.warn(e.toString());
		}
		return result;
	}

	public void save(Path path) {
		Objects.requireNonNull(path);

		try {
			OutputStream stream = Files.newOutputStream(path);
			ObjectOutputStream output = new ObjectOutputStream(stream);
			output.writeObject(this);
			output.close();
			stream.close();
		} catch (IOException e) {
			logger.warn(e.toString());
		}
	}

	public Case suggest(String word) {
		Objects.requireNonNull(word);
		word = word.trim();
		if (word.isEmpty()) {
			throw new IllegalArgumentException("'word' is empty");
		}

		Map<Term, Long> map = memory.get(word);
		if (null == map || map.isEmpty()) {
			return null; // or self case?
		}
		List<Map.Entry<Term, Long>> entries = new ArrayList(map.entrySet());
		Collections.sort(entries, (o1, o2) -> Long.compare(o2.getValue(), o1.getValue()));
		return entries.get(0).getKey().getCase();
	}

	public static class Builder {

		private final Map<String, Map<Term, Long>> memory;

		public Builder() {
			memory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		}

		public Builder(Single single) {
			this();
			Objects.requireNonNull(single);
			memory.putAll(single.memory);
		}

		public Builder log(String word) {
			Objects.requireNonNull(word);
			word = word.trim();
			if (word.isEmpty()) {
				throw new IllegalArgumentException("'word' is empty");
			}

			Map<Term, Long> map = memory.get(word);
			if (null == map) {
				map = new HashMap<>();
				memory.put(word.toLowerCase(), map);
			}
			Term term = new Term(word);
			map.put(term, 1L + map.getOrDefault(term, 0L));
			return this;
		}

		public Single build() {
			return new Single(this);
		}

	}

}
