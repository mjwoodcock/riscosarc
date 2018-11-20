package riscos.archive;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

public class LimitOutputStream extends FilterOutputStream
{
	private int total_size;
	private OutputStream os;

	public LimitOutputStream(OutputStream out, int size)
	{
		super(out);
		total_size = size;
		os = out;
	}

	public void close() throws IOException
	{
		flush();
	}

	public void write(int b) throws IOException
	{
		if (total_size > 0)
		{
			os.write(b);
			total_size--;
		}
	}

	public void write(byte buf[]) throws IOException
	{
		write(buf, 0, buf.length);
	}

	public void write(byte buf[], int off, int len) throws IOException
	{
		int bytes_to_write;

		if (total_size > 0)
		{
			bytes_to_write = total_size > len ? len : total_size;
			os.write(buf, off, bytes_to_write);
			total_size -= bytes_to_write;
		}
	}

	public void flush() throws IOException
	{
	}
}
