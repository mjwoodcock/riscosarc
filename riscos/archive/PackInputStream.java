// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class PackInputStream extends FilterInputStream {

  private byte[] ncrBuf;
  private int ncrBufI;
  private int ncrBufLen;
  private int ncrBufsize;
  private PushbackInputStream is;
  private static final int DEFAULT_BUFFER_SIZE = 1024;
  private static final byte RUNMARK = (byte)0x90;
  protected boolean eos;

  private int readNcrBuf() {
    if (ncrBufI == -1 || ncrBufI >= ncrBufLen) {
      int r;
      try {
        r = is.read(ncrBuf, 0, ncrBufsize);
      } catch (IOException e) {
        r = -1;
      }

      ncrBufLen = r;
      if (r == -1) {
        return r;
      } else if (r == ncrBufsize) {
        try {
          if (ncrBuf[ncrBufLen - 1] == RUNMARK) {
            ncrBuf[ncrBufLen++] = (byte)is.read();
          } else {
            int b = is.read();
            if ((byte)b == RUNMARK) {
              ncrBuf[ncrBufLen++] = RUNMARK;
              b = is.read();
              ncrBuf[ncrBufLen++] = (byte)b;
            } else {
              is.unread(b);
            }
          }
        } catch (IOException e) {
          // If we can't read any more, then we must be at the
          // end of the stream, therefore don't worry about the
          // exception
        }
      }
      ncrBufI = 0;
    }

    return ncrBufLen;
  }

  public PackInputStream(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE);
  }

  public PackInputStream(InputStream in, int size) {
    super(in);
    ncrBufsize = size;
    ncrBuf = new byte[size + 2]; // We may need to add 0x90 and count bytes
    ncrBufI = -1;
    is = new PushbackInputStream(in);
  }

  public void close() {
  }

  public int read() {
    byte[] b = new byte[1];

    int r = read(b, 0, 1);
    if (r == -1) {
      return r;
    }

    return (int)b[0];
  }

  public int read(byte[] buf, int off, int len) {
    int l = 0;

    while (l < len) {
      int r = readNcrBuf();
      if (r == -1) {
        if (l == 0) {
          return -1;
        } else {
          return l;
        }
      }

      if (ncrBuf[ncrBufI] == RUNMARK) {
        if (ncrBuf[ncrBufI + 1] == 0) {
          buf[off + l] = RUNMARK;
          ncrBufI += 2;
          l++;
        } else {
          while (l < len && ((int)(--ncrBuf[ncrBufI + 1]) & 0xFF) > 0) {
            buf[off + l] = ncrBuf[ncrBufI - 1];
            l++;
            if (ncrBuf[ncrBufI + 1] == 1) {
              ncrBufI += 2;
              break;
            }
          }
        }
      } else {
        buf[off + l] = ncrBuf[ncrBufI];
        l++;
        ncrBufI++;
      }

    }

    return l;
  }
}
