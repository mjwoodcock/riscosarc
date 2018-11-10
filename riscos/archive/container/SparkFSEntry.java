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
	public static final int ARCFS_ENDDIR = 0x0;
	public static final int ARCFS_DELETED = 0x1;
	public static final int ARCFS_STORE = 0x82;
	public static final int ARCFS_PACK = 0x83;
	public static final int ARCFS_CRUNCH = 0x88;
	public static final int ARCFS_COMPRESS = 0xff;
	public static final int ARCHPACK = 0x80;

	private SparkFSFile spark_file;

	private long entry_offset;
	private long next_entry_offset;

	private void convertCompType()
	{
		switch (comptype)
		{
		case SparkFSEntry.ARCFS_STORE:
			comptype = SparkFSFile.CT_NOTCOMP2;
			break;
		case SparkFSEntry.ARCFS_PACK:
			comptype = SparkFSFile.CT_PACK;
			break;
		case SparkFSEntry.ARCFS_CRUNCH:
			comptype = SparkFSFile.CT_CRUNCH;
			break;
		case SparkFSEntry.ARCFS_COMPRESS:
			comptype = SparkFSFile.CT_COMP;
			break;
		}
	}


	public SparkFSEntry(SparkFSFile spark, RandomAccessInputStream in, int dat_start)
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
		convertCompType();
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
			comptype = ARCFS_ENDDIR;
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
		if (!cur_dir.equals(""))
		{
			local_filename = cur_dir + "/" + name;
		}
		else
		{
			local_filename = name;
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

		if (spark_file.isArcfs())
		{
			readArcfsEntry(cur_dir);
		}
		else
		{
			readSparkEntry(cur_dir);
		}
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
