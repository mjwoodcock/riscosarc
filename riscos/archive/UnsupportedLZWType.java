package riscos.archive;

import riscos.archive.LZWConstants;

public class UnsupportedLZWType extends Exception
{
	public UnsupportedLZWType()
	{
		super("LZW type not supported by this class");
	}
}
