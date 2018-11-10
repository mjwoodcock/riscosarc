package riscos.archive;

import java.lang.Exception;

public class InvalidSparkCompressionType extends Exception
{
	public InvalidSparkCompressionType()
	{
		super("Invalid Spark compression type");
	}
}
