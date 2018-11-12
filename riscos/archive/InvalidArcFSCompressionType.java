package riscos.archive;

import java.lang.Exception;

public class InvalidArcFSCompressionType extends Exception
{
	public InvalidArcFSCompressionType()
	{
		super("Invalid ArcFS compression type");
	}
}
