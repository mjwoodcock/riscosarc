package riscos.archive.container;

import riscos.archive.*;
import riscos.archive.container.ArcFSFile;
import riscos.archive.container.ArchiveEntry;
import java.io.FilterInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

public class ArcFSEntry extends ArchiveEntry
{
	public static final int ARCFS_ENDDIR = 0x0;
	public static final int ARCFS_DELETED = 0x1;
	public static final int ARCHPACK = 0x80;

	private ArcFSFile spark_file;

	private long entry_offset;
	private long next_entry_offset;

	public ArcFSEntry(ArcFSFile spark, RandomAccessInputStream in, int dat_start)
	{
		super(in, dat_start);
		spark_file = spark;
		is_del = false;
		is_eof = false;
		append_filetype = true;
	}

	private void readArcfsEntry(String cur_dir) throws IOException
	{
		byte n[] = new byte[12];
		n[11] = 0;
		int r = in_file.read();
		if (r == -1)
		{
			is_eof = true;
		}
		comptype = r & 0xff;
		if (comptype == 0x1)
		{
			is_del = true;
		}
		in_file.read(n, 0, n.length - 1);
		int nul;
		for (nul = 0; nul < n.length; nul++)
		{
			if (n[nul] == 0)
			{
				break;
			}
		}
		name = new String(n, 0, nul);
		if (!cur_dir.equals(""))
		{
			local_filename = cur_dir + "/" + name;
		}
		else
		{
			local_filename = name;
		}
		origlen = spark_file.read32();
		load = spark_file.read32();
		exec = spark_file.read32();
		int a = spark_file.read32();
		crc = (a >> 16) & 0xffff;
		attr = a & 0xff;
		maxbits = ((a & 0xff00) >> 8) & 0xff;
		complen = spark_file.read32();
		int info_word = spark_file.read32();
		seek = (info_word & 0x7fffffff) + data_start;
		if (((info_word >> 31) & 0x1) != 0)
		{
			is_dir = true;
		}
		else
		{
			is_dir = false;
		}
		appendFiletype();
		calculateFileTime();
		next_entry_offset = in_file.getFilePointer();
	}

	public void readEntry(String cur_dir, long offset) throws IOException
	{
		in_file.seek(offset);
		entry_offset = in_file.getFilePointer();

		readArcfsEntry(cur_dir);
	}

	public void printEntryData()
	{
		System.out.println("Comptype = " + comptype);
		System.out.println("Name " + name);
		System.out.println("Local name " + local_filename);
		System.out.println("Origlen " + origlen);
		System.out.println("Load " + load);
		System.out.println("Exec " + exec);
		System.out.println("CRC " + crc);
		System.out.println("attr " + attr);
		System.out.println("maxbits " + maxbits);
		System.out.println("complen " + complen);
		System.out.println("seek " + seek);
		System.out.println("is_dir " + is_dir);
	}

	public boolean isEof()
	{
		return is_eof;
	}

	public long getNextEntryOffset()
	{
		return next_entry_offset;
	}
}
