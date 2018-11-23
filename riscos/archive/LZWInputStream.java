// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import riscos.archive.BitInputStream;
import riscos.archive.LZWConstants;

import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Hashtable;

public class LZWInputStream extends FilterInputStream {
  private class HashEntry {
    public byte theChar;
    public int code;
  }

  private static final int DEFAULT_BUFFER_SIZE = 1024;
  private static final int DEFAULT_TYPE = LZWConstants.UNIX_COMPRESS;
  private static final int DEFAULT_BITS = 0;
  private int oldCode;
  private int inchar;
  private BitInputStream is;
  private Hashtable<Integer, HashEntry> hash;
  private int freeEnt;
  private int bits;
  private int maxbits;
  private int maxmaxcode;
  private int stopCode;

  private int type;

  private byte[] lzwBuf;
  private int lzwBufI;

  private boolean gotHeader;

  protected boolean eos;

  private void readHeader() throws IOException {
    if (!gotHeader) {
      gotHeader = true;

      if (type == LZWConstants.UNIX_COMPRESS) {
        int r = is.read();
        r = is.read();
        r = is.read();
        maxbits = r & 0x1f;
      } else if (type == LZWConstants.SQUASH) {
        maxbits = LZWConstants.SQUASH_BITS;
      } else if (type == LZWConstants.ZOO) {
        maxbits = LZWConstants.SQUASH_BITS;
      } else if (type == LZWConstants.PACKDIR) {
        if (maxbits == 0) {
          //throw new UnsupportedLZWBitSize();
        }
      } else {
        if (maxbits == 0) {
          maxbits = is.read();
        }
      }

      maxmaxcode = (1 << maxbits) - 1;
      lzwBuf = new byte[maxmaxcode];
      lzwBufI = 0;
      bits = 9;
      is.setBitSize(bits);
      if (type == LZWConstants.ZOO || type == LZWConstants.PACKDIR) {
        stopCode = LZWConstants.Z_EOF;
        freeEnt = LZWConstants.FIRST + 1;
        oldCode = 0;
      } else {
        inchar = oldCode = is.readBitField();
        addByteToBuf((byte)oldCode);
      }
    }
  }

  private void addByteToBuf(byte byt) throws IOException {
    if (lzwBufI != maxmaxcode) {
      lzwBuf[lzwBufI++] = byt;
    }
  }

  private int copyBuffer(byte[] buf, int off, int len) {
    int bytesCopied;

    for (int i = 0; i < len && i < lzwBufI; i++) {
      buf[off + i] = lzwBuf[i];
    }

    bytesCopied = len < lzwBufI ? len : lzwBufI;

    if (len < lzwBufI) {
      int i;
      for (i = len; i < lzwBufI; i++) {
        lzwBuf[i - len] = lzwBuf[i];
      }
      lzwBufI = i - len;
    } else {
      lzwBufI = 0;
    }

    return bytesCopied;
  }

  private int readLZW(int bytes) throws IOException {
    int r;
    int code;
    int incode;
    byte[] stack = new byte[65535];
    int stacki = 0;

    if (eos) {
      return -1;
    }

    readHeader();

    do {
      if (type == LZWConstants.ZOO || type == LZWConstants.PACKDIR) {
        code = is.readPackDirBitField();
      } else {
        code = is.readBitField();
      }

      if (code == -1) {
        break;
      }

      if ((type == LZWConstants.ZOO || type == LZWConstants.PACKDIR) && code == stopCode) {
        eos = true;
        break;
      }

      if (code == LZWConstants.CLEAR) {
        for (code = 0; code < LZWConstants.CLEAR; ++code) {
          hash.get(code).code = 0;
        }

        bits = 9;
        is.setBitSize(bits);
        if (type == LZWConstants.ZOO || type == LZWConstants.PACKDIR) {
          freeEnt = LZWConstants.FIRST + 1;
          inchar = oldCode = code = is.readPackDirBitField();
          addByteToBuf((byte)inchar);
          continue;
        } else {
          freeEnt = LZWConstants.FIRST - 1;
          code = is.readBitField();
        }

        if (code == -1) {
          break;
        }
      }

      incode = code;

      if (code >= freeEnt) {
        stack[stacki++] = (byte)inchar;
        code = oldCode;
      }

      while (code >= LZWConstants.CLEAR) {
        stack[stacki++] = hash.get(code).theChar;
        code = hash.get(code).code;
      }

      inchar = hash.get(code).theChar;
      stack[stacki++] = (byte)inchar;

      while (stacki > 0) {
        stacki--;
        addByteToBuf(stack[stacki]);
      }

      if (freeEnt < maxmaxcode) {
        HashEntry ent = new HashEntry();
        ent.theChar = (byte)inchar;
        ent.code = oldCode;
        hash.put(freeEnt, ent);
        freeEnt++;
        if (freeEnt > ((1 << bits) - 1)) {
          is.setBitSize(++bits);
        }
      }
      oldCode = incode;
    } while (lzwBufI < bytes);

    if (code == -1) {
      return -1;
    } else {
      return 0;
    }
  }

  public LZWInputStream(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE, DEFAULT_TYPE, DEFAULT_BITS);
  }

  public LZWInputStream(InputStream in, int size) {
    this(in, size, DEFAULT_TYPE, DEFAULT_BITS);
  }

  public LZWInputStream(InputStream in, int size, int type) {
    this(in, size, type, DEFAULT_BITS);
  }

  public LZWInputStream(InputStream in, int siz, int typ, int bit) {
    super(in);
    maxbits = bit;
    type = typ;
    // size = siz;
    eos = false;
    is = new BitInputStream(in);
    hash = new Hashtable<Integer, HashEntry>();
    for (int i = 0; i < 256; i++) {
      HashEntry ent = new HashEntry();
      ent.code = 0;
      ent.theChar = (byte)i;
      hash.put(i, ent);
    }
    gotHeader = false;
    freeEnt = LZWConstants.FIRST;
  }

  public void close() throws IOException {
  }

  public int read() throws IOException {
    byte[] buf = new byte[1];

    // FIXME: Do this without an expensive memory copy in
    // copyBuffer
    int r = read(buf);
    if (r == -1) {
      return -1;
    }

    return buf[0];
  }

  public int read(byte[] buf) throws IOException {
    return read(buf, 0, buf.length);
  }

  public int read(byte[] buf, int off, int len) throws IOException {
    int r = -1;

    if (eos) {
      return -1;
    }

    if (len > lzwBufI) {
      r = readLZW(len);
    }

    int bytesCopied = copyBuffer(buf, off, len);
    if (r == -1 && bytesCopied == 0) {
      return -1;
    } else {
      return bytesCopied;
    }
  }
}
