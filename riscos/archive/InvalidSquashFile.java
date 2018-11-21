package riscos.archive;

public class InvalidSquashFile extends InvalidArchiveFile
{
	public InvalidSquashFile(String msg)
	{
		super("Invalid Squash file " + msg);
	}
}
