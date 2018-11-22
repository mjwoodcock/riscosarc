package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.InvalidCompressionType;
import riscos.archive.InvalidArchiveFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public abstract class ArchiveFile
{
	/** Returns the password the user is using to open the archive file
	 * @return the byte array of the password
	 */
	public abstract byte[] getPasswd();

	/** Reads a 32 bit value from the archive file
	 * @return 32 bit value
	 */
	public abstract int read32() throws IOException;

	/** Reads a 16 bit value from the archive file
	 * @return 16 bit value
	 */
	public abstract int read16() throws IOException;

	/** Opens an archive file for reading, and checks the integriry of the
	 * file
	 */
	public abstract void openForRead() throws IOException, InvalidArchiveFile;

	/** Gets this ArchiveFile's list of ArchiveEntry instances
	 * @return All the ArchiveEntry instances for the ArchiveFile
	 */
	public abstract Enumeration<ArchiveEntry> entries();

	/** Gets an InputStream for a specific ArchiveEntry.  The decompressed
	 * file data can be read() from the InputStream
	 * @param entry an ArchiveEntry for which the InputStream is wanted
	 * @return An InputStream for the ArchiveEntry
	 * @see ArchiveEntry
	 */
	public abstract InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType;

	/** Gets an InputStream for a specific ArchiveEntry.  The compressed
	 * file data stored in the ArchiveFile can be read() from the
	 * InputStream.  This is not particularly useful unless you have
	 * a separate decompressor that you want to use to decompress the data.
	 * @param entry an ArchiveEntry for which the InputStream is wanted
	 * @return An InputStream for the ArchiveEntry
	 * @see ArchiveEntry
	 */
	public abstract InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType;

	/** Get the CRC instance to calculate the CRC for data from this file
	 * @return the CRC instance
	 * @see CRC
	 */
	public abstract CRC getCRCInstance();
}
