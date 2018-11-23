// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LimitOutputStream extends FilterOutputStream {

  private int totalSize;
  private OutputStream os;

  public LimitOutputStream(OutputStream out, int size) {
    super(out);
    totalSize = size;
    os = out;
  }

  public void close() throws IOException {
    flush();
  }

  public void write(int val) throws IOException {
    if (totalSize > 0) {
      os.write(val);
      totalSize--;
    }
  }

  public void write(byte[] buf) throws IOException {
    write(buf, 0, buf.length);
  }

  public void write(byte[] buf, int off, int len) throws IOException {
    int bytesToWrite;

    if (totalSize > 0) {
      bytesToWrite = totalSize > len ? len : totalSize;
      os.write(buf, off, bytesToWrite);
      totalSize -= bytesToWrite;
    }
  }

  public void flush() throws IOException {
  }
}
