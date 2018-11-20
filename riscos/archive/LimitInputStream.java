package riscos.archive;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

public class LimitInputStream extends FilterInputStream
{
	private int total_size;
	private InputStream is;

	public LimitInputStream(InputStream in, int total_siz)
	{
		super(in);
		is = in;
		total_size = total_siz;
	}

	public void close()
	{
	}

	public int read() throws IOException
	{
		if (total_size == 0)
		{
			return -1;
		}

		int b = is.read();
		if (b != -1)
		{
			--total_size;
		}

		return b;
	}

	public int read(byte buf[]) throws IOException
	{
		return read(buf, 0, buf.length);
	}

	public int read(byte buf[], int off, int len) throws IOException
	{
		int size;
		
		if (total_size == 0)
		{
			return -1;
		}

		size = total_size > len ? len : total_size;
		int r = is.read(buf, off, size);
		if (r == -1)
		{
			return -1;
		}
		total_size -= r;

		return r;
	}
}
