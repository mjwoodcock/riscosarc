// vim:ts=2:sw=2:expandtab:ai

/* This code is a Java translation of the ncompress42.c code
 * The oricinal C code Authors:
 *   Spencer W. Thomas   (decvax!harpo!utah-cs!utah-gr!thomas)
 *   Jim McKie           (decvax!mcvax!jim)
 *   Steve Davies        (decvax!vax135!petsd!peora!srd)
 *   Ken Turkowski       (decvax!decwrl!turtlevax!ken)
 *   James A. Woods      (decvax!ihnp4!ames!jaw)
 *   Joe Orost           (decvax!vax135!petsd!joe)
 *   Dave Mack           (csu@alembic.acs.com)
 *   Peter Jannesen, Network Communication Systems
 *                       (peter@ncs.nl)
 */

package riscos.archive;

import riscos.archive.BitInputStream;
import riscos.archive.LZWConstants;

import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Hashtable;

public class NcompressLZWInputStream extends FilterInputStream {

  private class HashEntry {
    public byte theChar;
    public int code;
  }

  private static final int DEFAULT_BUFFER_SIZE = 1024;
  private static final int DEFAULT_TYPE = LZWConstants.UNIX_COMPRESS;
  private static final int DEFAULT_BITS = 0;
  private static final int MAGIC_1 = 0x1f;
  private static final int MAGIC_2 = 0x9d;
  private int oldCode;
  private BitInputStream is;
  private Hashtable<Integer, HashEntry> hash;
  private int freeEnt;
  private int finchar;
  private int bits;
  private int maxbits;
  private int maxmaxcode;
  private int maxcode;
  private boolean blockMode;

  private int type;

  private byte[] lzwBuf;
  private int lzwBufI;

  private boolean gotHeader;

  protected boolean eos;

  private void readHeader() throws IOException, CorruptLZWData {
    if (!this.gotHeader) {
      int r;

      this.gotHeader = true;

      if (this.type == LZWConstants.UNIX_COMPRESS) {
        r = this.is.read();
        if (r != this.MAGIC_1) {
          throw new CorruptLZWData("Bad Unix compress magic");
        }
        r = this.is.read();
        if (r != this.MAGIC_2) {
          throw new CorruptLZWData("Bad Unix compress magic");
        }
        r = this.is.read();
        this.maxbits = r & 0x1f;
        this.blockMode = (r & 0x80) == 0x80;
      } else if (this.type == LZWConstants.SQUASH) {
        this.maxbits = LZWConstants.SQUASH_BITS;
        this.blockMode = true;
      } else {
        if (this.maxbits == 0) {
          this.maxbits = this.is.read();
        }
        this.blockMode = true;
      }

      this.maxmaxcode = (1 << this.maxbits);
      this.lzwBuf = new byte[this.maxmaxcode];
      this.lzwBufI = 0;
      bits = 9;
      this.maxcode = (1 << this.bits) - 1;
      this.is.setBitSize(bits);
      this.oldCode = -1;
      this.finchar = -1;
    }
  }

  private void clearTabPrefixOf() {
    for (int code = 0; code < 256; code++) {
      HashEntry ent = this.hash.get(code);
      ent.code = code;
      this.hash.put(code, ent);
    }
  }

  private void addByteToBuf(byte val) {
    if (this.lzwBufI != lzwBuf.length) {
      this.lzwBuf[this.lzwBufI++] = val;
    }
  }

  private int copyBuffer(byte[] buf, int off, int len) {
    int bytesCopied;

    for (int i = 0; i < len && i < this.lzwBufI; i++) {
      buf[off + i] = this.lzwBuf[i];
    }

    bytesCopied = len < this.lzwBufI ? len : this.lzwBufI;

    if (len < this.lzwBufI) {
      int i;
      for (i = len; i < this.lzwBufI; i++) {
        this.lzwBuf[i - len] = this.lzwBuf[i];
      }
      this.lzwBufI = i - len;
    } else {
      this.lzwBufI = 0;
    }

    return bytesCopied;
  }

  private int readLZW(int bytes) throws IOException, CorruptLZWData {
    int r;
    int code = -1;
    int incode;
    byte[] stack = new byte[65535];
    int stacki = 65535;

    if (this.eos) {
      return -1;
    }

    readHeader();

    do {
      if (this.freeEnt > this.maxcode) {
        this.bits++;
        if (this.bits == this.maxbits) {
          this.maxcode = this.maxmaxcode;
        } else  {
          this.maxcode = (1 << this.bits) - 1;
        }
        this.is.setBitSize(bits);
        continue;
      }

      code = this.is.readBitField();
      if (code == -1) {
        break;
      }

      if (this.oldCode == -1) {
        this.finchar = this.oldCode = code;
        addByteToBuf((byte)oldCode);
        continue;
      }

      if (code == LZWConstants.CLEAR && this.blockMode) {
        clearTabPrefixOf();
        this.bits = 9;
        this.maxcode = (1 << 9) - 1;
        this.is.setBitSize(bits);
        this.freeEnt = LZWConstants.FIRST - 1;
        continue;

      }

      incode = code;
      stacki = stack.length - 1;

      if (code >= this.freeEnt) {
        if (code > this.freeEnt) {
          throw new CorruptLZWData("KwKwK");
        }

        stack[--stacki] = (byte)this.finchar;
        code = this.oldCode;
      }

      while (code >= 256) {
        stack[--stacki] = hash.get(code).theChar;
        code = hash.get(code).code;
      }

      this.finchar = hash.get(code).theChar;
      stack[--stacki] = (byte)this.finchar;

      while (stacki < stack.length - 1) {
        addByteToBuf(stack[stacki++]);
      }

      if ((code = this.freeEnt) < this.maxmaxcode) {
        HashEntry ent = new HashEntry();
        ent.theChar = (byte)this.finchar;
        ent.code = oldCode;
        this.hash.put(this.freeEnt, ent);
        this.freeEnt++;
      }

      this.oldCode = incode;
    } while (this.lzwBufI < bytes);

    if (code == -1) {
      return -1;
    } else {
      return 0;
    }
  }

  public NcompressLZWInputStream(InputStream in) throws UnsupportedLZWType {
    this(in, DEFAULT_BUFFER_SIZE, DEFAULT_TYPE, DEFAULT_BITS);
  }

  public NcompressLZWInputStream(InputStream in, int size) throws UnsupportedLZWType {
    this(in, size, DEFAULT_TYPE, DEFAULT_BITS);
  }

  public NcompressLZWInputStream(InputStream in, int size, int type) throws UnsupportedLZWType {
    this(in, size, type, DEFAULT_BITS);
  }

  public NcompressLZWInputStream(InputStream in, int siz, int typ, int bit) throws UnsupportedLZWType {
    super(in);

    if (typ == LZWConstants.ZOO || typ == LZWConstants.PACKDIR) {
      throw new UnsupportedLZWType();
    }
    this.maxbits = bit;
    this.type = typ;
    HashEntry ent;
    this.eos = false;
    is = new BitInputStream(in);
    hash = new Hashtable<Integer, HashEntry>();
    for (int i = 0; i < 256; i++) {
      ent = new HashEntry();
      ent.code = 0;
      ent.theChar = (byte)i;
      hash.put(i, ent);
    }
    this.gotHeader = false;
    this.freeEnt = LZWConstants.FIRST;
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

    if (this.eos) {
      return -1;
    }

    if (len > this.lzwBufI) {
      try {
        r = readLZW(len);
      } catch (CorruptLZWData e) {
        throw new IOException(e.toString());
      }
    }

    int bytesCopied = copyBuffer(buf, off, len);
    if (r == -1 && bytesCopied == 0) {
      return -1;
    } else {
      return bytesCopied;
    }
  }
}
