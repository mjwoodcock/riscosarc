// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.NcompressLZWInputStream;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.container.SquashFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class SquashEntry extends ArchiveEntry {
  private SquashFile squashFile;
  private long fileLength;

  public SquashEntry(SquashFile squash, RandomAccessInputStream in, String fname, boolean appendFiletype) {
    super(in, 0, appendFiletype);
    this.squashFile = squash;
    File f = new File(fname);
    this.fileLength = f.length();
    String basename = f.toPath().getFileName().toString();
    int idx = basename.indexOf(",fca");
    if (idx == -1) {
      idx = basename.indexOf(".fca");
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
    inFile.seek(offset);

    complen = (int)(fileLength - seek);
    origlen = squashFile.read32();
    load = squashFile.read32();
    exec = squashFile.read32();
    squashFile.read32();

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
