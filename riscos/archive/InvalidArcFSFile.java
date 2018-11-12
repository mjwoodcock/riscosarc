package riscos.archive;

import java.lang.Exception;

public class InvalidArcFSFile extends Exception
{
	public InvalidArcFSFile()
	{
		super("Invalid ArcFS file");
	}
}
