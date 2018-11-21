import riscos.archive.*;
import riscos.archive.CRC;
import riscos.archive.container.ArchiveFile;
import riscos.archive.container.ArchiveFileFactory;
import riscos.archive.container.ArchiveEntry;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;

public class riscosarc
{
	public static void usage()
	{
		System.err.println("Usage: java riscosarc <command> <argument> <archive_file> [filename]");
		System.err.println("  command can be:");
		System.err.println("  -l: list contents of file");
		System.err.println("  -x: extract file");
		System.err.println("  argument can be:");
		System.err.println("  -c: extract console");
		System.err.println("  -d<path>: extract files to <path>");
		System.err.println("  -g<password>: set password to <password>");
		System.err.println("  -r: extract raw compressed data");
		System.err.println("  -v: verbose list contents of file");
		System.exit(1);
	}

	public static void main(String args[])
	{
		boolean do_list = false;
		boolean do_verbose = false;
		boolean do_extract = false;
		boolean extract_raw = false;
		boolean console_output = false;
		String output_directory = ".";
		String password = null;
		String suffix = "";
		boolean error = false;
		int archive_file_arg = -1;
		String file_to_extract = null;

		if (args.length < 2)
		{
			riscosarc.usage();
		}

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].startsWith("-c"))
			{
				console_output = true;
			}
			else if (args[i].startsWith("-d"))
			{
				output_directory = args[i].substring(2);
			}
			else if (args[i].startsWith("-g"))
			{
				password = args[i].substring(2);
			}
			else if (args[i].equals("-l"))
			{
				do_list = true;
			}
			else if (args[i].equals("-r"))
			{
				extract_raw = true;
				suffix = ".raw";
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
			else
			{
				archive_file_arg = i;
				break;
			}
		}

		if (!(do_extract || do_list) || archive_file_arg == -1) {
				riscosarc.usage();
		}

		if (archive_file_arg < args.length - 1) {
			file_to_extract = args[archive_file_arg + 1];
		}

		Enumeration<ArchiveEntry> ent = null;
		ArchiveFileFactory aff;
		ArchiveFile af;
		try {
			aff = new ArchiveFileFactory(args[archive_file_arg], password);
			af = aff.getArchiveFile();
			ent = af.entries();
		} catch (Exception e) {
			error = true;
			af = null;
		}

		if (ent == null) {
			System.err.println("Invalid archive file");
			System.exit(1);
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

				if (file_to_extract != null && !file_to_extract.equals(entry.getLocalFilename())) {
					continue;
				}

				if (do_extract)
				{
					CRC crc = af.getCRCInstance();
					crc.setDataLength(entry.getUncompressedLength());
					entry.cleanOldFile();
					entry.mkDir(output_directory);
					try {
						InputStream fis = null;
						if (extract_raw) {
							fis = af.getRawInputStream(entry);
						} else {
							fis = af.getInputStream(entry);
						}
						if (fis == null) {
							System.err.println("Can not get input stream");
							System.exit(1);
						}

						FileOutputStream fos = null;
						LimitOutputStream los = null;

						if (console_output) {
							los = new LimitOutputStream(System.out, entry.getUncompressedLength());
						} else {
							fos = new FileOutputStream(output_directory + File.separator + entry.getLocalFilename() + suffix);
							los = new LimitOutputStream(fos, entry.getUncompressedLength());
						}

						int r = 0;
						byte buf[] = new byte[1024];
						do
						{
							r = fis.read(buf, 0, buf.length);
							if (r != -1)
							{
								crc.update(buf, 0, r);
								los.write(buf, 0, r);
							}
						} while (r != -1);

						if (!console_output) {
							if (los != null) {
								los.close();
							}
							if (fos != null) {
								fos.close();
							}

							entry.setFileTime();
						}

						if (!extract_raw && !crc.compare(entry.getCrcValue())) {
							System.out.println("CRC check failed");
							error = true;
						}
					} catch (Exception e) {
						System.out.println(e.toString());
						e.printStackTrace(System.out);
						error = true;
					}
				}
			}
		}
		catch (Exception e)
		{
			System.err.println(e.toString());
			e.printStackTrace(System.out);
			error = true;
		}

		if (error) {
			System.exit(1);
		}
	}
}
