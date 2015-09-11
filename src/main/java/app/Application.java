package app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import core.Single;
import edu.emory.clir.clearnlp.component.AbstractComponent;
import edu.emory.clir.clearnlp.component.mode.dep.DEPConfiguration;
import edu.emory.clir.clearnlp.component.mode.srl.SRLConfiguration;
import edu.emory.clir.clearnlp.component.utils.GlobalLexica;
import edu.emory.clir.clearnlp.component.utils.NLPUtils;
import edu.emory.clir.clearnlp.dependency.DEPNode;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.emory.clir.clearnlp.tokenization.AbstractTokenizer;
import edu.emory.clir.clearnlp.util.lang.TLanguage;
import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import org.xml.sax.SAXException;

/**
 * TODO Add some meaningful class description...
 */
public class Application {
	public static final int LIMIT = 1_000_000;

	public static final Path RESOURCE = Paths.get("./single.dat");

	public static final List<String> BAD_TAGS =
			Arrays.asList("$", ":", ",", ".", "``", "''", "\"", "-LRB-", "-RRB-", "HYPH", "NFP", "PUNC");
	private static final List<String> BAD_LEMMAS =
			Arrays.asList("#crd#", "#hlink#", "#ord#", "'", "'s", "-", "0");

	public static long elapsed;

	public static void main(String[] args) throws IOException, SAXException {
		if (args.length < 1) {
			System.err.println("Usage: Parser <xml-file.[bz2|gz]>");
			System.exit(-1);
		}

		elapsed = System.nanoTime();

		System.err.println("Loading NLP...");
		List<String> paths = new ArrayList<>();
		paths.add("brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz");
		GlobalLexica.initDistributionalSemanticsWords(paths);
		GlobalLexica.initNamedEntityDictionary("general-en-ner-gazetteer.xz");
		AbstractComponent[] components = new AbstractComponent[5];
		components[0] = NLPUtils.getPOSTagger(TLanguage.ENGLISH, "general-en-pos.xz");
		components[1] = NLPUtils.getMPAnalyzer(TLanguage.ENGLISH);
		components[2] = NLPUtils.getDEPParser(TLanguage.ENGLISH, "general-en-dep.xz", new DEPConfiguration("root"));
		components[3] = NLPUtils.getSRLabeler(TLanguage.ENGLISH, "general-en-srl.xz", new SRLConfiguration(4, 3));
		components[4] = NLPUtils.getNERecognizer(TLanguage.ENGLISH, "general-en-ner.xz");
		AbstractTokenizer tokenizer = NLPUtils.getTokenizer(TLanguage.ENGLISH);

		System.err.println("Loading WikiDump...");
		Path path = Paths.get(args[0]);
		InputStream stream = Files.newInputStream(path);
		WikiModel model = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
		Single.Builder builder = new Single.Builder();
		IArticleFilter filter = new IArticleFilter() {
			private int i = 0;

			@Override
			public void process(WikiArticle article, Siteinfo siteinfo) throws SAXException {
				Objects.requireNonNull(article);
				Objects.requireNonNull(siteinfo);

				if (article.isMain()) {
					try {
						model.setUp();
						String plain = model.render(new PlainTextConverter(), article.getText());
						plain = filter(plain);
						if (!plain.isEmpty()) {
							for (List<String> sentence :
									tokenizer.segmentize(new ByteArrayInputStream(plain.getBytes()))) {
								DEPTree tree = new DEPTree(sentence);
								for (AbstractComponent component : components) {
									component.process(tree);
								}
								for (DEPNode node : tree) {
									if (!BAD_TAGS.contains(node.getPOSTag()) &&
											!BAD_LEMMAS.contains(node.getLemma())) {
										builder.log(node.getWordForm());
									}
								}
							}
						}
					} finally {
						model.tearDown();
					}
				}
				i += 1;
				if (0 == (i % 1_000)) {
					System.err.format("%d articles out of %d processed so far...\n", i, LIMIT);
				}
				if (i >= LIMIT) {
					Single single = builder.build();
					single.save(RESOURCE);
					System.err.println("Resource file saved!");
					elapsed = System.nanoTime() - elapsed;
					System.err.format("Elaboration completed in %.3fs", (double) elapsed / 1_000_000_000.0);
					System.exit(0);
				}
			}
		};
		WikiXMLParser parser = new WikiXMLParser(stream, filter);
		parser.parse();
	}

	private static String filter(String content) {
		Objects.requireNonNull(content);

		content = content.trim();
		for (int i = content.lastIndexOf("{{"); i > -1; i = content.lastIndexOf("{{", i)) {
			int p = content.indexOf("}}", i);
			if (p != -1) {
				content = content.substring(0, i) + " " + content.substring(p + 2);
			}
		}
		List<String> list = new ArrayList<>();
		String[] lines = content.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (!line.isEmpty()) {
				list.add(line);
			}
		}
		return String.join("\n", list);
	}
}
