package cc.wikitools.lucene;

import java.io.File;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.similarities.LMDirichletSimilarityFactory;

import cc.wikitools.lucene.IndexWikipediaDump.IndexField;

public class SearchWikipedia {
  private static final int DEFAULT_NUM_RESULTS = 10;

  private static final String INDEX_OPTION = "index";
  private static final String QUERY_OPTION = "q";
  private static final String NUM_RESULTS_OPTION = "num_results";
  private static final String VERBOSE_OPTION = "verbose";

  @SuppressWarnings("static-access")
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("index location").create(INDEX_OPTION));
    options.addOption(OptionBuilder.withArgName("string").hasArg()
        .withDescription("query text").create(QUERY_OPTION));
    options.addOption(OptionBuilder.withArgName("num").hasArg()
        .withDescription("number of results to return").create(NUM_RESULTS_OPTION));
    options.addOption(new Option(VERBOSE_OPTION, "print out complete document"));

    CommandLine cmdline = null;
    CommandLineParser parser = new GnuParser();
    try {
      cmdline = parser.parse(options, args);
    } catch (ParseException exp) {
      System.err.println("Error parsing command line: " + exp.getMessage());
      System.exit(-1);
    }

    if (!cmdline.hasOption(QUERY_OPTION) || !cmdline.hasOption(INDEX_OPTION)
        || !cmdline.hasOption(QUERY_OPTION)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(SearchWikipedia.class.getName(), options);
      System.exit(-1);
    }

    File indexLocation = new File(cmdline.getOptionValue(INDEX_OPTION));
    if (!indexLocation.exists()) {
      System.err.println("Error: " + indexLocation + " does not exist!");
      System.exit(-1);
    }

    String queryText = cmdline.getOptionValue(QUERY_OPTION);
    int numResults = cmdline.hasOption(NUM_RESULTS_OPTION) ?
        Integer.parseInt(cmdline.getOptionValue(NUM_RESULTS_OPTION)) : DEFAULT_NUM_RESULTS;
    boolean verbose = cmdline.hasOption(VERBOSE_OPTION);

    PrintStream out = new PrintStream(System.out, true, "UTF-8");

    IndexReader reader = DirectoryReader.open(FSDirectory.open(indexLocation));
    IndexSearcher searcher = new IndexSearcher(reader);

    NamedList<Double> paramNamedList = new NamedList<Double>();
    paramNamedList.add("mu", 2500.0);
    SolrParams params = SolrParams.toSolrParams(paramNamedList);
    LMDirichletSimilarityFactory factory = new LMDirichletSimilarityFactory();
    factory.init(params);
    Similarity simLMDir = factory.getSimilarity();
    searcher.setSimilarity(simLMDir);

    QueryParser p = new QueryParser(Version.LUCENE_43, IndexField.TEXT.name, IndexWikipediaDump.ANALYZER);
    Query query = p.parse(queryText);

    TopDocs rs = searcher.search(query, numResults);

    int i = 1;
    for (ScoreDoc scoreDoc : rs.scoreDocs) {
      Document hit = searcher.doc(scoreDoc.doc);

      out.println(String.format("%d. %s (id = %s) %f", i,
          hit.getField(IndexField.TITLE.name).stringValue(),
          hit.getField(IndexField.ID.name).stringValue(), 
          scoreDoc.score));
      if (verbose) {
        out.println("# " + hit.toString().replaceAll("[\\n\\r]+", " "));
      }
      i++;
    }

    reader.close();
    out.close();
  }
}
