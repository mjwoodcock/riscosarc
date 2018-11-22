package riscos.archive.container;

import riscos.archive.InvalidArchiveFile;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.container.CFSFile;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.File;

public class CFSEntry extends ArchiveEntry
{
	private CFSFile cfs_file;
	private long file_length;

	public CFSEntry(CFSFile cfs, RandomAccessInputStream in, String fname, boolean appendFiletype)
	{
		super(in, 0, appendFiletype);
		this.cfs_file = cfs;
		File f = new File(fname);
		this.file_length = f.length();
		String basename = f.toPath().getFileName().toString();
		int idx = basename.indexOf(",d96");
		if (idx == -1) {
			idx = basename.indexOf(".d96");
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

		complen = (int)(this.file_length - seek);
		origlen = this.cfs_file.read32();
		n = this.cfs_file.read32();
		load = this.cfs_file.read32();
		exec = this.cfs_file.read32();

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
