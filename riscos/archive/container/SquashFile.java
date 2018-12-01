// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidSquashFile;
import riscos.archive.LimitInputStream;
import riscos.archive.NcompressLZWInputStream;
import riscos.archive.NullCRC;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.UnsupportedLZWType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

public class SquashFile extends ArchiveFile {
  private RandomAccessInputStream inFile;
  private String archiveFile;
  private Vector<ArchiveEntry> entryList;
  private boolean appendFiletype;
  private int options;

  public SquashFile(String filename) {
    this(filename, true, 0);
  }

  public SquashFile(String filename, boolean appendFiletype, int options) {
    this.archiveFile = filename;
    this.entryList = new Vector<ArchiveEntry>();
    this.appendFiletype = appendFiletype;
    this.options = options;
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
    return 0xffffffff;
  }

  public String readString(int len) throws IOException {
    StringBuffer s = new StringBuffer();
    int r;

    do {
      r = inFile.read();
      if (r != 0) {
        s.append((char)r);
      }
    } while (len-- > 1 && r != 0);

    return s.toString();
  }

  private void readHeader() throws InvalidArchiveFile {
    try {
      String hdr = readString(4);
      if (!hdr.equals("SQSH")) {
        throw new InvalidSquashFile("Bad magic");
      }
    } catch (IOException e) {
      // throw strop
    }
  }

  public void openForRead() throws IOException, InvalidArchiveFile {
    inFile = new RandomAccessInputStream(archiveFile);

    readHeader();

    long offset = inFile.getFilePointer();

    SquashEntry se = new SquashEntry(this, inFile, archiveFile, this.appendFiletype);
    try {
      se.readEntry(offset);
      entryList.add(se);
    } catch (IOException e) {
      System.err.println(e.toString());
    }
  }

  public Enumeration<ArchiveEntry> entries() {
    return entryList.elements();
  }

  public InputStream getInputStream(ArchiveEntry entry) throws InvalidSquashFile {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidSquashFile("Bad seek");
    }

    LimitInputStream lis = new LimitInputStream(inFile, entry.getCompressedLength());

    try {
      return new NcompressLZWInputStream(lis, 0, riscos.archive.LZWConstants.UNIX_COMPRESS);
    } catch (UnsupportedLZWType e) {
      throw new InvalidSquashFile(e.toString());
    }
  }

  public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidSquashFile {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidSquashFile("Bad seek");
    }

    return new LimitInputStream(inFile, entry.getCompressedLength());
  }

  public void printInfo() {
  }

  public byte[] getPasswd() {
    return null;
  }

  public CRC getCRCInstance() {
    return new NullCRC();
  }
}
