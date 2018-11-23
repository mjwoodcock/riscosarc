// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.InvalidArcFile;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.container.ArcFile;
import riscos.archive.container.ArchiveEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class ArcEntry extends ArchiveEntry {

  public static final int SPARKFS_ENDDIR = 0x0;
  public static final int ARCHPACK = 0x80;

  private ArcFile arcFile;

  private long entryOffset;
  private long nextEntryOffset;

  public ArcEntry(ArcFile spark, RandomAccessInputStream in, int datStart) {
    /* MSDOS .arc files do not contain filetype information */
    super(in, datStart, false);
    arcFile = spark;
    isDel = false;
    isEof = false;
  }

  private void readArcEntry(String curDir) throws IOException, InvalidArcFile {
    int r = inFile.read();

    if (r == -1) {
      isEof = true;
      return;
    }

    comptype = r & 0xff;

    if (comptype == 0 || comptype == 0x80) {
      comptype = 0;
      isEof = true;
      return;
    }

    byte[] n = new byte[14];
    n[13] = '\0';
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
      isEof = true;
      comptype = SPARKFS_ENDDIR;
      return;
    }

    /* arc files created on RISC OS do not have their filenames
     * translated to MSDOS format.  Try to work out if the
     * filename should be translated to a valid local name */
    String translatedName;
    if (name.indexOf('/') != -1) {
      translatedName = ArchiveEntry.translateFilename(name);
    } else {
      translatedName = name;
    }

    if (!curDir.equals("")) {
      localFilename = curDir + "/" + translatedName;
    } else {
      localFilename = translatedName;
    }
    complen = arcFile.read32();
    if (complen < 0) {
      throw new InvalidArcFile();
    }
    int date = arcFile.read16();
    int time = arcFile.read16();
    crc = arcFile.read16();
    origlen = arcFile.read32();
    seek = (int)inFile.getFilePointer();
    nextEntryOffset = seek + complen + 1;
    /* FIXME: Calculate the file time */
  }

  public void readEntry(String curDir, long offset) throws IOException, InvalidArcFile {
    inFile.seek(offset);
    entryOffset = inFile.getFilePointer();

    readArcEntry(curDir);
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
    System.out.println("is_dir " + isDir);
  }

  public boolean isEof() {
    return isEof;
  }

  public long getNextEntryOffset() {
    return nextEntryOffset;
  }
}
