package riscos.archive.container;

import riscos.archive.*;
import riscos.archive.RandomAccessInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Enumeration;

public class PackDirFile
{
	public static final int CT_NOTCOMP = 0x01;
	public static final int CT_LZW = 0x07;

	private RandomAccessInputStream in_file;
	private String archive_file;
	private int lzw_bits;
	private String current_dir;
	private Vector<ArchiveEntry> entry_list;
	private int dir_entries[];
	private int dir_entriesi;
	private int num_files;
	private int num_dirs;

	public PackDirFile(String filename)
	{
		archive_file = filename;
		entry_list = new Vector<ArchiveEntry>();
		current_dir = "";
		dir_entries = new int[100];
		dir_entriesi = -1;
		num_files = 0;
		num_dirs = 0;
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

	public String readString() throws IOException
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
		} while (r != 0);

		return s.toString();
	}

	private void readHeader() throws InvalidPackDirFile
	{
		try
		{
			String hdr = readString();
			if (hdr.equals("PACK"))
			{
				lzw_bits = read32();
				lzw_bits += 12;
			}
			else
			{
				throw new InvalidPackDirFile();
			}
		}
		catch (IOException e)
		{
			throw new InvalidPackDirFile();
		}
	}

	public void openForRead() throws IOException, InvalidPackDirFile
	{
		in_file = new RandomAccessInputStream(archive_file);

		readHeader();

		long offset = in_file.getFilePointer();
		do
		{
			PackDirEntry pde = new PackDirEntry(this, in_file, lzw_bits);
			try
			{
				pde.readEntry(current_dir, offset);
				if (pde.isDir())
				{
					num_dirs++;
					if (current_dir.equals(""))
					{
						current_dir = pde.getName();
					}
					else
					{
						current_dir = current_dir + "/" + pde.getName();
					}
					if (dir_entriesi >= 0)
					{
						--dir_entries[dir_entriesi];
					}
					dir_entries[++dir_entriesi] = pde.getNumEntries();
				}
				else
				{
					num_files++;
					if (dir_entriesi >= 0)
					{
						--dir_entries[dir_entriesi];
					}
					entry_list.add(pde);
				}
				while (dir_entriesi >= 0 && dir_entries[dir_entriesi] == 0)
				{
					int i = current_dir.lastIndexOf('/');

					if (i != -1)
					{
						current_dir = current_dir.substring(0, i);
					}
					else
					{
						current_dir = "";
					}
					dir_entriesi--;
				}
				offset = pde.getNextEntryOffset();
			}
			catch (IOException e)
			{
				System.err.println(e.toString());
			}
		} while (dir_entries[0] > 0);
	}

	public Enumeration<ArchiveEntry> entries()
	{
		return entry_list.elements();
	}

	public InputStream getInputStream(ArchiveEntry entry) throws InvalidPackDirFile, InvalidPackDirCompressionType
	{
		try {
			in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidPackDirFile();
		}

		LimitInputStream lis = new LimitInputStream(in_file, entry.getCompressedLength());

		switch (entry.getCompressType())
		{
		case CT_NOTCOMP:
			return lis;
		case CT_LZW:
			return new LZWInputStream(lis, 0, riscos.archive.LZWConstants.PACKDIR, entry.getMaxBits());
		default:
			throw new InvalidPackDirCompressionType();
		}
	}

	public void printInfo()
	{
		System.out.println("Number of bits: " + lzw_bits);
		System.out.println("Number of files: " + num_files);
		System.out.println("Number of dirs: " + num_dirs);
	}
}
