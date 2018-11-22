package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.NullCRC;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.InvalidCFSFile;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.CFSInputStream;
import riscos.archive.LimitInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Enumeration;

public class CFSFile extends ArchiveFile
{
	private RandomAccessInputStream in_file;
	private String archive_file;
	private Vector<ArchiveEntry> entry_list;
	private boolean appendFiletype;

	public CFSFile(String filename)
	{
		this(filename, true);
	}

	public CFSFile(String filename, boolean appendFiletype)
	{
		this.archive_file = filename;
		this.entry_list = new Vector<ArchiveEntry>();
		this.appendFiletype = appendFiletype;
	}

	public int read32() throws IOException
	{
		int r = 0;

		r = (this.in_file.read()) & 0xff;
		r |= (this.in_file.read() & 0xff) << 8;
		r |= (this.in_file.read() & 0xff) << 16;
		r |= (this.in_file.read() & 0xff) << 24;

		return r;
	}

	public int read16() throws IOException
	{
		return 0xffffffff;
	}

	private void readHeader() throws InvalidArchiveFile
	{
		try {
			read32();
			int magic = read32();
			if (magic != 0x303) {
				throw new InvalidCFSFile();
			}
		} catch (IOException e) {
			// throw strop
		}
	}

	public void openForRead() throws IOException, InvalidArchiveFile
	{
		this.in_file = new RandomAccessInputStream(this.archive_file);

		readHeader();

		CFSEntry se = new CFSEntry(this, this.in_file, this.archive_file, this.appendFiletype);
		try {
			se.readEntry(0);
			this.entry_list.add(se);
		} catch (IOException e) {
			System.err.println(e.toString());
		}
	}

	public Enumeration<ArchiveEntry> entries()
	{
		return this.entry_list.elements();
	}

	public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile
	{
		try {
			this.in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidCFSFile();
		}

		LimitInputStream lis = new LimitInputStream(this.in_file, entry.getCompressedLength());

		return new CFSInputStream(lis);
	}

	public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile
	{
		try {
			this.in_file.seek(entry.getOffset());
		} catch (IOException e) {
			throw new InvalidCFSFile();
		}

		return new LimitInputStream(this.in_file, entry.getCompressedLength());
	}

	public void printInfo()
	{
	}

	public byte[] getPasswd()
	{
		return null;
	}

	public CRC getCRCInstance()
	{
		return new NullCRC();
	}
}
