// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.GarbleInputStream;
import riscos.archive.HuffInputStream;
import riscos.archive.InvalidSparkFile;
import riscos.archive.LimitInputStream;
import riscos.archive.NcompressLZWInputStream;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.container.ArchiveEntry;
import riscos.archive.container.SparkFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class SparkEntry extends ArchiveEntry {
  public static final int SPARKFS_ENDDIR = 0x0;
  public static final int ARCHPACK = 0x80;

  private SparkFile sparkFile;

  private long entryOffset;
  private long nextEntyOffset;

  public SparkEntry(SparkFile spark, RandomAccessInputStream in, int datStart, boolean appendFiletype) {
    super(in, datStart, appendFiletype);
    sparkFile = spark;
    isDel = false;
    isEof = false;
  }

  private void readSparkEntry(String curDir) throws IOException, InvalidSparkFile {
    int r = inFile.read();

    if (r == -1) {
      isEof = true;
      return;
    }

    comptype = r & 0xff;
    if ((comptype & ~ARCHPACK) == 0) {
      comptype = SPARKFS_ENDDIR;
      return;
    }

    byte[] n = new byte[14];
    n[12] = '\0';
    inFile.read(n, 0, n.length - 1);
    int nul;
    for (nul = 0; nul < n.length; nul++) {
      if (n[nul] < ' ' || n[nul] > '~') {
        n[nul] = '\0';
        break;
      }
    }

    name = new String(n, 0, nul);
    if (name.length() == 0) {
      /* Some old Spark files have some extra empty
       * data on the end. */
      isEof = true;
      comptype = SPARKFS_ENDDIR;
      return;
    }

    if (!curDir.equals("")) {
      localFilename = curDir + "/" + ArchiveEntry.translateFilename(name);
    } else {
      localFilename = ArchiveEntry.translateFilename(name);
    }
    complen = sparkFile.read32();
    if (complen < 0) {
      throw new InvalidSparkFile();
    }
    int date = sparkFile.read16();
    int time = sparkFile.read16();
    crc = sparkFile.read16();
    if ((comptype & ~ARCHPACK) > SparkFile.CT_NOTCOMP) {
      origlen = sparkFile.read32();
    } else {
      origlen = complen;
    }
    if ((comptype & ARCHPACK) == ARCHPACK) {
      load = sparkFile.read32();
      exec = sparkFile.read32();
      attr = sparkFile.read32();
    }
    comptype &= ~ARCHPACK;
    seek = (int)inFile.getFilePointer();
    if ((load & 0xffffff00) == 0xfffddc00) {
      isDir = true;
    }
    if (isDir) {
      nextEntyOffset = seek + 1;
    } else {
      nextEntyOffset = seek + complen + 1;
    }
    calculateFileTime();
    appendFiletype();
  }

  public void readEntry(String curDir, long offset) throws IOException, InvalidSparkFile {
    inFile.seek(offset);
    entryOffset = inFile.getFilePointer();

    readSparkEntry(curDir);
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
    return nextEntyOffset;
  }
}
