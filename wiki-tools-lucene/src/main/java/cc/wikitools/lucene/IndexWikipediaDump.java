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
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wikiclean.WikiClean;
import org.wikiclean.WikiCleanBuilder;
import org.wikiclean.WikipediaDumpBz2InputStream;

public class IndexWikipediaDump {
  private static final Logger LOG = Logger.getLogger(IndexWikipediaDump.class);

  public static final Analyzer ANALYZER = new StandardAnalyzer(Version.LUCENE_43);

  public static enum IndexField {
    ID("id"),
    TITLE("title"),
    TEXT("text");

    public final String name;

    IndexField(String s) {
      name = s;
    }
  };

  private static final String INPUT_OPTION = "input";
  private static final String INDEX_OPTION = "index";
  private static final String MAX_OPTION = "maxdocs";

  @SuppressWarnings("static-access")
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("gzipped XML dump file").create(INPUT_OPTION));
    options.addOption(OptionBuilder.withArgName("dir").hasArg()
        .withDescription("index location").create(INDEX_OPTION));
    options.addOption(OptionBuilder.withArgName("num").hasArg()
        .withDescription("maximum number of documents to index").create(MAX_OPTION));

    CommandLine cmdline = null;
    CommandLineParser parser = new GnuParser();
    try {
      cmdline = parser.parse(options, args);
    } catch (ParseException exp) {
      System.err.println("Error parsing command line: " + exp.getMessage());
      System.exit(-1);
    }

    if (!cmdline.hasOption(INPUT_OPTION) || !cmdline.hasOption(INDEX_OPTION)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(IndexWikipediaDump.class.getCanonicalName(), options);
      System.exit(-1);
    }

    String indexPath = cmdline.getOptionValue(INDEX_OPTION);
    int maxdocs = cmdline.hasOption(MAX_OPTION) ?
        Integer.parseInt(cmdline.getOptionValue(MAX_OPTION)) : Integer.MAX_VALUE;

    final FieldType textOptions = new FieldType();
    textOptions.setIndexed(true);
    textOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    textOptions.setStored(true);
    textOptions.setTokenized(true);        

    long startTime = System.currentTimeMillis();

    String path = cmdline.getOptionValue(INPUT_OPTION);
    PrintStream out = new PrintStream(System.out, true, "UTF-8");
    WikiClean cleaner = new WikiCleanBuilder().withTitle(true).build();

    Directory dir = FSDirectory.open(new File(indexPath));
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, ANALYZER);
    config.setOpenMode(OpenMode.CREATE);

    IndexWriter writer = new IndexWriter(dir, config);
    LOG.info("Creating index at " + indexPath);

    try {
      WikipediaDumpBz2InputStream stream = new WikipediaDumpBz2InputStream(path);

      int cnt = 0;
      String page;
      while ((page = stream.readNext()) != null) {
        Document doc = new Document();
        doc.add(new IntField(IndexField.ID.name, Integer.parseInt(cleaner.getId(page)), Field.Store.YES));
        doc.add(new Field(IndexField.TEXT.name, cleaner.clean(page), textOptions));
        doc.add(new Field(IndexField.TITLE.name, cleaner.getTitle(page), textOptions));

        writer.addDocument(doc);

        cnt++;
        if (cnt % 10000 == 0) {
          LOG.info(cnt + " statuses indexed");
        }
        if (cnt >= maxdocs) {
          break;
        }
      }

      LOG.info("Total of " + cnt + " docs indexed.");
      LOG.info("Total elapsed time: " + (System.currentTimeMillis() - startTime) + "ms");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      writer.close();
      dir.close();
      out.close();
    }
  }
}
