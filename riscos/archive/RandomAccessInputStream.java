// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessInputStream extends InputStream {

  RandomAccessFile raf;

  public RandomAccessInputStream(String filename) throws IOException {
    raf = new RandomAccessFile(filename, "r");
  }

  public void close() throws IOException {
    raf.close();
  }

  public int read() throws IOException {
    return raf.read();
  }

  public int read(byte[] buf) throws IOException {
    return read(buf, 0, buf.length);
  }

  public int read(byte[] buf, int offset, int length) throws IOException {
    return raf.read(buf, offset, length);
  }

  public int available() throws IOException {
    return (int)(raf.length() - raf.getFilePointer());
  }

  public boolean markSupported() {
    return false;
  }

  public long skip(long bytesToSkip) throws IOException {
    return raf.skipBytes((int)bytesToSkip);
  }

  public long getFilePointer() throws IOException {
    return raf.getFilePointer();
  }

  public void seek(long pos) throws IOException {
    raf.seek(pos);
  }
}
