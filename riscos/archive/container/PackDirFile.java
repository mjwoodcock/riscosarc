// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidCompressionType;
import riscos.archive.InvalidPackDirCompressionType;
import riscos.archive.InvalidPackDirFile;
import riscos.archive.LZWConstants;
import riscos.archive.LZWInputStream;
import riscos.archive.LimitInputStream;
import riscos.archive.NullCRC;
import riscos.archive.RandomAccessInputStream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

public class PackDirFile extends ArchiveFile {

  public static final int CT_NOTCOMP = 0x01;
  public static final int CT_LZW = 0x07;

  private RandomAccessInputStream inFile;
  private String archiveFile;
  private int lzwBits;
  private String currentDir;
  private Vector<ArchiveEntry> entryList;
  private int[] dirEndtries;
  private int dirEntriesI;
  private int numFiles;
  private int numDirs;
  private boolean appendFiletype;
  private int options;

  public PackDirFile(String filename) {
    this(filename, true, 0);
  }

  public PackDirFile(String filename, boolean appendFiletype, int options) {
    this.archiveFile = filename;
    this.entryList = new Vector<ArchiveEntry>();
    this.currentDir = "";
    this.dirEndtries = new int[100];
    this.dirEntriesI = -1;
    this.numFiles = 0;
    this.numDirs = 0;
    this.appendFiletype = appendFiletype;
    this.options = options;
  }

  public byte[] getPasswd() {
    return null;
  }

  public int read32() throws IOException {
    int r = 0;

    r = (inFile.read()) & 0xff;
    r |= (inFile.read() & 0xff) << 8;
    r |= (inFile.read() & 0xff) << 16;
    r |= (inFile.read() & 0xff) << 24;

    return r;
  }

  public int read16() throws IOException {
    int r = 0;

    r = (inFile.read()) & 0xff;
    r |= (inFile.read() & 0xff) << 8;

    return r;
  }

  public String readString() throws IOException {
    StringBuffer s = new StringBuffer();
    int r;

    do {
      r = inFile.read();
      if (r == -1) {
        return null;
      } else if (r != 0) {
        s.append((char)r);
      }
    } while (r != 0);

    return s.toString();
  }

  private void readHeader() throws InvalidArchiveFile {
    try {
      String hdr = readString();
      if (hdr.equals("PACK")) {
        lzwBits = read32();
        lzwBits += 12;
      } else {
        throw new InvalidPackDirFile();
      }
    } catch (IOException e) {
      throw new InvalidPackDirFile();
    }
  }

  public void openForRead() throws IOException, InvalidArchiveFile {
    inFile = new RandomAccessInputStream(archiveFile);

    readHeader();

    long offset = inFile.getFilePointer();
    do {
      PackDirEntry pde = new PackDirEntry(this, inFile, lzwBits, this.appendFiletype);
      try {
        pde.readEntry(currentDir, offset);
        if (pde.isDir()) {
          numDirs++;
          if (currentDir.equals("")) {
            currentDir = pde.getName();
          } else {
            currentDir = currentDir + "/" + pde.getName();
          }
          if (dirEntriesI >= 0) {
            --dirEndtries[dirEntriesI];
          }
          dirEndtries[++dirEntriesI] = pde.getNumEntries();
        } else {
          numFiles++;
          if (dirEntriesI >= 0) {
            --dirEndtries[dirEntriesI];
          }
          entryList.add(pde);
        }
        while (dirEntriesI >= 0 && dirEndtries[dirEntriesI] == 0) {
          int i = currentDir.lastIndexOf('/');

          if (i != -1) {
            currentDir = currentDir.substring(0, i);
          } else {
            currentDir = "";
          }
          dirEntriesI--;
        }
        offset = pde.getNextEntryOffset();
      } catch (IOException e) {
        System.err.println(e.toString());
        break;
      }
    } while (dirEndtries[0] > 0);
  }

  public Enumeration<ArchiveEntry> entries() {
    return entryList.elements();
  }

  public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidPackDirFile();
    }

    LimitInputStream lis = new LimitInputStream(inFile, entry.getCompressedLength());

    switch (entry.getCompressType()) {
      case CT_NOTCOMP:
        return lis;
      case CT_LZW:
        return new LZWInputStream(lis, 0, riscos.archive.LZWConstants.PACKDIR, entry.getMaxBits());
      default:
        throw new InvalidPackDirCompressionType();
    }
  }

  public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidPackDirFile();
    }

    return new LimitInputStream(inFile, entry.getCompressedLength());
  }

  public void printInfo() {
    System.out.println("Number of bits: " + lzwBits);
    System.out.println("Number of files: " + numFiles);
    System.out.println("Number of dirs: " + numDirs);
  }

  public CRC getCRCInstance() {
    return new NullCRC();
  }
}
