package riscos.archive;

/* FIXME: Check crc32 */

import java.util.zip.CRC32;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

public class CFSInputStream extends FilterInputStream
{
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

	private byte src_buffer[];
	private int src_bufferi;
	private int src_bufferif;
	private int src_buffer_end;

	private byte block_buffer[];
	private int block_bufferi;
	private int block_buffer_end;
	private int block_buffer_read;

	private int token[];
	private int pfx[];

	private boolean got_header;

	protected CRC32 crc;
	protected boolean eos;

	private int get_char() throws IOException
	{
		if (this.src_bufferi == this.src_buffer_end) {
			fillSrcBuffer();
		}

		if (this.src_bufferi == this.src_buffer_end) {
			return -1;
		}

		int c = this.src_buffer[this.src_bufferi++] & 0xff;

		return c;
	}

	private void fillSrcBuffer() throws IOException
	{
		if (this.src_bufferi > 0) {
			System.arraycopy(this.src_buffer, this.src_bufferi,
					this.src_buffer, 0,
					this.src_buffer_end - this.src_bufferi);
			this.src_buffer_end -= this.src_bufferi;
			this.src_bufferif -= this.src_bufferi;
			this.src_bufferi = 0;
		}
		int r = is.read(this.src_buffer, this.src_buffer_end,
				this.src_buffer.length - this.src_buffer_end);
		if (r != -1) {
			this.src_buffer_end += r;
		}
		
	}

	private void addCharToBlockBuffer(int c)
	{
		this.block_buffer[this.block_bufferi++] = (byte)c;
	}

	private void copyCharsToBlockBuffer(int buf[], int offset, int len)
	{
		while (len-- > 0) {
			if (this.block_bufferi >= this.block_buffer.length) {
				byte b[] = new byte[this.block_buffer.length * 2];
				System.arraycopy(this.block_buffer, 0, b, 0, this.block_buffer.length);
				this.block_buffer = b;
				this.block_buffer_end = this.block_buffer.length;
			}
			this.block_buffer[this.block_bufferi++] = (byte)buf[offset++];
		}
	}

	private int nextcode() throws IOException
	{
		int i = this.src_bufferi;
		int diff = this.shift + this.bits;
		int code = 0;

		if (((this.src_buffer_end - i) << 3) < diff) {
			fillSrcBuffer();
			i = this.src_bufferi;
			if (((this.src_buffer_end - i) << 3) < diff) {
				return -1;
			}
		}

		diff -= 16;
		if (diff > 0) {
			code = get_char();
			code |= get_char() << 8;
			code |= this.src_buffer[this.src_bufferi] << 16;
			code >>= shift;
			shift = diff;
		} else {
			code = get_char();
			code |= (int)(this.src_buffer[this.src_bufferi] << 8);
			code &= 0xffff;
			code >>= this.shift;
			this.shift = 8 + diff;
			if (this.shift == 0) {
				this.src_bufferi++;
			}
		}

		return (code & this.highcode);
	}

	private void readHeader() throws IOException
	{
		if (!this.got_header)
		{
			this.got_header = true;

			this.maxbits = 12;

			this.maxmaxcode = (1 << maxbits) - 1;
			this.pfx = new int[this.maxmaxcode + 1];
			this.bits = 9;
		}
	}

	private void setBitSize(int bits, boolean start) throws IOException
	{
		if (start) {
			if (this.src_bufferi > this.src_bufferif) {
				if (this.shift > 0) {
					this.src_bufferi++;
				}

				this.shift = this.src_bufferi - this.src_bufferif;
				this.shift = (shift + 0x3) & (~0x3);
				this.src_bufferi = this.src_bufferif + shift;
			}

			this.shift = 0;
			this.blocktype = get_char();
			if (this.blocktype == -1) {
				this.blocktype = ENDBLOCK;
				return;
			}
			this.codelimit = get_char();
			this.codelimit |= get_char() << 8;
			this.codelimit |= get_char() << 16;
			if (this.blocktype == COMPRESSEDBLOCK) {
				this.codelimit += 0xff;
			}
			this.block_buffer = new byte[this.codelimit];
			this.block_bufferi = 0;
			this.block_buffer_end = this.codelimit;
			this.src_bufferif = this.src_bufferi;
		} else {
			this.shift += 1;
		}
		this.bits = bits;
	}

	private int readCFSBlock() throws IOException
	{
		this.highcode = (1 << 9) - 1;
		this.bits = 9;
		setBitSize(this.bits, true);
		this.nextfree = FIRST_FREE;

		int p, q;

		p = q = MAXTOKLEN - 1;

		if (this.blocktype == COMPRESSEDBLOCK) {
			int code, savecode;
			int prefixcode = nextcode();
			if (prefixcode < 0) {
				return -1;
			}

			int sufxchar = prefixcode;
			addCharToBlockBuffer(sufxchar);

			while (this.nextfree < this.codelimit &&
				(code = savecode = nextcode()) >= 0) {
				p = q;

				if (code >= this.nextfree) {
					if (code != this.nextfree) {
//						throw new InvalidCFSFile();
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
			System.out.println("RAWBLOCK not handled");
		} else if (this.blocktype == ZEROBLOCK) {
			System.out.println("ZEROBLOCK not handled");
		} else if (this.blocktype == ENDBLOCK) {
			return -1;
		}
		return 0;
	}

	public CFSInputStream(InputStream in)
	{
		this(in, DEFAULT_BUFFER_SIZE, DEFAULT_BITS);
	}

	public CFSInputStream(InputStream in, int siz, int bit)
	{
		super(in);
		this.maxbits = bit;
		this.eos = false;
		this.is = in;
		this.got_header = false;
		this.src_buffer = new byte[0x4000];
		this.src_bufferi = 0;
		this.token = new int[MAXTOKLEN];
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
		if (r == -1)
		{
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
		int l = len;
		int o = off;
		
		if (eos) {
			return -1;
		}

		readHeader();

		int bytes_to_copy = -1;
		int bytes_copied = 0;
		boolean did_partial = false;
		while (l > 0) {
			if (l > this.block_bufferi - this.block_buffer_read) {
				bytes_to_copy = this.block_bufferi - this.block_buffer_read;
				if (this.block_bufferi > this.block_buffer_read) {
					System.arraycopy(this.block_buffer, this.block_buffer_read,
						buf, o,
						bytes_to_copy);
				}
				bytes_copied += bytes_to_copy;
				o += bytes_to_copy;
				l -= bytes_to_copy;
				this.block_bufferi = this.block_buffer_read = 0;
				did_partial = true;
				r = readCFSBlock();
				if (r == -1) {
					this.eos = true;
					return bytes_copied;
				}
			}

			if (this.block_bufferi - this.block_buffer_read == 0) {
				break;
			}
			bytes_to_copy = l > (this.block_bufferi - this.block_buffer_read) ?
					(this.block_bufferi - this.block_buffer_read)
					: l;

			System.arraycopy(this.block_buffer, this.block_buffer_read,
						buf, o,
						bytes_to_copy);

			this.block_buffer_read += bytes_to_copy;
			l -= bytes_to_copy;
			o += bytes_to_copy;
			bytes_copied += bytes_to_copy;
		}

		return bytes_copied;
	}
}
