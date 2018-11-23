// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class BitInputStream extends FilterInputStream {

  private static final int EIGHT = 8;
  private static final int DEFAULT_BIT_SIZE = EIGHT;
  private static final int DEFAULT_BUFFER_SIZE = 1024;
  private long buf;
  private int bitSize;
  private long bitMask;
  private int bitsAvail;
  private InputStream is;
  private byte[] byteBuf;
  private int byteBufI;
  private int byteBufLen;

  public BitInputStream(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE, DEFAULT_BIT_SIZE);
  }

  public BitInputStream(InputStream in, int size) {
    this(in, size, DEFAULT_BIT_SIZE);
  }

  public BitInputStream(InputStream in, int size, int bs) {
    super(in);
    bitSize = bs;
    bitMask = (1 << bs) - 1;
    bitsAvail = 0;
    buf = 0;
    is = in;
    byteBuf = new byte[20];
    byteBufI = 0;
    byteBufLen = 0;
  }

  public void setBitSize(int bs) {
    if (bitSize != bs) {
      bitSize = bs;
      bitMask = (1 << bs) - 1;
      byteBufI = bs;
    }
  }

  public void close() {
  }

  public int readBitField() throws IOException {
    int readBits = bitSize;
    int r = 0;

    while (bitsAvail < bitSize) {
      if (byteBufI >= bitSize) {
        r = is.read(byteBuf, 0, bitSize);
        if (r < 0) {
          return -1;
        }
        bitsAvail = 0;
        byteBufI = 0;
        byteBufLen = r;
      }
      if (r == -1 || byteBufI >= byteBufLen) {
        return -1;
      }
      r = (int)byteBuf[byteBufI++] & 0xff;
      buf |= (r << bitsAvail);
      bitsAvail += EIGHT;
      buf &= (((1 << (bitsAvail)) - 1));
    }

    r = (int)(buf & bitMask);
    buf = buf >> readBits;
    bitsAvail -= readBits;
    buf &= ((1 << bitsAvail) - 1);

    return r;
  }

  public int readPackDirBitField() throws IOException {
    int readBits = bitSize;
    int r = 0;

    while (bitsAvail < bitSize) {
      r = is.read();
      if (r == -1) {
        return -1;
      }
      buf |= (r << bitsAvail);
      bitsAvail += EIGHT;
      buf &= (((1 << (bitsAvail)) - 1));
    }

    r = (int)(buf & bitMask);
    buf = buf >> readBits;
    bitsAvail -= readBits;
    buf &= ((1 << bitsAvail) - 1);

    return r;
  }

}
