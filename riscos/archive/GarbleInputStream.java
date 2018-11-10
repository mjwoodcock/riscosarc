package riscos.archive;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

public class GarbleInputStream extends FilterInputStream
{
	private byte passwd[];
	private int passwdi = 0;
	private InputStream is;

	public GarbleInputStream(InputStream in)
	{
		this(in, null);
	}

	public GarbleInputStream(InputStream in, byte pass[])
	{
		super(in);
		passwd = pass;
		passwdi = 0;
		is = in;
	}

	public void close()
	{
	}

	public int read() throws IOException
	{
		int r = is.read();

		if (passwd != null && r != -1)
		{
			r ^= passwd[passwdi++];
			if (passwdi == passwd.length)
			{
				passwdi = 0;
			}
		}

		return r;
	}

	public int read(byte buf[]) throws IOException
	{
		return read(buf, 0, buf.length);
	}

	public int read(byte buf[], int off, int len) throws IOException
	{
		int r;

		r = is.read(buf, off, len);
		if (passwd != null)
		{
			for (int i = off; i < off + len; i++)
			{
				buf[off + i] ^= passwd[passwdi++];
				if (passwdi == passwd.length)
				{
					passwdi = 0;
				}
			}
		}

		return r;
	}

	public void consumePasswdChar()
	{
		if (passwd != null)
		{
			passwdi++;
			if (passwdi == passwd.length)
			{
				passwdi = 0;
			}
		}
	}
}
