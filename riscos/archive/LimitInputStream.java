// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitInputStream extends FilterInputStream {

  private int totalSize;
  private InputStream is;

  public LimitInputStream(InputStream in, int totalSiz) {
    super(in);
    is = in;
    totalSize = totalSiz;
  }

  public void close() {
  }

  public int read() throws IOException {
    if (totalSize == 0) {
      return -1;
    }

    int b = is.read();
    if (b != -1) {
      --totalSize;
    }

    return b;
  }

  public int read(byte[] buf) throws IOException {
    return read(buf, 0, buf.length);
  }

  public int read(byte[] buf, int off, int len) throws IOException {
    int size;

    if (totalSize == 0) {
      return -1;
    }

    size = totalSize > len ? len : totalSize;
    int r = is.read(buf, off, size);
    if (r == -1) {
      return -1;
    }
    totalSize -= r;

    return r;
  }

}
