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


	/** Constructs and ArchiveEntry.
	 * @param in an input stream to read the data from
	 * @param dat_start the location in the stream of the compressed data
	 * @param appendFiletype true if the filetype should be appended to the filename.  False otherwise
	 */
	ArchiveEntry(RandomAccessInputStream in, int dat_start, boolean appendFiletype)
	{
		this.in_file = in;
		this.data_start = dat_start;
		this.append_filetype = appendFiletype;
	}

	/**
	 * Calculates the file time from the RISC OS load and exec values
	 */
	protected void calculateFileTime()
	{
		long high = (load & 0xff) - 0x33 & 0xffffffffl;
		long low;
		long lexec = ((long)exec) & 0xffffffffl;

		low = (lexec - 0x6e996a00l) & 0xffffffffl;
		file_date = (high * 42949673 + low / 100) & 0xffffffffl;
		file_date *= 1000;
	}

	/**
	 * Deletes a stale file from the disk.
	 */
	public void cleanOldFile()
	{
		File f = new File(local_filename);

		if (f.isFile())
		{
			f.delete();
		}
	}

	/**
	 * Stamps the file with the correct date stamp.
	 */
	public void setFileTime()
	{
		File f = new File(local_filename);

		f.setLastModified(file_date);
	}

	/**
	 * Gets the RISC OS filetype from the RISC OS load value
	 * @return the filetype
	 */
	protected int getFileType()
	{
		int filetype = -1;
		if ((load & 0xfff00000) == 0xfff00000)
		{
			filetype = (load >> 8) & 0xfff;
		}

		return filetype;
	}

	/**
	 * Appends the RISC OS filetype to the filename if necessary.
	 */
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

	/**
	 * Translates the filename from a RISC OS representation to the
	 * local representation.
	 * @param roname the filename according to RISC OS
	 * @return the filename suitable for the local filesystem
	 */
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

	/** Gets the length of the compressed data in the containing ArchiveFile
	 * @return the compressed data length
	 * @see ArchiveFile
	 */
	public int getCompressedLength()
	{
		return complen;
	}

	/** Gets the offset of the compressed data in the containing ArchiveFile
	 * @return the offset of the data
	 * @see ArchiveFile
	 */
	public long getOffset()
	{
		return seek;
	}

	/** Gets the length of the uncompressed data of this ArchiveEntry
	 * @return the uncompressed file length
	 */
	public int getUncompressedLength()
	{
		return origlen;
	}

	/** Creates the directory structire needed to extract the file
	 * @param prefix the base directory to create the structure in
	 */
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

	/** Creates the directory structire (under the current directory)
	 * needed to extract the file
	 */
	public void mkDir()
	{
		mkDir("");
	}

	public boolean isDir()
	{
		return is_dir;
	}

	/** Gets the filename as it would appear on the local filesystem
	 * @return the local filename
	 */
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

	/* Gets the name as it would appear on RISC OS
	 * @return the filename
	 */
	public String getName()
	{
		return name;
	}

	/* Gets the CRC value
	 * @return the crc value
	 * @see CRC
	 */
	public long getCrcValue()
	{
		return crc;
	}
}
