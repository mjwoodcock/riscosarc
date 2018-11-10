import riscos.archive.*;
import riscos.archive.container.SparkFSFile;
import riscos.archive.container.PackDirFile;
import riscos.archive.container.SquashFile;
import riscos.archive.container.ArchiveEntry;
import java.io.FilterInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;

public class riscosarc
{
	public static void usage()
	{
		System.err.println("Usage: java riscosarc <argument> <file>");
		System.err.println("  argument can be:");
		System.err.println("  -l: list contents of file");
		System.err.println("  -lv: verbose list contents of file");
		System.err.println("  -x: extract file");
		System.exit(1);
	}

	public static void main(String args[])
	{
		boolean do_list = false;
		boolean do_verbose = false;
		boolean do_extract = false;

		if (args.length < 2)
		{
			riscosarc.usage();
		}

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-l"))
			{
				do_list = true;
			}
			else if (args[i].equals("-v"))
			{
				do_verbose = true;
			}
			else if (args[i].equals("-x"))
			{
				do_extract = true;
			}
			else if (args[i].charAt(0) == '-')
			{
				riscosarc.usage();
			}
		}

		Enumeration<ArchiveEntry> ent = null;
		PackDirFile packdir = null;
		SparkFSFile sparkfs = null;
		SquashFile squash = null;
		try {
			packdir = new PackDirFile(args[args.length - 1]);
			packdir.openForRead();
			ent = packdir.entries();
		} catch (Exception e) {
			packdir = null;
		}

		if (ent == null) {
			sparkfs = new SparkFSFile(args[args.length - 1], null);
			try {
				sparkfs.openForRead();
				ent = sparkfs.entries();
			} catch (Exception e) {
				sparkfs = null;
			}
		}

		if (ent == null) {
			squash = new SquashFile(args[args.length - 1]);
			try {
				squash.openForRead();
				ent = squash.entries();
			} catch (Exception e) {
				squash = null;
			}
		}

		if (ent == null) {
			System.err.println("Invalid archive file");
		}

		try
		{
			while (ent.hasMoreElements())
			{
				ArchiveEntry entry = ent.nextElement();
				if (do_list)
				{
					System.out.println(entry.getLocalFilename());
				}
				else if (do_verbose)
				{
					entry.printEntryData();
				}

				if (do_extract)
				{
					entry.cleanOldFile();
					entry.mkDir();
					try {
						FilterInputStream fis = null;
						if (sparkfs != null) {
							fis = (FilterInputStream)sparkfs.getInputStream(entry);
						} else if (packdir != null) {
							fis = (FilterInputStream)packdir.getInputStream(entry);
						} else if (squash != null) {
							fis = (FilterInputStream)squash.getInputStream(entry);
						}
						FileOutputStream fos = new FileOutputStream(entry.getLocalFilename());
						LimitOutputStream los = new LimitOutputStream(fos, entry.getUncompressedLength());
						int r = 0;
						byte buf[] = new byte[1024];
						do
						{
							r = fis.read(buf, 0, buf.length);
							if (r != -1)
							{
								los.write(buf, 0, r);
							}
						} while (r != -1);
						los.close();
						fos.close();
						entry.setFileTime();
					} catch (Exception e) {
						System.out.println(e.toString());
						e.printStackTrace(System.out);
					}
				}
			}
		}
		catch (Exception e)
		{
			System.err.println(e.toString());
		}
	}
}