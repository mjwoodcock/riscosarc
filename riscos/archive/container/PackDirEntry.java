// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.LZWInputStream;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.container.PackDirFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class PackDirEntry extends ArchiveEntry {

  public static final int ENTRY_TYPE_FILE = 0;
  public static final int ENTRY_TYPE_DIR = 1;

  public static final long ROOT_DIR_OFFSET = 9;

  private PackDirFile packdirFile;
  private int numEntries;

  private long entryOffset;
  private long nextEntryOffset;

  public PackDirEntry(PackDirFile packdir, RandomAccessInputStream in, int lzwBits, boolean appendFiletype) {
    super(in, 0, appendFiletype);
    this.packdirFile = packdir;
    this.maxbits = lzwBits;
  }

  public void readEntry(String curDir, long offset) throws IOException {
    int n;
    int type;
    boolean root = false;

    if (offset == ROOT_DIR_OFFSET) {
      root = true;
    }
    inFile.seek(offset);
    entryOffset = inFile.getFilePointer();
    name = packdirFile.readString();
    if (name == null) {
      throw new IOException();
    }
    if (name != "") {
      if (root) {
        int i = name.lastIndexOf('.');
        if (i == -1) {
          i = name.lastIndexOf(':');
        }
        if (i != -1) {
          name = name.substring(i + 1);
        }
      }
      load = packdirFile.read32();
      exec = packdirFile.read32();
      n = packdirFile.read32();
      attr = packdirFile.read32();
      if (root) {
        type = ENTRY_TYPE_DIR;
      } else {
        type = packdirFile.read32();
      }
      if (type == ENTRY_TYPE_DIR) {
        isDir = true;
        numEntries = n;
      } else {
        isDir = false;
        origlen = n;
        complen = packdirFile.read32();
        if (complen == -1) {
          comptype = PackDirFile.CT_NOTCOMP;
          complen = origlen;
        } else {
          comptype = PackDirFile.CT_LZW;
        }
      }

      if (!curDir.equals("")) {
        localFilename = curDir + "/" + name.replace('/', '.');
      } else {
        localFilename = name;
      }
      seek = inFile.getFilePointer();
      calculateFileTime();
      if (isDir) {
        nextEntryOffset = seek;
      } else {
        appendFiletype();
        nextEntryOffset = seek + (complen == -1 ? origlen : complen);
      }
    }
  }

  public void printEntryData() {
    System.out.println("Comptype = " + comptype);
    System.out.println("Name " + name);
    System.out.println("Local name " + localFilename);
    if (isDir) {
      System.out.println("Num entries " + numEntries);
    } else {
      System.out.println("Complen " + complen);
      System.out.println("Origlen " + origlen);
    }
    System.out.println("Load " + Integer.toHexString(load));
    System.out.println("Exec " + Integer.toHexString(exec));
    System.out.println("attr " + Integer.toHexString(attr));
    System.out.println("maxbits " + maxbits);
    System.out.println("seek " + seek);
    System.out.println("isDir " + isDir);
  }

  public long getNextEntryOffset() {
    return nextEntryOffset;
  }

  public int getNumEntries() {
    return numEntries;
  }
}
