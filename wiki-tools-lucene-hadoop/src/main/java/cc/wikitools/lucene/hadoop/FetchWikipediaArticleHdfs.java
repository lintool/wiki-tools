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
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.lucene.document.Document;

import cc.wikitools.lucene.FetchWikipediaArticle;
import cc.wikitools.lucene.IndexWikipediaDump.IndexField;

public class FetchWikipediaArticleHdfs extends Configured implements Tool {
  private static final String INDEX_OPTION = "index";
  private static final String ID_OPTION = "id";
  private static final String TITLE_OPTION = "title";

  @SuppressWarnings("static-access")
  @Override
  public int run(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("index location").create(INDEX_OPTION));
    options.addOption(OptionBuilder.withArgName("num").hasArg()
        .withDescription("id").create(ID_OPTION));
    options.addOption(OptionBuilder.withArgName("string").hasArg()
        .withDescription("title").create(TITLE_OPTION));

    CommandLine cmdline = null;
    CommandLineParser parser = new GnuParser();
    try {
      cmdline = parser.parse(options, args);
    } catch (ParseException exp) {
      System.err.println("Error parsing command line: " + exp.getMessage());
      System.exit(-1);
    }

    if (!(cmdline.hasOption(ID_OPTION) || cmdline.hasOption(TITLE_OPTION)) || 
        !cmdline.hasOption(INDEX_OPTION)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(FetchWikipediaArticle.class.getName(), options);
      System.exit(-1);
    }

    String indexLocation = cmdline.getOptionValue(INDEX_OPTION);

    HdfsWikipediaSearcher searcher =
        new HdfsWikipediaSearcher(new Path(indexLocation), getConf());
    PrintStream out = new PrintStream(System.out, true, "UTF-8");

    if (cmdline.hasOption(ID_OPTION)) {
      int id = Integer.parseInt(cmdline.getOptionValue(ID_OPTION));
      Document doc = searcher.getArticle(id);

      if (doc == null) {
        System.err.print("id " + id + " doesn't exist!\n");
      } else {
        out.println(doc.getField(IndexField.TEXT.name).stringValue());
      }
    } else {
      String title = cmdline.getOptionValue(TITLE_OPTION);
      Document doc = searcher.getArticle(title);

      if (doc == null) {
        System.err.print("article \"" + title+ "\" doesn't exist!\n");
      } else {
        out.println(doc.getField(IndexField.TEXT.name).stringValue());
      }
    }

    searcher.close();
    out.close();

    return 0;
  }

  public static void main(String[] args) throws Exception {
    ToolRunner.run(new FetchWikipediaArticleHdfs(), args);
  }
}
