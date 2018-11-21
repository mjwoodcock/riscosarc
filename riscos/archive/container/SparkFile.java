package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.SparkCRC;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.InvalidSparkCompressionType;
import riscos.archive.InvalidSparkFile;
import riscos.archive.InvalidCompressionType;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.LZWInputStream;
import riscos.archive.GarbleInputStream;
import riscos.archive.LimitInputStream;
import riscos.archive.PackInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Enumeration;

public class SparkFile extends ArchiveFile
{
	public static final int CT_NOTCOMP = 0x01;
	public static final int CT_NOTCOMP2 = 0x02;
	public static final int CT_PACK = 0x03;
	public static final int CT_PACKSQUEEZE = 0x04;
	public static final int CT_LZOLD = 0x05;
	public static final int CT_LZNEW = 0x06;
	public static final int CT_LZW = 0x07;
	public static final int CT_CRUNCH = 0x08;
	public static final int CT_SQUASH = 0x09;
	public static final int CT_COMP = 0x7f;

	private static final byte SPARKFS_STARTBYTE = 0x1a;
	private RandomAccessInputStream in_file;
	private String archive_file;
	private int header_length;
	private int data_start;
	private String current_dir;
	private Vector<ArchiveEntry> entry_list;
	private byte passwd[];

	public SparkFile(String filename, String pass)
	{
		archive_file = filename;
		entry_list = new Vector<ArchiveEntry>();
		current_dir = "";
		if (pass != null)
		{
			passwd = pass.getBytes();
		}
	}

	public byte[] getPasswd()
	{
		return passwd;
	}

	public boolean isArcfs()
	{
		return false;
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
		int r = 0;

		r = (in_file.read()) & 0xff;
		r |= (in_file.read() & 0xff) << 8;

		return r;
	}

	private void readHeader() throws InvalidArchiveFile
	{
		try
		{
			byte b1 = (byte)in_file.read();
			byte b2 = (byte)in_file.read();

			/* If b2 high bit is not set, then this is a DOS
			 * format .arc file */
			if (b1 != SPARKFS_STARTBYTE || ((b2 & 0x80) == 0x0) )
			{
				throw new InvalidSparkFile();
			}
		}
		catch (IOException e)
		{
			throw new InvalidSparkFile();
		}
	}

	public void openForRead() throws IOException, InvalidArchiveFile
	{
		in_file = new RandomAccessInputStream(archive_file);

		readHeader();

		long offset = 1;
		do
		{
			SparkEntry fse = new SparkEntry(this, in_file, data_start);
			try
			{
				fse.readEntry(current_dir, offset);
				if (fse.isEof())
				{
					break;
				}
				if (fse.getCompressType() == SparkEntry.SPARKFS_ENDDIR)
				{
					offset += 2;
					int idx = current_dir.lastIndexOf('/');
					if (idx > -1)
					{
						current_dir = current_dir.substring(0, idx);
					}
					else
					{
						current_dir = "";
					}
					continue;
				}
				else
				{
					if (fse.isDir())
					{
						if (current_dir != "")
						{
							current_dir = current_dir + "/" + fse.getName();
						}
						else
						{
							current_dir = fse.getName();
						}
					}
					else
					{
						entry_list.add(fse);
					}
				}
				offset = fse.getNextEntryOffset();
			}
			catch (Exception e)
			{
				System.err.println(e.toString());
				break;
			}
		} while (true);
	}

	public Enumeration<ArchiveEntry> entries()
	{
		return entry_list.elements();
	}

	public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType
	{
		try {
			in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidSparkFile();
		}

		LimitInputStream lis = new LimitInputStream(in_file, entry.getCompressedLength());
		GarbleInputStream gis = new GarbleInputStream(lis, passwd);

		switch (entry.getCompressType())
		{
		case CT_NOTCOMP:
		case CT_NOTCOMP2:
			return gis;
		case CT_COMP:
			return new LZWInputStream(gis, 0, riscos.archive.LZWConstants.COMPRESS);
		case CT_PACK:
			return new PackInputStream(gis);
		case CT_CRUNCH:
			return new PackInputStream(new LZWInputStream(gis, 0, riscos.archive.LZWConstants.CRUNCH, entry.getMaxBits()));
		case CT_SQUASH:
			return new LZWInputStream(gis, 0, riscos.archive.LZWConstants.SQUASH);
		default:
			throw new InvalidSparkCompressionType();
		}
	}

	public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile
	{
		try {
			in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidSparkFile();
		}

		return new LimitInputStream(in_file, entry.getCompressedLength());
	}

	public void printSparkInfo()
	{
		System.out.println("Header length = " + header_length);
		System.out.println("Data start = " + data_start);
	}

	public CRC getCRCInstance()
	{
		return new SparkCRC();
	}
}
