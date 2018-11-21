package riscos.archive;

public class InvalidArcFSCompressionType extends InvalidCompressionType
{
	public InvalidArcFSCompressionType()
	{
		super("Invalid ArcFS compression type");
	}

	public InvalidArcFSCompressionType(String msg)
	{
		super("Invalid ArcFS compression type: " + msg);
	}
}
