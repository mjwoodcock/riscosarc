package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.ZipCRC;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidZipFile;
import riscos.archive.InvalidCompressionType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.Vector;
import java.util.Enumeration;

public class ZipFileWrapper extends ArchiveFile
{
	private ZipFile zipFile;
	private Vector<ArchiveEntry> entry_list;
	private boolean appendFiletype;

	public ZipFileWrapper(String filename, String pass) throws IOException, InvalidArchiveFile
	{
		this(filename, pass, true);
	}

	public ZipFileWrapper(String filename, String pass, boolean appendFiletype) throws IOException, InvalidArchiveFile
	{
		try {
			this.zipFile = new ZipFile(filename);
		} catch (ZipException e) {
			throw new InvalidZipFile();
		}
		this.entry_list = new Vector<ArchiveEntry>();
		this.appendFiletype = appendFiletype;
	}

	public int read32()
	{
		return 0xffffffff;
	}

	public int read16()
	{
		return 0xffff;
	}

	public byte[] getPasswd()
	{
		return null;
	}

	public void openForRead() throws IOException, InvalidArchiveFile
	{
		Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
		while (entries.hasMoreElements()) {
			try {
				ZipEntry ze = entries.nextElement();
				if (!ze.isDirectory()) {
					this.entry_list.add(new ZipEntryWrapper(ze.getName(), this, ze, this.appendFiletype));
				}
			} catch (Exception e) {
				throw new InvalidZipFile();
			}
		}
	}

	public Enumeration<ArchiveEntry> entries()
	{
		return this.entry_list.elements();
	}

	public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType
	{
		try {
			ZipEntryWrapper ze = (ZipEntryWrapper)entry;
			return this.zipFile.getInputStream(ze.getZipEntry());
		} catch (IOException e) {
			throw new InvalidZipFile();
		}
	}

	public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile
	{
		return null;
	}

	public CRC getCRCInstance()
	{
		return new ZipCRC();
	}
}
