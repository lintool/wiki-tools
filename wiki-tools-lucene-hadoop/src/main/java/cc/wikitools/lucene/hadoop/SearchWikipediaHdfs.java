/**
 * wiki-tools-lucene-hadoop: Java tools for searching Wikipedia Lucene indexes in HDFS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.wikitools.lucene.hadoop;

import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import cc.wikitools.lucene.IndexWikipediaDump.IndexField;

public class SearchWikipediaHdfs extends Configured implements Tool {
  private static final int DEFAULT_NUM_RESULTS = 10;

  private static final String INDEX_OPTION = "index";
  private static final String QUERY_OPTION = "q";
  private static final String NUM_RESULTS_OPTION = "num_results";
  private static final String VERBOSE_OPTION = "verbose";
  private static final String ARTICLE_OPTION = "article";
  private static final String TITLE_OPTION = "title";

  @SuppressWarnings("static-access")
  @Override
  public int run(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("index location").create(INDEX_OPTION));
    options.addOption(OptionBuilder.withArgName("string").hasArg()
        .withDescription("query text").create(QUERY_OPTION));
    options.addOption(OptionBuilder.withArgName("num").hasArg()
        .withDescription("number of results to return").create(NUM_RESULTS_OPTION));

    options.addOption(new Option(VERBOSE_OPTION, "print out complete document"));
    options.addOption(new Option(TITLE_OPTION, "search title"));
    options.addOption(new Option(ARTICLE_OPTION, "search article"));

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
      formatter.printHelp(SearchWikipediaHdfs.class.getName(), options);
      System.exit(-1);
    }

    String indexLocation = cmdline.getOptionValue(INDEX_OPTION);
    String queryText = cmdline.getOptionValue(QUERY_OPTION);
    int numResults = cmdline.hasOption(NUM_RESULTS_OPTION) ?
        Integer.parseInt(cmdline.getOptionValue(NUM_RESULTS_OPTION)) : DEFAULT_NUM_RESULTS;
    boolean verbose = cmdline.hasOption(VERBOSE_OPTION);
    boolean searchArticle = !cmdline.hasOption(TITLE_OPTION);

    PrintStream out = new PrintStream(System.out, true, "UTF-8");

    HdfsWikipediaSearcher searcher = 
        new HdfsWikipediaSearcher(new Path(indexLocation), getConf());
    TopDocs rs = null;
    if (searchArticle) {
      rs = searcher.searchArticle(queryText, numResults);
    } else {
      rs = searcher.searchTitle(queryText, numResults);
    }

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

    searcher.close();
    out.close();

    return 0;
  }

  public static void main(String[] args) throws Exception {
    ToolRunner.run(new SearchWikipediaHdfs(), args);
  }
}
