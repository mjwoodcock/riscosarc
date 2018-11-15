package riscos.archive.container;

import riscos.archive.container.ArchiveFile;
import riscos.archive.container.SparkFSFile;
import riscos.archive.InvalidArchiveFile;
import java.io.IOException;

public class ArchiveFileFactory
{
	private ArchiveFile archive;

	public ArchiveFileFactory(String filename, String pass) throws IOException, InvalidArchiveFile
	{
		try
		{
			SparkFSFile sfs = new SparkFSFile(filename, pass);
			sfs.openForRead();
			archive = sfs;
		}
		catch (Exception e)
		{
		}

		try
		{
			ArcFSFile afs = new ArcFSFile(filename, pass);
			afs.openForRead();
			archive = afs;
		}
		catch (Exception e)
		{
		}

		try
		{
			PackDirFile pd = new PackDirFile(filename);
			pd.openForRead();
			archive = pd;
		}
		catch (Exception e)
		{
		}

		try
		{
			SquashFile sf = new SquashFile(filename);
			sf.openForRead();
			archive = sf;
		}
		catch (Exception e)
		{
		}
	}

	public ArchiveFileFactory(String filename) throws IOException, InvalidArchiveFile
	{
		this(filename, null);
	}

	public ArchiveFile getArchiveFile()
	{
		return archive;
	}
}
