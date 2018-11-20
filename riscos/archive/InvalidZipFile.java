package riscos.archive;

import java.lang.Exception;
import riscos.archive.InvalidArchiveFile;

public class InvalidZipFile extends InvalidArchiveFile
{
	public InvalidZipFile()
	{
		super("Invalid Zip file");
	}
}
