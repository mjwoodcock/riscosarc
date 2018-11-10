package riscos.archive.container;

import riscos.archive.RandomAccessInputStream;
import riscos.archive.InvalidSparkCompressionType;
import riscos.archive.InvalidSparkFSFile;
import riscos.archive.LZWInputStream;
import riscos.archive.GarbleInputStream;
import riscos.archive.LimitInputStream;
import riscos.archive.PackInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Enumeration;

public class SparkFSFile
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
	private static final byte ARCFS_HEADER_SIZE = 36;
	private RandomAccessInputStream in_file;
	private String archive_file;
	private int header_length;
	private int data_start;
	private int version;
	private int rw_version;
	private int arc_format;
	private boolean is_arcfs;
	private String current_dir;
	private Vector<ArchiveEntry> entry_list;
	private byte passwd[];

	public SparkFSFile(String filename, String pass)
	{
		archive_file = filename;
		is_arcfs = true;
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
		return is_arcfs;
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

	private void readHeader() throws InvalidSparkFSFile
	{
		try
		{
			byte b;

			b = (byte)in_file.read();
			if (b == SPARKFS_STARTBYTE)
			{
				is_arcfs = false;
			}
			else
			{
				if (b == 'A')
				{
					byte h[] = new byte[7];
					in_file.read(h);
					if (h[0] == 'r'
						&& h[1] == 'c'
						&& h[2] == 'h'
						&& h[3] == 'i'
						&& h[4] == 'v'
						&& h[5] == 'e'
						&& h[6] == 0)
					{
						is_arcfs = true;
					}
					else
					{
						throw new InvalidSparkFSFile();
					}
				}
				else
				{
					throw new InvalidSparkFSFile();
				}
			}
		}
		catch (IOException e)
		{
			throw new InvalidSparkFSFile();
		}
	}

	private void readArcfsHeader() throws IOException, InvalidSparkFSFile
	{
		header_length = read32();
		data_start = read32();
		version = read32();
		if (version > 40)
		{
			throw new InvalidSparkFSFile();
		}
		rw_version = read32();
		arc_format = read32();
		for (int i = 0; i < 17; i++)
		{
			read32(); // reserved
		}

	}

	public void openForRead() throws IOException, InvalidSparkFSFile
	{
		in_file = new RandomAccessInputStream(archive_file);

		readHeader();
		if (is_arcfs)
		{
			readArcfsHeader();
		}

		long offset = in_file.getFilePointer();
		do
		{
			SparkFSEntry fse = new SparkFSEntry(this, in_file, data_start);
			try
			{
				fse.readEntry(current_dir, offset);
				if (fse.isEof())
				{
					break;
				}
				if (fse.getCompressType() == SparkFSEntry.ARCFS_ENDDIR)
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
				if (fse.getCompressType() != SparkFSEntry.ARCFS_DELETED)
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
			catch (IOException e)
			{
				System.err.println(e.toString());
			}
		} while (true);
	}

	public Enumeration<ArchiveEntry> entries()
	{
		return entry_list.elements();
	}

	public InputStream getInputStream(ArchiveEntry entry) throws InvalidSparkFSFile, InvalidSparkCompressionType
	{
		try {
			in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidSparkFSFile();
		}

		LimitInputStream lis = new LimitInputStream(in_file, entry.getCompressedLength());
		GarbleInputStream gis = new GarbleInputStream(lis, passwd);

		if (isArcfs()) {
			gis.consumePasswdChar();
		}

		switch (entry.getCompressType())
		{
		case CT_NOTCOMP:
		case CT_NOTCOMP2:
			return gis;
		case CT_COMP:
			if (isArcfs()) {
				return new LZWInputStream(gis, 0, riscos.archive.LZWConstants.COMPRESS, entry.getMaxBits());
			} else {
				return new LZWInputStream(gis, 0, riscos.archive.LZWConstants.COMPRESS);
			}
		case CT_PACK:
			return new PackInputStream(gis);
		case CT_CRUNCH:
			return new PackInputStream(new LZWInputStream(gis, 0, riscos.archive.LZWConstants.CRUNCH, entry.getMaxBits()));
		default:
			throw new InvalidSparkCompressionType();
		}
	}

	public void printSparkInfo()
	{
		System.out.println("Header length = " + header_length);
		System.out.println("Data start = " + data_start);
		System.out.println("Version = " + version / 100 + "." + version % 100);
		System.out.println("RW Version = " + rw_version / 100 + "." + rw_version % 100);
		System.out.println("Arc format = " + arc_format);
	}
}
