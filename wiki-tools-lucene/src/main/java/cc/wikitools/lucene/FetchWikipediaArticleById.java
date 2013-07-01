package cc.wikitools.lucene;

import java.io.File;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.document.Document;

import cc.wikitools.lucene.IndexWikipediaDump.IndexField;

public class FetchWikipediaArticleById {
  private static final String INDEX_OPTION = "index";
  private static final String ID_OPTION = "id";

  @SuppressWarnings("static-access")
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("index location").create(INDEX_OPTION));
    options.addOption(OptionBuilder.withArgName("string").hasArg()
        .withDescription("query text").create(ID_OPTION));

    CommandLine cmdline = null;
    CommandLineParser parser = new GnuParser();
    try {
      cmdline = parser.parse(options, args);
    } catch (ParseException exp) {
      System.err.println("Error parsing command line: " + exp.getMessage());
      System.exit(-1);
    }

    if (!cmdline.hasOption(ID_OPTION) || !cmdline.hasOption(INDEX_OPTION)
        || !cmdline.hasOption(ID_OPTION)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(SearchWikipedia.class.getName(), options);
      System.exit(-1);
    }

    File indexLocation = new File(cmdline.getOptionValue(INDEX_OPTION));
    if (!indexLocation.exists()) {
      System.err.println("Error: " + indexLocation + " does not exist!");
      System.exit(-1);
    }

    int id = Integer.parseInt(cmdline.getOptionValue(ID_OPTION));

    PrintStream out = new PrintStream(System.out, true, "UTF-8");

    WikipediaSearcher searcher = new WikipediaSearcher(indexLocation);
    Document doc = searcher.getArticle(id);

    if (doc == null) {
      System.err.print("id " + id + " doesn't exist!\n");
    } else {
      out.println(doc.getField(IndexField.TEXT.name).stringValue());
    }

    searcher.close();
    out.close();
  }
}
