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

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;

import cc.wikitools.lucene.WikipediaSearcher;

public class HdfsWikipediaSearcher extends WikipediaSearcher{
  public HdfsWikipediaSearcher(Path indexLocation, Configuration conf) throws IOException {
    FileSystem fs = FileSystem.get(conf);
    Directory directory = new FileSystemDirectory(fs, indexLocation, false, conf);
    
    reader = DirectoryReader.open(directory);

    init();
  }
}
