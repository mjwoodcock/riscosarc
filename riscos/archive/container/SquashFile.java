package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.NullCRC;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidSquashFile;
import riscos.archive.UnsupportedLZWType;
import riscos.archive.NcompressLZWInputStream;
import riscos.archive.LimitInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Enumeration;

public class SquashFile extends ArchiveFile
{
	private RandomAccessInputStream in_file;
	private String archive_file;
	private Vector<ArchiveEntry> entry_list;

	public SquashFile(String filename)
	{
		archive_file = filename;
		entry_list = new Vector<ArchiveEntry>();
	}

	public int read32() throws IOException
	{
		int r = 0;

		r = (in_file.read()) & 0xff;
		r |= (in_file.read() & 0xff) << 8;
		r |= (in_file.read() & 0xff) << 16;
		r |= (in_file.read() & 0xff) << 24;

		return r;
	}

	public int read16() throws IOException
	{
		return 0xffffffff;
	}

	public String readString(int len) throws IOException
	{
		StringBuffer s = new StringBuffer();
		int r;

		do
		{
			r = in_file.read();
			if (r != 0)
			{
				s.append((char)r);
			}
		} while (len-- > 1 && r != 0);

		return s.toString();
	}

	private void readHeader() throws InvalidArchiveFile
	{
		try
		{
			String hdr = readString(4);
			if (!hdr.equals("SQSH"))
			{
				throw new InvalidSquashFile("Bad magic");
			}
		}
		catch (IOException e)
		{
			// throw strop
		}
	}

	public void openForRead() throws IOException, InvalidArchiveFile
	{
		in_file = new RandomAccessInputStream(archive_file);

		readHeader();

		long offset = in_file.getFilePointer();

		SquashEntry se = new SquashEntry(this, in_file, archive_file);
		try
		{
			se.readEntry(offset);
			entry_list.add(se);
		}
		catch (IOException e)
		{
			System.err.println(e.toString());
		}
	}

	public Enumeration<ArchiveEntry> entries()
	{
		return entry_list.elements();
	}

	public InputStream getInputStream(ArchiveEntry entry) throws InvalidSquashFile
	{
		try {
			in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidSquashFile("Bad seek");
		}

		LimitInputStream lis = new LimitInputStream(in_file, entry.getCompressedLength());

		try {
			return new NcompressLZWInputStream(lis, 0, riscos.archive.LZWConstants.UNIX_COMPRESS);
		} catch (UnsupportedLZWType e) {
			throw new InvalidSquashFile(e.toString());
		}
	}

	public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidSquashFile
	{
		try {
			in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidSquashFile("Bad seek");
		}

		return new LimitInputStream(in_file, entry.getCompressedLength());
	}

	public void printInfo()
	{
	}

	public byte[] getPasswd()
	{
		return null;
	}

	public CRC getCRCInstance()
	{
		return new NullCRC();
	}
}
