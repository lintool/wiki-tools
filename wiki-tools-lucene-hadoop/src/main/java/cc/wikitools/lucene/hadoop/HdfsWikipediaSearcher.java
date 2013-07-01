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
