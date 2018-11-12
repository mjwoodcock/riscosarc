package riscos.archive.container;

import riscos.archive.InvalidCompressionType;
import riscos.archive.InvalidArchiveFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public abstract class ArchiveFile
{
	public abstract byte[] getPasswd();
	public abstract int read32() throws IOException;
	public abstract int read16() throws IOException;
	public abstract void openForRead() throws IOException, InvalidArchiveFile;
	public abstract Enumeration<ArchiveEntry> entries();
	public abstract InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType;
}
