package riscos.archive;

import java.lang.Exception;

public class CorruptLZWData extends Exception
{
	public CorruptLZWData(String err)
	{
		super("LZW data is corrupt: " + err);
	}
}
