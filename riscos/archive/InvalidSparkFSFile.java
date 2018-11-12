package riscos.archive;

import java.lang.Exception;
import riscos.archive.InvalidArchiveFile;

public class InvalidSparkFSFile extends InvalidArchiveFile
{
	public InvalidSparkFSFile()
	{
		super("Invalid Spark file");
	}
}
