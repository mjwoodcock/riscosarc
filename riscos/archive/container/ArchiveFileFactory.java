package riscos.archive.container;

import riscos.archive.container.ArchiveFile;
import riscos.archive.InvalidArchiveFile;
import java.io.IOException;

public class ArchiveFileFactory
{
	private ArchiveFile archive;

	public ArchiveFileFactory(String filename) throws IOException, InvalidArchiveFile
	{
		this(filename, null);
	}

	public ArchiveFileFactory(String filename, String pass) throws IOException, InvalidArchiveFile
	{
		this(filename, pass, true);
	}

	public ArchiveFileFactory(String filename, String pass, boolean appendFiletype) throws IOException, InvalidArchiveFile
	{
		try
		{
			SparkFile sfs = new SparkFile(filename, pass, appendFiletype);
			sfs.openForRead();
			archive = sfs;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			ArcFSFile afs = new ArcFSFile(filename, pass, appendFiletype);
			afs.openForRead();
			archive = afs;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			PackDirFile pd = new PackDirFile(filename, appendFiletype);
			pd.openForRead();
			archive = pd;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			SquashFile sf = new SquashFile(filename, appendFiletype);
			sf.openForRead();
			archive = sf;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			CFSFile cfs = new CFSFile(filename, appendFiletype);
			cfs.openForRead();
			archive = cfs;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			ZipFileWrapper z = new ZipFileWrapper(filename, null, appendFiletype);
			z.openForRead();
			archive = z;
			return;
		}
		catch (Exception e)
		{
		}

		try
		{
			ArcFile a = new ArcFile(filename, null);
			a.openForRead();
			archive = a;
			return;
		}
		catch (Exception e)
		{
		}

	}

	public ArchiveFile getArchiveFile()
	{
		return archive;
	}
}
