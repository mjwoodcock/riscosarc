// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.NcompressLZWInputStream;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.container.ArcFSFile;
import riscos.archive.container.ArchiveEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class ArcFSEntry extends ArchiveEntry {

  public static final int ARCFS_ENDDIR = 0x0;
  public static final int ARCFS_DELETED = 0x1;
  public static final int ARCHPACK = 0x80;

  private ArcFSFile arcFile;

  private long entryOffset;
  private long nextEntryOffset;

  public ArcFSEntry(ArcFSFile arc, RandomAccessInputStream in, int datStart, boolean appendFiletype) {
    super(in, datStart, appendFiletype);
    this.arcFile = arc;
    this.isDel = false;
    this.isEof = false;
  }

  private void readArcfsEntry(String curDir) throws IOException {
    byte[] n = new byte[12];
    n[11] = 0;
    int r = inFile.read();
    if (r == -1) {
      isEof = true;
    }
    comptype = r & 0xff;
    if (comptype == 0x1) {
      isDel = true;
    }
    inFile.read(n, 0, n.length - 1);
    int nul;
    for (nul = 0; nul < n.length; nul++) {
      if (n[nul] == 0) {
        break;
      }
    }
    name = new String(n, 0, nul);
    if (!curDir.equals("")) {
      localFilename = curDir + "/" + ArchiveEntry.translateFilename(name);
    } else {
      localFilename = ArchiveEntry.translateFilename(name);
    }
    origlen = arcFile.read32();
    load = arcFile.read32();
    exec = arcFile.read32();
    int a = arcFile.read32();
    crc = (a >> 16) & 0xffff;
    attr = a & 0xff;
    maxbits = ((a & 0xff00) >> 8) & 0xff;
    complen = arcFile.read32();
    int infoWord = arcFile.read32();
    seek = (infoWord & 0x7fffffff) + dataStart;
    if (((infoWord >> 31) & 0x1) != 0) {
      isDir = true;
    } else {
      isDir = false;
    }
    appendFiletype();
    calculateFileTime();
    nextEntryOffset = inFile.getFilePointer();
  }

  public void readEntry(String curDir, long offset) throws IOException {
    inFile.seek(offset);
    entryOffset = inFile.getFilePointer();

    readArcfsEntry(curDir);
  }

  public void printEntryData() {
    System.out.println("Comptype = " + comptype);
    System.out.println("Name " + name);
    System.out.println("Local name " + localFilename);
    System.out.println("Origlen " + origlen);
    System.out.println("Load " + load);
    System.out.println("Exec " + exec);
    System.out.println("CRC " + crc);
    System.out.println("attr " + attr);
    System.out.println("maxbits " + maxbits);
    System.out.println("complen " + complen);
    System.out.println("seek " + seek);
    System.out.println("isDir " + isDir);
  }

  public boolean isEof() {
    return isEof;
  }

  public long getNextEntryOffset() {
    return nextEntryOffset;
  }
}
