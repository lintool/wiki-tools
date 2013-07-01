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
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
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

import com.google.common.base.Preconditions;

public class WikipediaSearcher {
  private IndexReader reader;
  private IndexSearcher searcher;
  private QueryParser parserArticle;
  private QueryParser parserTitle;

  public WikipediaSearcher(File indexLocation) throws IOException {
    Preconditions.checkNotNull(indexLocation);
    Preconditions.checkArgument(indexLocation.exists());

    reader = DirectoryReader.open(FSDirectory.open(indexLocation));
    searcher = new IndexSearcher(reader);

    NamedList<Double> paramNamedList = new NamedList<Double>();
    paramNamedList.add("mu", 2500.0);
    SolrParams params = SolrParams.toSolrParams(paramNamedList);
    LMDirichletSimilarityFactory factory = new LMDirichletSimilarityFactory();
    factory.init(params);
    Similarity simLMDir = factory.getSimilarity();
    searcher.setSimilarity(simLMDir);

    parserArticle = new QueryParser(Version.LUCENE_43, IndexField.TEXT.name, IndexWikipediaDump.ANALYZER);
    parserTitle = new QueryParser(Version.LUCENE_43, IndexField.TITLE.name, IndexWikipediaDump.ANALYZER);
  }

  public TopDocs searchArticle(String q, int numResults) {
    try {
      Query query = parserArticle.parse(q);
      return searcher.search(query, numResults);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public TopDocs searchTitle(String q, int numResults) {
    try {
      Query query = parserTitle.parse(q);
      return searcher.search(query, numResults);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public Document doc(int docid) {
    try {
      return searcher.doc(docid);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public Document getArticle(int id) {
    try {
      Query query = NumericRangeQuery.newIntRange(IndexField.ID.name, id, id, true, true);
      TopDocs rs = searcher.search(query, 1);

      if (rs.totalHits == 0) {
        return null;
      }
      ScoreDoc scoreDoc = rs.scoreDocs[0];
      Document hit = searcher.doc(scoreDoc.doc);

      return hit;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public void close() {
    try {
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
