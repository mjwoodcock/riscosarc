package riscos.archive.container;

import riscos.archive.*;
import riscos.archive.container.SparkFile;
import java.io.FilterInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

public class ArchiveEntry
{
	protected boolean is_dir;
	protected RandomAccessInputStream in_file;
	protected int data_start;
	protected int comptype;
	protected String name;
	protected String local_filename;
	protected int complen;
	protected int date;
	protected int time;
	protected long crc;
	protected int origlen;
	protected int load;
	protected int exec;
	protected int attr;
	protected long seek;
	protected int maxbits;
	protected long file_date;
	protected boolean is_del;
	protected boolean is_eof;
	protected boolean append_filetype;


	ArchiveEntry(RandomAccessInputStream in, int dat_start)
	{
		in_file = in;
		data_start = dat_start;
	}

	protected void calculateFileTime()
	{
		long high = (load & 0xff) - 0x33 & 0xffffffffl;
		long low;
		long lexec = ((long)exec) & 0xffffffffl;

		low = (lexec - 0x6e996a00l) & 0xffffffffl;
		file_date = (high * 42949673 + low / 100) & 0xffffffffl;
		file_date *= 1000;
	}

	public void cleanOldFile()
	{
		File f = new File(local_filename);

		if (f.isFile())
		{
			f.delete();
		}
	}

	public void setFileTime()
	{
		File f = new File(local_filename);

		f.setLastModified(file_date);
	}

	protected int getFileType()
	{
		int filetype = -1;
		if ((load & 0xfff00000) == 0xfff00000)
		{
			filetype = (load >> 8) & 0xfff;
		}

		return filetype;
	}

	protected void appendFiletype()
	{
		if (append_filetype)
		{
			if ((load & 0xfff00000) == 0xfff00000)
			{
				int filetype = (load >> 8) & 0xfff;
				local_filename += "," + Integer.toHexString(filetype);
			}
		}
	}

	protected static String translateFilename(String roname)
	{
		String localname = "";

		for (int i = 0; i < roname.length(); i++ ) {
			switch (roname.charAt(i)) {
				case '/':
					localname += '.';
					break;
				case '.':
					localname += '/';
					break;
				default:
					localname += roname.charAt(i);
					break;
			}
		}

		return localname;
	}

	public int getCompressedLength()
	{
		return complen;
	}

	public long getOffset()
	{
		return seek;
	}

	public int getUncompressedLength()
	{
		return origlen;
	}

	public void mkDir(String prefix)
	{
		File f = new File(prefix + File.separator + local_filename);
		File parent = f.getParentFile();

		if (parent != null && !parent.isDirectory())
		{
			if (!parent.isFile())
			{
				parent.mkdirs();
			}
		}
	}

	public void mkDir()
	{
		mkDir("");
	}

	public boolean isDir()
	{
		return is_dir;
	}

	public String getLocalFilename()
	{
		return local_filename;
	}

	public void printEntryData()
	{
	}

	public int getMaxBits()
	{
		return maxbits;
	}

	public int getCompressType()
	{
		return comptype;
	}

	public String getName()
	{
		return name;
	}

	public long getCrcValue()
	{
		return crc;
	}
}
