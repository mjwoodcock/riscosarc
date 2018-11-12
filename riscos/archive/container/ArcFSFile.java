package riscos.archive.container;

import riscos.archive.RandomAccessInputStream;
import riscos.archive.InvalidArcFSCompressionType;
import riscos.archive.InvalidArcFSFile;
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

public class ArcFSFile extends ArchiveFile
{
	public static final int ARCFS_STORE = 0x82;
	public static final int ARCFS_PACK = 0x83;
	public static final int ARCFS_CRUNCH = 0x88;
	public static final int ARCFS_COMPRESS = 0xff;

	private static final byte ARCFS_HEADER_SIZE = 36;
	private RandomAccessInputStream in_file;
	private String archive_file;
	private int header_length;
	private int data_start;
	private int version;
	private int rw_version;
	private int arc_format;
	private String current_dir;
	private Vector<ArchiveEntry> entry_list;
	private byte passwd[];

	public ArcFSFile(String filename, String pass)
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
			byte b;

			b = (byte)in_file.read();
			if (b == 'A')
			{
				byte h[] = new byte[7];
				in_file.read(h);
				if (!(h[0] == 'r'
					&& h[1] == 'c'
					&& h[2] == 'h'
					&& h[3] == 'i'
					&& h[4] == 'v'
					&& h[5] == 'e'
					&& h[6] == 0))
				{
					throw new InvalidArcFSFile();
				}
			}
			else
			{
				throw new InvalidArcFSFile();
			}
		}
		catch (IOException e)
		{
			throw new InvalidArcFSFile();
		}
	}

	private void readArcfsHeader() throws IOException, InvalidArchiveFile
	{
		header_length = read32();
		data_start = read32();
		version = read32();
		if (version > 40)
		{
			throw new InvalidArcFSFile();
		}
		rw_version = read32();
		arc_format = read32();
		for (int i = 0; i < 17; i++)
		{
			read32(); // reserved
		}

	}

	public void openForRead() throws IOException, InvalidArchiveFile
	{
		in_file = new RandomAccessInputStream(archive_file);

		readHeader();
		readArcfsHeader();

		long offset = in_file.getFilePointer();
		int num_entries = header_length / 36;
		for (int i = 0; i < num_entries; i++)
		{
			ArcFSEntry fse = new ArcFSEntry(this, in_file, data_start);
			try
			{
				fse.readEntry(current_dir, offset);
				if (fse.isEof())
				{
					break;
				}
				if (fse.getCompressType() == ArcFSEntry.ARCFS_ENDDIR)
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
				}
				else if (fse.getCompressType() != ArcFSEntry.ARCFS_DELETED)
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
		}
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
			throw new InvalidArcFSFile();
		}

		LimitInputStream lis = new LimitInputStream(in_file, entry.getCompressedLength());
		GarbleInputStream gis = new GarbleInputStream(lis, passwd);

		gis.consumePasswdChar();

		switch (entry.getCompressType())
		{
		case ARCFS_STORE:
			return gis;
		case ARCFS_COMPRESS:
			return new LZWInputStream(gis, 0, riscos.archive.LZWConstants.COMPRESS, entry.getMaxBits());
		case ARCFS_PACK:
			return new PackInputStream(gis);
		case ARCFS_CRUNCH:
			return new PackInputStream(new LZWInputStream(gis, 0, riscos.archive.LZWConstants.CRUNCH, entry.getMaxBits()));
		default:
			throw new InvalidArcFSCompressionType();
		}
	}

	public void printArcFSInfo()
	{
		System.out.println("Header length = " + header_length);
		System.out.println("Data start = " + data_start);
		System.out.println("Version = " + version / 100 + "." + version % 100);
		System.out.println("RW Version = " + rw_version / 100 + "." + rw_version % 100);
		System.out.println("Arc format = " + arc_format);
	}
}
