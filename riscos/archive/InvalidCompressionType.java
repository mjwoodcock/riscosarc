package riscos.archive;

import java.lang.Exception;

public abstract class InvalidCompressionType extends Exception
{
	public InvalidCompressionType(String msg)
	{
		super(msg);
	}
}
