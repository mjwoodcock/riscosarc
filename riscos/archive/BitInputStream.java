package riscos.archive;

import java.util.zip.CRC32;
import java.io.PushbackInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

public class BitInputStream extends FilterInputStream
{
	private static final int EIGHT = 8;
	private static final int DEFAULT_BIT_SIZE = EIGHT;
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private long buf;
	private int bit_size;
	private long bit_mask;
	private int bits_avail;
	private InputStream is;
	private byte byte_buf[];
	private int byte_bufi;
	private int byte_buf_len;

	public BitInputStream(InputStream in)
	{
		this(in, DEFAULT_BUFFER_SIZE, DEFAULT_BIT_SIZE);
	}

	public BitInputStream(InputStream in, int size)
	{
		this(in, size, DEFAULT_BIT_SIZE);
	}

	public BitInputStream(InputStream in, int size, int bs)
	{
		super(in);
		bit_size = bs;
		bit_mask = (1 << bs) - 1;
		bits_avail = 0;
		buf = 0;
		is = in;
		byte_buf = new byte[20];
		byte_bufi = 0;
		byte_buf_len = 0;
	}
	
	public void setBitSize(int bs)
	{
		if (bit_size != bs)
		{
			bit_size = bs;
			bit_mask = (1 << bs) - 1;
			byte_bufi = bs;
		}
	}

	public void close()
	{
	}

	public int readBitField() throws IOException
	{
		int read_bits = bit_size;
		int r = 0;

		while (bits_avail < bit_size)
		{
			if (byte_bufi >= bit_size)
			{
				r = is.read(byte_buf, 0, bit_size);
				if (r < 0)
				{
					return -1;
				}
				byte_bufi = 0;
				byte_buf_len = r;
			}
			if (r == -1 || byte_bufi >= byte_buf_len)
			{
				return -1;
			}
			r = (int)byte_buf[byte_bufi++] & 0xff;
			buf |= (r << bits_avail);
			bits_avail += EIGHT;
			buf &= (((1 << (bits_avail)) - 1));
		}

		r = (int)(buf & bit_mask);
		buf = buf >> read_bits;
		bits_avail -= read_bits;
		buf &= ((1 << bits_avail) - 1);

		return r;
	}

	public int readPackDirBitField() throws IOException
	{
		int read_bits = bit_size;
		int r = 0;

		while (bits_avail < bit_size)
		{
			r = is.read();
			if (r == -1)
			{
				return -1;
			}
			buf |= (r << bits_avail);
			bits_avail += EIGHT;
			buf &= (((1 << (bits_avail)) - 1));
		}

		r = (int)(buf & bit_mask);
		buf = buf >> read_bits;
		bits_avail -= read_bits;
		buf &= ((1 << bits_avail) - 1);

		return r;
	}

}
