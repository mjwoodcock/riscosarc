package riscos.archive;

import riscos.archive.InvalidCompressionType;

public class InvalidArcCompressionType extends InvalidCompressionType
{
	public InvalidArcCompressionType()
	{
		super("Invalid Arc compression type");
	}

	public InvalidArcCompressionType(String msg)
	{
		super("Invalid Arc compression type " + msg);
	}
}
