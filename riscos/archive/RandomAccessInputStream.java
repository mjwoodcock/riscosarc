package riscos.archive;

import java.io.RandomAccessFile;
import java.io.InputStream;
import java.io.IOException;

public class RandomAccessInputStream extends InputStream
{
	RandomAccessFile raf;

	public RandomAccessInputStream(String filename) throws IOException
	{
		raf = new RandomAccessFile(filename, "r");
	}

	public void close() throws IOException
	{
		raf.close();
	}

	public int read() throws IOException
	{
		return raf.read();
	}

	public int read(byte b[]) throws IOException
	{
		return read(b, 0, b.length);
	}

	public int read(byte b[], int offset, int length) throws IOException
	{
		return raf.read(b, offset, length);
	}

	public int available() throws IOException
	{
		return (int)(raf.length() - raf.getFilePointer());
	}

	public boolean markSupported()
	{
		return false;
	}

	public long skip(long n) throws IOException
	{
		return raf.skipBytes((int)n);
	}

	public long getFilePointer() throws IOException
	{
		return raf.getFilePointer();
	}

	public void seek(long pos) throws IOException
	{
		raf.seek(pos);
	}
}
