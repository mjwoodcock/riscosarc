// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.CFSInputStream;
import riscos.archive.CRC;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidCFSFile;
import riscos.archive.LimitInputStream;
import riscos.archive.NullCRC;
import riscos.archive.RandomAccessInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

public class CFSFile extends ArchiveFile {

  private RandomAccessInputStream inFile;
  private String archiveFile;
  private Vector<ArchiveEntry> entryList;
  private boolean appendFiletype;
  private int options;

  public CFSFile(String filename) {
    this(filename, true, 0);
  }

  public CFSFile(String filename, boolean appendFiletype, int options) {
    this.archiveFile = filename;
    this.entryList = new Vector<ArchiveEntry>();
    this.appendFiletype = appendFiletype;
    this.options = options;
  }

  public int read32() throws IOException {
    int r = 0;

    r = (this.inFile.read()) & 0xff;
    r |= (this.inFile.read() & 0xff) << 8;
    r |= (this.inFile.read() & 0xff) << 16;
    r |= (this.inFile.read() & 0xff) << 24;

    return r;
  }

  public int read16() throws IOException {
    return 0xffffffff;
  }

  private void readHeader() throws InvalidArchiveFile {
    try {
      read32();
      int magic = read32();
      if (magic != 0x303) {
        throw new InvalidCFSFile();
      }
    } catch (IOException e) {
      // throw strop
    }
  }

  public void openForRead() throws IOException, InvalidArchiveFile {
    this.inFile = new RandomAccessInputStream(this.archiveFile);

    readHeader();

    CFSEntry se = new CFSEntry(this, this.inFile, this.archiveFile, this.appendFiletype);
    try {
      se.readEntry(0);
      this.entryList.add(se);
    } catch (IOException e) {
      System.err.println(e.toString());
    }
  }

  public Enumeration<ArchiveEntry> entries() {
    return this.entryList.elements();
  }

  public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile {
    try {
      this.inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidCFSFile();
    }

    LimitInputStream lis = new LimitInputStream(this.inFile, entry.getCompressedLength());

    return new CFSInputStream(lis);
  }

  public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile {
    try {
      this.inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidCFSFile();
    }

    return new LimitInputStream(this.inFile, entry.getCompressedLength());
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
