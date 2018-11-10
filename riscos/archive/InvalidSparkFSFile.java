package riscos.archive;

import java.lang.Exception;

public class InvalidSparkFSFile extends Exception
{
	public InvalidSparkFSFile()
	{
		super("Invalid Spark file");
	}
}
