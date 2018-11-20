package riscos.archive.container;

import riscos.archive.*;
import riscos.archive.container.ArchiveEntry;
import java.io.IOException;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public class ZipEntryWrapper extends ArchiveEntry
{
	private ZipFileWrapper zipFile;
	private ZipEntry zipEntry;
	private static final short EF_SPARK = 0x4341;
	private static final int SPARKID_2 = 0x30435241;

	public ZipEntryWrapper(String name, ZipFileWrapper zip, ZipEntry ze)
	{
		super(null, 0);
		this.zipFile = zip;
		this.zipEntry = ze;
		this.name = name;
		this.local_filename = name;
		super.complen = (int)this.zipEntry.getCompressedSize();
		super.origlen = (int)this.zipEntry.getSize();
		super.crc = this.zipEntry.getCrc();
		if (ze.getExtra() != null) {
			parseExtraData(ze.getExtra());
		}
		append_filetype = true;
	}

	private short byteArrayToShort(byte b[], int offset)
	{
		return (short)((b[offset + 1] << 8) | (b[offset] & 0xff));
	}

	private int byteArrayToInt(byte b[], int offset)
	{
		long l1 = (b[offset + 3] << 24) & 0xff000000;
		long l2 = (b[offset + 2] << 16) & 0xff0000;
		long l3 = (b[offset + 1] << 8) & 0xff00;
		long l4 = b[offset + 0] & 0xff;
		return (int)(l1 | l2 | l3 | l4);
	}

	private void parseExtraData(byte b[])
	{
		int id = byteArrayToShort(b, 0);
		int len = byteArrayToShort(b, 2);

		if (id == 0x4341) {
			if (len == 24 || len == 20) {
				int id2 = byteArrayToInt(b, 4);
				if (id2 == SPARKID_2) {
					super.load = byteArrayToInt(b, 8);
					super.exec = byteArrayToInt(b, 12);
					super.attr = byteArrayToInt(b, 16);

					calculateFileTime();
				}
			}
		}
	}

	public void readEntry(String cur_dir, long offset) throws IOException, InvalidSparkFile
	{
	}

	public void printEntryData()
	{
	}

	public boolean isEof()
	{
		return false;
	}

	public long getNextEntryOffset()
	{
		return -1;
	}

	public ZipEntry getZipEntry()
	{
		return this.zipEntry;
	}
}
