package riscos.archive.container;

import riscos.archive.*;
import riscos.archive.container.PackDirFile;
import java.io.FilterInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

public class PackDirEntry extends ArchiveEntry
{
	public static final int ENTRY_TYPE_FILE = 0;
	public static final int ENTRY_TYPE_DIR = 1;

	public static final long ROOT_DIR_OFFSET = 9;

	private PackDirFile packdir_file;
	private int num_entries;

	private long entry_offset;
	private long next_entry_offset;

	public PackDirEntry(PackDirFile packdir, RandomAccessInputStream in, int lzw_bits)
	{
		super(in, 0);
		packdir_file = packdir;
		maxbits = lzw_bits;
		append_filetype = true;
	}

	public void readEntry(String cur_dir, long offset) throws IOException
	{
		int n;
		int type;
		boolean root = false;

		if (offset == ROOT_DIR_OFFSET)
		{
			root = true;
		}
		in_file.seek(offset);
		entry_offset = in_file.getFilePointer();
		name = packdir_file.readString();
		if (name != "")
		{
			if (root)
			{
				int i = name.lastIndexOf('.');
				if (i == -1)
				{
					i = name.lastIndexOf(':');
				}
				if (i != -1)
				{
					name = name.substring(i + 1);
				}
			}
			load = packdir_file.read32();
			exec = packdir_file.read32();
			n = packdir_file.read32();
			attr = packdir_file.read32();
			if (root)
			{
				type = ENTRY_TYPE_DIR;
			}
			else
			{
				type = packdir_file.read32();
			}
			if (type == ENTRY_TYPE_DIR)
			{
				is_dir = true;
				num_entries = n;
			}
			else
			{
				is_dir = false;
				origlen = n;
				complen = packdir_file.read32();
				if (complen == -1)
				{
					comptype = PackDirFile.CT_NOTCOMP;
					complen = origlen;
				}
				else
				{
					comptype = PackDirFile.CT_LZW;
				}
			}

			if (!cur_dir.equals(""))
			{
				local_filename = cur_dir + "/" + name.replace('/', '.');
			}
			else
			{
				local_filename = name;
			}
			seek = in_file.getFilePointer();
			calculateFileTime();
			if (is_dir)
			{
				next_entry_offset = seek;
			}
			else
			{
				appendFiletype();
				next_entry_offset = seek + (complen == -1 ? origlen : complen);
			}
		}
	}

	public void printEntryData()
	{
		System.out.println("Comptype = " + comptype);
		System.out.println("Name " + name);
		System.out.println("Local name " + local_filename);
		if (is_dir)
		{
			System.out.println("Num entries " + num_entries);
		}
		else
		{
			System.out.println("Complen " + complen);
			System.out.println("Origlen " + origlen);
		}
		System.out.println("Load " + Integer.toHexString(load));
		System.out.println("Exec " + Integer.toHexString(exec));
		System.out.println("attr " + Integer.toHexString(attr));
		System.out.println("maxbits " + maxbits);
		System.out.println("seek " + seek);
		System.out.println("is_dir " + is_dir);
	}

	public long getNextEntryOffset()
	{
		return next_entry_offset;
	}

	public int getNumEntries()
	{
		return num_entries;
	}
}
