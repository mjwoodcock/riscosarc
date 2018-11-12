import riscos.archive.*;
import riscos.archive.container.ArchiveFile;
import riscos.archive.container.ArchiveFileFactory;
import riscos.archive.container.ArchiveEntry;
import java.io.File;
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
		System.err.println("  -v: verbose list contents of file");
		System.err.println("  -x: extract file");
		System.exit(1);
	}

	public static void main(String args[])
	{
		boolean do_list = false;
		boolean do_verbose = false;
		boolean do_extract = false;
		String output_directory = ".";

		if (args.length < 2)
		{
			riscosarc.usage();
		}

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].startsWith("-d"))
			{
				output_directory = args[i].substring(2);
			}
			else if (args[i].equals("-l"))
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
		ArchiveFileFactory aff;
		ArchiveFile af;
		try {
			aff = new ArchiveFileFactory(args[args.length - 1]);
			af = aff.getArchiveFile();
			ent = af.entries();
		} catch (Exception e) {
			af = null;
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
					entry.mkDir(output_directory);
					try {
						FilterInputStream fis = null;
						if (af != null) {
							fis = (FilterInputStream)af.getInputStream(entry);
						}
						FileOutputStream fos = new FileOutputStream(output_directory + File.separator + entry.getLocalFilename());
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
