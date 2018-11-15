package riscos.archive;

/* FIXME: Check crc32 */

import java.util.zip.CRC32;
import java.io.FilterInputStream;
import java.io.PushbackInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import riscos.archive.BitInputStream;
import riscos.archive.LZWConstants;

public class LZWInputStream extends FilterInputStream
{
	private class hashEntry
	{
		public byte the_char;
		public int code;
	};

	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final int DEFAULT_TYPE = LZWConstants.UNIX_COMPRESS;
	private static final int DEFAULT_BITS = 0;
	private int old_code;
	private int inchar;
	private BitInputStream is;
	private Hashtable<Integer, hashEntry> hash;
	private int free_ent;
	private int bits;
	private int maxbits;
	private int maxmaxcode;
	private int stop_code;

	private int type;

	private byte lzw_buf[];
	private int lzw_bufi;

	private boolean got_header;

	protected CRC32 crc;
	protected boolean eos;

	private void readHeader() throws IOException
	{
		if (!got_header)
		{
			int r;

			got_header = true;

			if (type == LZWConstants.UNIX_COMPRESS)
			{
				r = is.read();
				r = is.read();
				r = is.read();
				maxbits = r & 0x1f;
			}
			else if (type == LZWConstants.SQUASH)
			{
				maxbits = LZWConstants.SQUASH_BITS;
			}
			else if (type == LZWConstants.ZOO)
			{
				maxbits = LZWConstants.SQUASH_BITS;
			}
			else if (type == LZWConstants.PACKDIR)
			{
				if (maxbits == 0)
				{
//					throw new UnsupportedLZWBitSize();
				}
			}
			else
			{
				if (maxbits == 0)
				{
					maxbits = is.read();
				}
			}

			maxmaxcode = (1 << maxbits) - 1;
			lzw_buf = new byte[maxmaxcode];
			lzw_bufi = 0;
			bits = 9;
			is.setBitSize(bits);
			if (type == LZWConstants.ZOO || type == LZWConstants.PACKDIR)
			{
				stop_code = LZWConstants.Z_EOF;
				free_ent = LZWConstants.FIRST + 1;
				old_code = 0;
			}
			else
			{
				inchar = old_code = is.readBitField();
				addByteToBuf((byte)old_code);
			}
		}
	}

	private void addByteToBuf(byte b) throws IOException
	{
		if (lzw_bufi != maxmaxcode)
		{
			lzw_buf[lzw_bufi++] = b;
		}
	}

	private int copyBuffer(byte buf[], int off, int len)
	{
		int bytes_copied;

		for (int i = 0; i < len && i < lzw_bufi; i++)
		{
			buf[off + i] = lzw_buf[i];
		}

		bytes_copied = len < lzw_bufi ? len : lzw_bufi;

		if (len < lzw_bufi)
		{
			int i;
			for (i = len; i < lzw_bufi; i++)
			{
				lzw_buf[i - len] = lzw_buf[i];
			}
			lzw_bufi = i - len;
		}
		else
		{
			lzw_bufi = 0;
		}

		return bytes_copied;
	}

	private int readLZW(int bytes) throws IOException
	{
		int r;
		int code;
		int incode;
		byte stack[] = new byte[65535];
		int stacki = 0;

		if (eos)
		{
			return -1;
		}

		readHeader();

		do
		{
			if (type == LZWConstants.ZOO || type == LZWConstants.PACKDIR)
			{
				code = is.readPackDirBitField();
			}
			else
			{
				code = is.readBitField();
			}
			if (code == -1)
			{
				break;
			}
			if ((type == LZWConstants.ZOO || type == LZWConstants.PACKDIR) && code == stop_code)
			{
				eos = true;
				break;
			}

			if (code == LZWConstants.CLEAR)
			{
				for (code = 0; code < LZWConstants.CLEAR; ++code)
					hash.get(code).code = 0;
				bits = 9;
				is.setBitSize(bits);
				if (type == LZWConstants.ZOO || type == LZWConstants.PACKDIR)
				{
					free_ent = LZWConstants.FIRST + 1;
					inchar = old_code = code = is.readPackDirBitField();
					addByteToBuf((byte)inchar);
					continue;
				}
				else
				{
					free_ent = LZWConstants.FIRST - 1;
					code = is.readBitField();
				}
				if (code == -1)
				{
					break;
				}
			}

			incode = code;

			if (code >= free_ent)
			{
				stack[stacki++] = (byte)inchar;
				code = old_code;
			}

			while (code >= LZWConstants.CLEAR)
			{
				stack[stacki++] = hash.get(code).the_char;
				code = hash.get(code).code;
			}

			inchar = hash.get(code).the_char;
			stack[stacki++] = (byte)inchar;

			while (stacki > 0)
			{
				stacki--;
				addByteToBuf(stack[stacki]);
			}

			if (free_ent < maxmaxcode)
			{
				hashEntry ent = new hashEntry();
				ent.the_char = (byte)inchar;
				ent.code = old_code;
				hash.put(free_ent, ent);
				free_ent++;
				if (free_ent > ((1 << bits) - 1))
				{
					is.setBitSize(++bits);
				}
			}
			old_code = incode;
		} while(lzw_bufi < bytes);

		if (code == -1)
		{
			return -1;
		}
		else
		{
			return 0;
		}
	}

	public LZWInputStream(InputStream in)
	{
		this(in, DEFAULT_BUFFER_SIZE, DEFAULT_TYPE, DEFAULT_BITS);
	}

	public LZWInputStream(InputStream in, int size)
	{
		this(in, size, DEFAULT_TYPE, DEFAULT_BITS);
	}

	public LZWInputStream(InputStream in, int size, int type)
	{
		this(in, size, type, DEFAULT_BITS);
	}

	public LZWInputStream(InputStream in, int siz, int typ, int bit)
	{
		super(in);
		maxbits = bit;
		type = typ;
//		size = siz;
		hashEntry ent;
		eos = false;
		is = new BitInputStream(in);
		hash = new Hashtable<Integer, hashEntry>();
		for (int i = 0; i < 256; i++)
		{
			ent = new hashEntry();
			ent.code = 0;
			ent.the_char = (byte)i;
			hash.put(i, ent);
		}
		got_header = false;
		free_ent = LZWConstants.FIRST;
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
		
		if (eos)
		{
			return -1;
		}

		if (len > lzw_bufi)
		{
			r = readLZW(len);
		}

		int bytes_copied = copyBuffer(buf, off, len);
		if (r == -1 && bytes_copied == 0)
		{
			return -1;
		}
		else
		{
			return bytes_copied;
		}
	}
}
