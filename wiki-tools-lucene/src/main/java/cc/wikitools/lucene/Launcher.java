package cc.wikitools.lucene;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Launcher {
  public static void main(String[] args) throws Exception {
    Class<?> cls = Class.forName(args[0]);
    Method method = cls.getMethod("main", String[].class);
    String[] params = new String[args.length-1];

    System.arraycopy(args, 1, params, 0, args.length-1);
    System.out.println("Invoking " + args[0] + " with " + Arrays.toString(params));
    method.invoke(null, (Object) params);
  }
}
