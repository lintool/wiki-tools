package cc.wikitools.lucene;

import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;

public class DummyTest {

  @Test
  public void testScrewyRefs() {
    assertTrue(true);
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(DummyTest.class);
  }
}
