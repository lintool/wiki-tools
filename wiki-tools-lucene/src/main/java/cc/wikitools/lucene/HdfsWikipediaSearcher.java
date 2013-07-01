package cc.wikitools.lucene;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.store.Directory;

import cc.wikitools.lucene.hadoop.FileSystemDirectory;

public class HdfsWikipediaSearcher extends WikipediaSearcher{

  public HdfsWikipediaSearcher(String indexLocation) throws IOException {
    super(indexLocation);
  }

  protected Directory createDirectory(String indexLocation) throws IOException {
    Configuration conf = new Configuration();
    FileSystem fs = FileSystem.get(conf);
    Path path = new Path(indexLocation);
    return new FileSystemDirectory(fs, path, false, conf);
  }
}
