/* An abstract class to deal with calculating CRC data. */

package riscos.archive;

public abstract class CRC
{
	public abstract void update(byte c);
	public abstract void update(byte[] chunk, int offset, int length);
	public abstract void update(byte[] chunk);
	public abstract void reset();
	public abstract long getValue();
	public abstract void setDataLength(int crcSize);
	public abstract boolean compare(long crcVal);
}
