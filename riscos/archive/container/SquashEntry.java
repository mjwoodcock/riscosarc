package riscos.archive.container;

import riscos.archive.*;
import riscos.archive.container.SquashFile;
import java.io.FilterInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

public class SquashEntry extends ArchiveEntry
{
	private SquashFile squash_file;
	private long file_length;

	public SquashEntry(SquashFile squash, RandomAccessInputStream in, String fname, boolean appendFiletype)
	{
		super(in, 0, appendFiletype);
		this.squash_file = squash;
		File f = new File(fname);
		this.file_length = f.length();
		String basename = f.toPath().getFileName().toString();
		int idx = basename.indexOf(",fca");
		if (idx == -1) {
			idx = basename.indexOf(".fca");
		}
		if (idx == basename.length() - 4) {
			local_filename = basename.substring(0, idx);
			name = local_filename;
		} else {
			local_filename = basename;
			name = local_filename;
		}
	}

	public void readEntry(long offset) throws IOException
	{
		int n;
		int type;
		long end;

		in_file.seek(offset);

		complen = (int)(file_length - seek);
		origlen = squash_file.read32();
		load = squash_file.read32();
		exec = squash_file.read32();
		n = squash_file.read32();

		seek = in_file.getFilePointer();
		calculateFileTime();
		appendFiletype();
	}

	public void printEntryData()
	{
		System.out.println("Local name " + local_filename);
		System.out.println("Complen " + complen);
		System.out.println("Origlen " + origlen);
		System.out.println("Load " + Integer.toHexString(load));
		System.out.println("Exec " + Integer.toHexString(exec));
		System.out.println("attr " + Integer.toHexString(attr));
		System.out.println("seek " + seek);
	}
}
