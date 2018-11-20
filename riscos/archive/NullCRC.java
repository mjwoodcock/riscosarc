/* A class to handle container files that do not hold CRC information */

package riscos.archive;

public class NullCRC extends CRC
{
	public NullCRC()
	{
	}

	public void setDataLength(int crcSize)
	{
	}

	public void update(byte c)
	{
	}

	public void update(byte[] chunk, int offset, int length)
	{
	}

	public void update(byte[] chunk)
	{
	}

	public void reset()
	{
	}

	public long getValue()
	{
		return 0;
	}

	public boolean compare(long crcVal)
	{
		return true;
	}
}
