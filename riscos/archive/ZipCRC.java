/* A class to calculate the CRC of Zip data */
package riscos.archive;

import java.util.zip.CRC32;

public class ZipCRC extends CRC
{
	private long crcValue;
	CRC32 crc32;

	public ZipCRC()
	{
		crc32 = new CRC32();
	}

	public void setDataLength(int crcSize)
	{
	}

	public void update(byte c)
	{
		crc32.update(c);
	}

	public void update(byte[] chunk, int offset, int length)
	{
		crc32.update(chunk, offset, length);
	}

	public void update(byte[] chunk)
	{
		update(chunk, 0, chunk.length);
	}

	public void reset()
	{
		crc32.reset();
	}

	public long getValue()
	{
		return this.getValue();
	}

	public boolean compare(long crcVal)
	{
		return crc32.getValue() == crcVal;
	}
}
