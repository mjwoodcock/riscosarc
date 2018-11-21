package riscos.archive;

import riscos.archive.InvalidCompressionType;

public class InvalidSparkCompressionType extends InvalidCompressionType
{
	public InvalidSparkCompressionType()
	{
		super("Invalid Spark compression type");
	}

	public InvalidSparkCompressionType(String msg)
	{
		super("Invalid Spark compression type " + msg);
	}
}
