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

import java.io.FilterInputStream;
import java.io.PushbackInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import riscos.archive.BitInputStream;
import riscos.archive.LZWConstants;

public class NcompressLZWInputStream extends FilterInputStream
{
	private class hashEntry
	{
		public byte the_char;
		public int code;
	};

	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final int DEFAULT_TYPE = LZWConstants.UNIX_COMPRESS;
	private static final int DEFAULT_BITS = 0;
	private static final int MAGIC_1 = 0x1f;
	private static final int MAGIC_2 = 0x9d;
	private int old_code;
	private BitInputStream is;
	private Hashtable<Integer, hashEntry> hash;
	private int free_ent;
	private int stop_code;
	private int finchar;
	private int bits;
	private int maxbits;
	private int maxmaxcode;
	private int maxcode;
	private boolean block_mode;

	private int type;

	private byte lzw_buf[];
	private int lzw_bufi;

	private boolean got_header;

	protected boolean eos;

	private void readHeader() throws IOException, CorruptLZWData
	{
		if (!this.got_header) {
			int r;

			this.got_header = true;

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
				this.block_mode = (r & 0x80) == 0x80;
			} else if (this.type == LZWConstants.SQUASH) {
				this.maxbits = LZWConstants.SQUASH_BITS;
				this.block_mode = true;
			} else {
				if (this.maxbits == 0) {
					this.maxbits = this.is.read();
				}
				this.block_mode = true;
			}

			this.maxmaxcode = (1 << this.maxbits);
			this.lzw_buf = new byte[this.maxmaxcode];
			this.lzw_bufi = 0;
			bits = 9;
			this.maxcode = (1 << this.bits) - 1;
			this.is.setBitSize(bits);
			this.old_code = -1;
			this.finchar = -1;
		}
	}

	private void clearTabPrefixOf()
	{
		for (int code = 0; code < 256; code++) {
			hashEntry ent = this.hash.get(code);
			ent.code = code;
			this.hash.put(code, ent);
		}
	}

	private void addByteToBuf(byte b)
	{
		if (this.lzw_bufi != lzw_buf.length) {
			this.lzw_buf[this.lzw_bufi++] = b;
		}
	}

	private int copyBuffer(byte buf[], int off, int len)
	{
		int bytes_copied;

		for (int i = 0; i < len && i < this.lzw_bufi; i++) {
			buf[off + i] = this.lzw_buf[i];
		}

		bytes_copied = len < this.lzw_bufi ? len : this.lzw_bufi;

		if (len < this.lzw_bufi) {
			int i;
			for (i = len; i < this.lzw_bufi; i++) {
				this.lzw_buf[i - len] = this.lzw_buf[i];
			}
			this.lzw_bufi = i - len;
		} else {
			this.lzw_bufi = 0;
		}

		return bytes_copied;
	}

	private int readLZW(int bytes) throws IOException, CorruptLZWData
	{
		int r;
		int code = -1;
		int incode;
		byte stack[] = new byte[65535];
		int stacki = 65535;

		if (this.eos) {
			return -1;
		}

		readHeader();

		do {
			if (this.free_ent > this.maxcode) {
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

			if (this.old_code == -1) {
				this.finchar = this.old_code = code;
				addByteToBuf((byte)old_code);
				continue;
			}

			if (code == LZWConstants.CLEAR && this.block_mode) {
				clearTabPrefixOf();
				this.bits = 9;
				this.maxcode = (1 << 9) - 1;
				this.is.setBitSize(bits);
				this.free_ent = LZWConstants.FIRST - 1;
				continue;

			}

			incode = code;
			stacki = stack.length - 1;

			if (code >= this.free_ent) {
				if (code > this.free_ent) {
					throw new CorruptLZWData("KwKwK");
				}

				stack[--stacki] = (byte)this.finchar;
				code = this.old_code;
			}

			while (code >= 256) {
				stack[--stacki] = hash.get(code).the_char;
				code = hash.get(code).code;
			}

			this.finchar = hash.get(code).the_char;
			stack[--stacki] = (byte)this.finchar;

			while (stacki < stack.length - 1) {
				addByteToBuf(stack[stacki++]);
			}

			if ((code = this.free_ent) < this.maxmaxcode) {
				hashEntry ent = new hashEntry();
				ent.the_char = (byte)this.finchar;
				ent.code = old_code;
				this.hash.put(this.free_ent, ent);
				this.free_ent++;
			}

			this.old_code = incode;
		} while(this.lzw_bufi < bytes);

		if (code == -1) {
			return -1;
		} else {
			return 0;
		}
	}

	public NcompressLZWInputStream(InputStream in) throws UnsupportedLZWType
	{
		this(in, DEFAULT_BUFFER_SIZE, DEFAULT_TYPE, DEFAULT_BITS);
	}

	public NcompressLZWInputStream(InputStream in, int size) throws UnsupportedLZWType
	{
		this(in, size, DEFAULT_TYPE, DEFAULT_BITS);
	}

	public NcompressLZWInputStream(InputStream in, int size, int type) throws UnsupportedLZWType
	{
		this(in, size, type, DEFAULT_BITS);
	}

	public NcompressLZWInputStream(InputStream in, int siz, int typ, int bit) throws UnsupportedLZWType
	{
		super(in);

		if (typ == LZWConstants.ZOO || typ == LZWConstants.PACKDIR) {
			throw new UnsupportedLZWType();
		}
		this.maxbits = bit;
		this.type = typ;
		hashEntry ent;
		this.eos = false;
		is = new BitInputStream(in);
		hash = new Hashtable<Integer, hashEntry>();
		for (int i = 0; i < 256; i++) {
			ent = new hashEntry();
			ent.code = 0;
			ent.the_char = (byte)i;
			hash.put(i, ent);
		}
		this.got_header = false;
		this.free_ent = LZWConstants.FIRST;
	}

	public void close() throws IOException
	{
	}

	public int read() throws IOException
	{
		byte buf[] = new byte[1];

		// FIXME: Do this without an expensive memory copy in 
		// copyBuffer
		int r = read(buf);
		if (r == -1) {
			return -1;
		}
		
		return buf[0];
	}

	public int read(byte buf[]) throws IOException
	{
		return read(buf, 0, buf.length);
	}

	public int read(byte buf[], int off, int len) throws IOException
	{
		int r = -1;
		
		if (this.eos) {
			return -1;
		}

		if (len > this.lzw_bufi) {
			try {
				r = readLZW(len);
			} catch (CorruptLZWData e) {
				throw new IOException(e.toString());
			}
		}

		int bytes_copied = copyBuffer(buf, off, len);
		if (r == -1 && bytes_copied == 0) {
			return -1;
		} else {
			return bytes_copied;
		}
	}
}
