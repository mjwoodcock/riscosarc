package riscos.archive;

import java.lang.Exception;

public class InvalidPackDirCompressionType extends Exception
{
	public InvalidPackDirCompressionType()
	{
		super("Invalid PackDir compression type");
	}
}
