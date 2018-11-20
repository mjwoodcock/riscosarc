package riscos.archive;

import java.io.FilterInputStream;
import java.io.PushbackInputStream;
import java.io.InputStream;
import java.io.IOException;

public class PackInputStream extends FilterInputStream
{
	private byte ncr_buf[];
	private int ncr_bufi;
	private int ncr_buflen;
	private int ncr_bufsize;
	private PushbackInputStream is;
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final byte RUNMARK = (byte)0x90;

	private int read_ncr_buf()
	{
		if (ncr_bufi == -1 || ncr_bufi >= ncr_buflen)
		{
			int r;
			try
			{
				r = is.read(ncr_buf, 0, ncr_bufsize);
			}
			catch (IOException e)
			{
				r = -1;
			}

			ncr_buflen = r;
			if (r == -1)
			{
				return r;
			}
			else if (r == ncr_bufsize)
			{
				try
				{
					if (ncr_buf[ncr_buflen - 1] == RUNMARK)
					{
						ncr_buf[ncr_buflen++] = (byte)is.read();
					}
					else
					{
						int b = is.read();
						if ((byte)b == RUNMARK)
						{
							ncr_buf[ncr_buflen++] = RUNMARK;
							b = is.read();
							ncr_buf[ncr_buflen++] = (byte)b;
						}
						else
						{
							is.unread(b);
						}
					}
				}
				catch (IOException e)
				{
					// If we can't read any more, then we must be at the
					// end of the stream, therefore don't worry about the
					// exception
				}
			}
			ncr_bufi = 0;
		}

		return ncr_buflen;
	}

	protected boolean eos;

	public PackInputStream(InputStream in)
	{
		this(in, DEFAULT_BUFFER_SIZE);
	}

	public PackInputStream(InputStream in, int size)
	{
		super(in);
		ncr_bufsize = size;
		ncr_buf = new byte[size + 2]; // We may need to add 0x90 and count bytes
		ncr_bufi = -1;
		is = new PushbackInputStream(in);
	}

	public void close()
	{
	}

	public int read()
	{
		byte b[] = new byte[1];

		int r = read(b, 0, 1);
		if (r == -1)
		{
			return r;
		}

		return (int)b[0];
	}

	public int read(byte buf[], int off, int len)
	{
		int l = 0;

		while (l < len)
		{
			int r = read_ncr_buf();
			if (r == -1)
			{
				if (l == 0)
				{
					return -1;
				}
				else
				{
					return l;
				}
			}

			if (ncr_buf[ncr_bufi] == RUNMARK)
			{
				if (ncr_buf[ncr_bufi + 1] == 0)
				{
					buf[off + l] = RUNMARK;
					ncr_bufi += 2;
					l++;
				}
				else
				{
					while (l < len && ((int)(--ncr_buf[ncr_bufi + 1]) & 0xFF) > 0)
					{
						buf[off + l] = ncr_buf[ncr_bufi - 1];
						l++;
						if (ncr_buf[ncr_bufi + 1] == 1)
						{
							ncr_bufi += 2;
							break;
						}
					}
				}
			}
			else
			{
				buf[off + l] = ncr_buf[ncr_bufi];
				l++;
				ncr_bufi++;
			}

		}

		return l;
	}
}
