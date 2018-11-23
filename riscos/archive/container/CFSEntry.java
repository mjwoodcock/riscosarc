// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.InvalidArchiveFile;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.container.CFSFile;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;

public class CFSEntry extends ArchiveEntry {

  private CFSFile cfsFile;
  private long fileLength;

  public CFSEntry(CFSFile cfs, RandomAccessInputStream in, String fname, boolean appendFiletype) {
    super(in, 0, appendFiletype);
    this.cfsFile = cfs;
    File f = new File(fname);
    this.fileLength = f.length();
    String basename = f.toPath().getFileName().toString();
    int idx = basename.indexOf(",d96");
    if (idx == -1) {
      idx = basename.indexOf(".d96");
    }
    if (idx == basename.length() - 4) {
      localFilename = basename.substring(0, idx);
      name = localFilename;
    } else {
      localFilename = basename;
      name = localFilename;
    }
  }

  public void readEntry(long offset) throws IOException {
    int type;
    long end;

    inFile.seek(offset);

    complen = (int)(this.fileLength - seek);
    origlen = this.cfsFile.read32();
    this.cfsFile.read32();
    load = this.cfsFile.read32();
    exec = this.cfsFile.read32();

    seek = inFile.getFilePointer();
    calculateFileTime();
    appendFiletype();
  }

  public void printEntryData() {
    System.out.println("Local name " + localFilename);
    System.out.println("Complen " + complen);
    System.out.println("Origlen " + origlen);
    System.out.println("Load " + Integer.toHexString(load));
    System.out.println("Exec " + Integer.toHexString(exec));
    System.out.println("attr " + Integer.toHexString(attr));
    System.out.println("seek " + seek);
  }
}
