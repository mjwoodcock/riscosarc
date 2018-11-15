package riscos.archive.container;

import riscos.archive.*;
import riscos.archive.container.SparkFSFile;
import riscos.archive.container.ArchiveEntry;
import java.io.FilterInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

public class SparkFSEntry extends ArchiveEntry
{
	public static final int SPARKFS_ENDDIR = 0x0;
	public static final int ARCHPACK = 0x80;

	private SparkFSFile spark_file;

	private long entry_offset;
	private long next_entry_offset;

	public SparkFSEntry(SparkFSFile spark, RandomAccessInputStream in, int dat_start)
	{
		super(in, dat_start);
		spark_file = spark;
		is_del = false;
		is_eof = false;
		append_filetype = true;
	}

	private void readSparkEntry(String cur_dir) throws IOException
	{
		int date;
		int time;
		int r = in_file.read();

		if (r == -1)
		{
			is_eof = true;
			return;
		}

		comptype = r & 0xff;
		if ((comptype & ~ARCHPACK) == 0)
		{
			comptype = SPARKFS_ENDDIR;
			return;
		}
		
		byte n[] = new byte[14];
		n[12] = '\0';
		in_file.read(n, 0, n.length - 1);
		int nul;
		for (nul = 0; nul < n.length; nul++)
		{
			if (n[nul] < ' ' || n[nul] > '~')
			{
				n[nul] = '\0';
				break;
			}
		}

		name = new String(n, 0, nul);
		if (name.length() == 0)
		{
			/* Some old Spark files have some extra empty
			 * data on the end. */
			is_eof = true;
			comptype = SPARKFS_ENDDIR;
			return;
		}

		if (!cur_dir.equals(""))
		{
			local_filename = cur_dir + "/" + ArchiveEntry.translateFilename(name);
		}
		else
		{
			local_filename = ArchiveEntry.translateFilename(name);
		}
		complen = spark_file.read32();
		date = spark_file.read16();
		time = spark_file.read16();
		crc = spark_file.read16();
		if ((comptype & ~ARCHPACK) > SparkFSFile.CT_NOTCOMP)
		{
			origlen = spark_file.read32();
		}
		else
		{
			origlen = complen;
		}
		if ((comptype & ARCHPACK) == ARCHPACK)
		{
			load = spark_file.read32();
			exec = spark_file.read32();
			attr = spark_file.read32();
		}
		comptype &= ~ARCHPACK;
		seek = (int)in_file.getFilePointer();
		if ((load & 0xffffff00) == 0xfffddc00)
		{
			is_dir = true;
		}
		if (is_dir)
		{
			next_entry_offset = seek + 1;
		}
		else
		{
			next_entry_offset = seek + complen + 1;
		}
		calculateFileTime();
		appendFiletype();
	}

	public void readEntry(String cur_dir, long offset) throws IOException
	{
		in_file.seek(offset);
		entry_offset = in_file.getFilePointer();

		readSparkEntry(cur_dir);
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
