/**
 * wiki-tools-lucene: Java package for searching Wikipedia dumps with Lucene
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

public class FetchWikipediaArticle {
  private static final String INDEX_OPTION = "index";
  private static final String ID_OPTION = "id";
  private static final String TITLE_OPTION = "title";

  @SuppressWarnings("static-access")
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("index location").create(INDEX_OPTION));
    options.addOption(OptionBuilder.withArgName("num").hasArg()
        .withDescription("article id").create(ID_OPTION));
    options.addOption(OptionBuilder.withArgName("string").hasArg()
        .withDescription("article title").create(TITLE_OPTION));

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

    File indexLocation = new File(cmdline.getOptionValue(INDEX_OPTION));
    if (!indexLocation.exists()) {
      System.err.println("Error: " + indexLocation + " does not exist!");
      System.exit(-1);
    }

    WikipediaSearcher searcher = new WikipediaSearcher(indexLocation);
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
  }
}
