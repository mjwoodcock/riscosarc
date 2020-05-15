// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;


/* Code taken from David Pilling's FileShrinker code from
 * https://www.davidpilling.com/wiki/index.php/FileShrinker */

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class CFSInputStream extends FilterInputStream {

  private static final int ENDBLOCK = 0x0;
  private static final int COMPRESSEDBLOCK = 0x1;
  private static final int RAWBLOCK = 0x2;
  private static final int HEADERBLOCK = 0x3;
  private static final int ZEROBLOCK = 0x4;

  private static final int DEFAULT_BUFFER_SIZE = 1024;
  private static final int DEFAULT_BITS = 12;
  private static final int FIRST_FREE = 256;
  private static final int MAXTOKLEN = 0x4000;

  private InputStream is;
  private int bits;
  private int highcode;
  private int nextfree;
  private int maxbits;
  private int maxmaxcode;
  private int codelimit;
  private int blocktype;
  private int shift;

  private byte[] srcBuffer;
  private int srcBufferI;
  private int srcBufferIF;
  private int srcBufferEnd;

  private byte[] blockBuffer;
  private int blockBufferI;
  private int blockBufferEnd;
  private int blockBufferRead;

  private int[] token;
  private int[] pfx;

  private boolean gotHeader;

  protected boolean eos;

  private int getChar() throws IOException {
    if (this.srcBufferI == this.srcBufferEnd) {
      fillSrcBuffer();
    }

    if (this.srcBufferI == this.srcBufferEnd) {
      return -1;
    }

    int c = this.srcBuffer[this.srcBufferI++] & 0xff;

    return c;
  }

  private void fillSrcBuffer() throws IOException {
    if (this.srcBufferI > 0) {
      System.arraycopy(this.srcBuffer, this.srcBufferI,
                       this.srcBuffer, 0,
                       this.srcBufferEnd - this.srcBufferI);
      this.srcBufferEnd -= this.srcBufferI;
      this.srcBufferIF -= this.srcBufferI;
      this.srcBufferI = 0;
    }
    int r = is.read(this.srcBuffer, this.srcBufferEnd,
        this.srcBuffer.length - this.srcBufferEnd);
    if (r != -1) {
      this.srcBufferEnd += r;
    }

  }

  private void zeroSrcBuffer() throws IOException {
    if (this.srcBufferI > 0) {
      System.arraycopy(this.srcBuffer, this.srcBufferI,
                       this.srcBuffer, 0,
                       this.srcBufferEnd - this.srcBufferI);
      this.srcBufferEnd -= this.srcBufferI;
      this.srcBufferIF -= this.srcBufferI;
      this.srcBufferI = 0;
    }
    Arrays.fill(this.srcBuffer, this.srcBufferI,
                this.srcBuffer.length - this.srcBufferEnd, (byte)0);
    this.srcBufferEnd = srcBuffer.length;
  }

  private void addCharToBlockBuffer(int chr) {
    this.blockBuffer[this.blockBufferI++] = (byte)chr;
  }

  private void copyCharsToBlockBuffer(int[] buf, int offset, int len) {
    while (len-- > 0) {
      if (this.blockBufferI >= this.blockBuffer.length) {
        byte[] b = new byte[this.blockBuffer.length * 2];
        System.arraycopy(this.blockBuffer, 0, b, 0, this.blockBuffer.length);
        this.blockBuffer = b;
        this.blockBufferEnd = this.blockBuffer.length;
      }
      this.blockBuffer[this.blockBufferI++] = (byte)buf[offset++];
    }
  }

  private void copySrcToBlockBuffer(int len) {
    if (len > this.srcBufferEnd - this.srcBufferI) {
      len = this.srcBufferEnd - this.srcBufferI;
    }
    System.arraycopy(this.srcBuffer, this.srcBufferI, this.blockBuffer, this.blockBufferI, len);
    this.blockBufferI += len;
    this.blockBufferEnd += len;
    this.srcBufferI += len;
    this.srcBufferIF += this.srcBufferIF;
  }

  private int nextcode() throws IOException {
    int i = this.srcBufferI;
    int diff = this.shift + this.bits;
    int code = 0;

    if (((this.srcBufferEnd - i) << 3) < diff) {
      fillSrcBuffer();
      i = this.srcBufferI;
      if (((this.srcBufferEnd - i) << 3) < diff) {
        return -1;
      }
    }

    diff -= 16;
    if (diff > 0) {
      code = getChar();
      code |= getChar() << 8;
      code |= this.srcBuffer[this.srcBufferI] << 16;
      code >>= shift;
      shift = diff;
    } else {
      code = getChar();
      code |= (int)(this.srcBuffer[this.srcBufferI] << 8);
      code &= 0xffff;
      code >>= this.shift;
      this.shift = 8 + diff;
      if (this.shift == 0) {
        this.srcBufferI++;
      }
    }

    return (code & this.highcode);
  }

  private void readHeader() throws IOException {
    if (!this.gotHeader) {
      this.gotHeader = true;

      this.maxbits = 12;

      this.maxmaxcode = (1 << maxbits) - 1;
      this.pfx = new int[this.maxmaxcode + 1];
      this.bits = 9;
    }
  }

  private void setBitSize(int bits, boolean start) throws IOException {
    if (start) {
      if (this.srcBufferI > this.srcBufferIF) {
        if (this.shift > 0) {
          this.srcBufferI++;
        }

        this.shift = this.srcBufferI - this.srcBufferIF;
        this.shift = (shift + 0x3) & (~0x3);
        this.srcBufferI = this.srcBufferIF + shift;
      }

      this.shift = 0;
      this.blocktype = getChar();
      if (this.blocktype == -1) {
        this.blocktype = ENDBLOCK;
        return;
      }
      this.codelimit = getChar();
      this.codelimit |= getChar() << 8;
      this.codelimit |= getChar() << 16;
      if (this.blocktype == COMPRESSEDBLOCK) {
        this.codelimit += 0xff;
      }
      this.blockBuffer = new byte[this.codelimit];
      this.blockBufferI = 0;
      this.blockBufferEnd = this.codelimit;
      this.srcBufferIF = this.srcBufferI;
    } else {
      this.shift += 1;
    }
    this.bits = bits;
  }

  private int readCFSBlock() throws IOException {
    this.highcode = (1 << 9) - 1;
    this.bits = 9;
    setBitSize(this.bits, true);
    this.nextfree = FIRST_FREE;

    int p;
    int q;

    p = q = MAXTOKLEN - 1;

    if (this.blocktype == COMPRESSEDBLOCK) {
      int code;
      int savecode;
      int prefixcode = nextcode();

      if (prefixcode < 0) {
        return -1;
      }

      int sufxchar = prefixcode;
      addCharToBlockBuffer(sufxchar);

      while (this.nextfree < this.codelimit
             && (code = savecode = nextcode()) >= 0) {
        p = q;

        if (code >= this.nextfree) {
          if (code != this.nextfree) {
            //throw new InvalidCFSFile();
            return -1;
          }

          code = prefixcode;
          token[p--] = sufxchar;
        }
        while (code >= 256) {
          code = this.pfx[code];
          token[p--] = code;
          code >>= 8;
        }

        this.token[p--] = sufxchar = code;
        copyCharsToBlockBuffer(token, p + 1, q - p);

        // if (!(256 <= nextfree && nextfree <= maxcode)) throw strop
        this.pfx[this.nextfree] = (prefixcode << 8) | sufxchar;
        prefixcode = savecode;
        if (this.nextfree++ == this.highcode) {
          this.bits++;
          setBitSize(this.bits, false);
          this.highcode += this.nextfree;
        }
      }
    } else if (this.blocktype == RAWBLOCK) {
      if (this.srcBufferEnd - this.srcBufferI < this.codelimit) {
        fillSrcBuffer();
      }
      copySrcToBlockBuffer(this.codelimit);
    } else if (this.blocktype == ZEROBLOCK) {
      if (this.srcBufferEnd - this.srcBufferI < this.codelimit) {
        zeroSrcBuffer();
      }
      copySrcToBlockBuffer(this.codelimit);
    } else if (this.blocktype == ENDBLOCK) {
      return -1;
    } else {
      return -1;
      //throw new IOException();
    }

    return 0;
  }

  public CFSInputStream(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE, DEFAULT_BITS);
  }

  public CFSInputStream(InputStream in, int siz, int bit) {
    super(in);
    this.maxbits = bit;
    this.eos = false;
    this.is = in;
    this.gotHeader = false;
    this.srcBuffer = new byte[0x4000];
    this.srcBufferI = 0;
    this.token = new int[MAXTOKLEN];
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
    int l = len;
    int o = off;

    if (eos) {
      return -1;
    }

    readHeader();

    int bytesToCopy = -1;
    int bytesCopied = 0;
    boolean didPartial = false;
    while (l > 0) {
      if (l > this.blockBufferI - this.blockBufferRead) {
        bytesToCopy = this.blockBufferI - this.blockBufferRead;
        if (this.blockBufferI > this.blockBufferRead) {
          System.arraycopy(this.blockBuffer, this.blockBufferRead,
                           buf, o, bytesToCopy);
        }
        bytesCopied += bytesToCopy;
        o += bytesToCopy;
        l -= bytesToCopy;
        this.blockBufferI = this.blockBufferRead = 0;
        didPartial = true;
        r = readCFSBlock();
        if (r == -1) {
          this.eos = true;
          return bytesCopied;
        }
      }

      if (this.blockBufferI - this.blockBufferRead == 0) {
        break;
      }
      bytesToCopy = l > (this.blockBufferI - this.blockBufferRead)
                        ? (this.blockBufferI - this.blockBufferRead)
                        : l;

      System.arraycopy(this.blockBuffer, this.blockBufferRead,
            buf, o,
            bytesToCopy);

      this.blockBufferRead += bytesToCopy;
      l -= bytesToCopy;
      o += bytesToCopy;
      bytesCopied += bytesToCopy;
    }

    return bytesCopied;
  }
}
