package riscos.archive.container;

import riscos.archive.container.ArchiveFile;
import riscos.archive.InvalidArchiveFile;
import java.io.IOException;

public class ArchiveFileFactory
{
	private ArchiveFile archive;

	public ArchiveFileFactory(String filename, String pass) throws IOException, InvalidArchiveFile
	{
		try
		{
			SparkFile sfs = new SparkFile(filename, pass);
			sfs.openForRead();
			archive = sfs;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			ArcFSFile afs = new ArcFSFile(filename, pass);
			afs.openForRead();
			archive = afs;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			PackDirFile pd = new PackDirFile(filename);
			pd.openForRead();
			archive = pd;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			SquashFile sf = new SquashFile(filename);
			sf.openForRead();
			archive = sf;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			CFSFile cfs = new CFSFile(filename);
			cfs.openForRead();
			archive = cfs;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			ZipFileWrapper z = new ZipFileWrapper(filename, null);
			z.openForRead();
			archive = z;
			return;
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
