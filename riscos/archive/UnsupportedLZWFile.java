package riscos.archive;

import java.lang.Exception;
import riscos.archive.LZWConstants;

public class UnsupportedLZWFile extends Exception
{
	public UnsupportedLZWFile()
	{
		super("LZW file type must be in the range 1.." + LZWConstants.MAX_SUPPORTED_TYPE);
	}
}
