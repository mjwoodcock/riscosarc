// vim:ts=2:sw=2:expandtab:ai

/* MSDOS .arc file container.
 * There appear to be an entire 3 of these on arcade-bbs.net */

package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.GarbleInputStream;
import riscos.archive.InvalidArcCompressionType;
import riscos.archive.InvalidArcFile;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidCompressionType;
import riscos.archive.LimitInputStream;
import riscos.archive.NcompressLZWInputStream;
import riscos.archive.PackInputStream;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.SparkCRC;
import riscos.archive.UnsupportedLZWType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

public class ArcFile extends ArchiveFile {

  public static final int CT_NOTCOMP = 0x01;
  public static final int CT_NOTCOMP2 = 0x02;
  public static final int CT_PACK = 0x03;
  public static final int CT_PACKSQUEEZE = 0x04;
  public static final int CT_LZOLD = 0x05;
  public static final int CT_LZNEW = 0x06;
  public static final int CT_LZW = 0x07;
  public static final int CT_CRUNCH = 0x08;
  public static final int CT_SQUASH = 0x09;
  public static final int CT_COMP = 0x7f;

  private static final byte SPARKFS_STARTBYTE = 0x1a;
  private RandomAccessInputStream inFile;
  private String archiveFile;
  private int headerLength;
  private int dataStart;
  private String currentDir;
  private Vector<ArchiveEntry> entryList;
  private byte[] passwd;

  public ArcFile(String filename, String pass) {
    archiveFile = filename;
    entryList = new Vector<ArchiveEntry>();
    currentDir = "";
    if (pass != null) {
      passwd = pass.getBytes();
    }
  }

  public byte[] getPasswd() {
    return passwd;
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

  private void readHeader() throws InvalidArchiveFile {
    try {
      byte b1 = (byte)inFile.read();
      byte b2 = (byte)inFile.read();

      /* If b2 high bit is not set, then this is a DOS
       * format .arc file */
      if (b1 != SPARKFS_STARTBYTE || ((b2 & 0x80) != 0x0) ) {
        throw new InvalidArcFile();
      }
    } catch (IOException e) {
      throw new InvalidArcFile();
    }
  }

  public void openForRead() throws IOException, InvalidArchiveFile {
    inFile = new RandomAccessInputStream(archiveFile);

    readHeader();

    long offset = 1;
    do {
      ArcEntry fse = new ArcEntry(this, inFile, dataStart);
      try {
        fse.readEntry(currentDir, offset);
        if (fse.isEof()) {
          break;
        } else {
          entryList.add(fse);
        }
        offset = fse.getNextEntryOffset();
      } catch (Exception e) {
        System.err.println(e.toString());
        break;
      }
    } while (true);
  }

  public Enumeration<ArchiveEntry> entries() {
    return entryList.elements();
  }

  public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidArcFile();
    }

    LimitInputStream lis = new LimitInputStream(inFile, entry.getCompressedLength());
    GarbleInputStream gis = new GarbleInputStream(lis, passwd);

    try {
      switch (entry.getCompressType()) {
        case CT_NOTCOMP:
        case CT_NOTCOMP2:
          return gis;
        case CT_COMP:
          return new NcompressLZWInputStream(gis, 0, riscos.archive.LZWConstants.COMPRESS);
        case CT_PACK:
          return new PackInputStream(gis);
        case CT_CRUNCH:
          return new PackInputStream(new NcompressLZWInputStream(gis, 0, riscos.archive.LZWConstants.CRUNCH));
        case CT_SQUASH:
          return new NcompressLZWInputStream(gis, 0, riscos.archive.LZWConstants.SQUASH);
        default:
          throw new InvalidArcCompressionType();
      }
    } catch (UnsupportedLZWType e) {
      throw new InvalidArcCompressionType(e.toString());
    }
  }

  public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidArcFile();
    }

    return new LimitInputStream(inFile, entry.getCompressedLength());
  }

  public void printArcInfo() {
    System.out.println("Header length = " + headerLength);
    System.out.println("Data start = " + dataStart);
  }

  public CRC getCRCInstance() {
    return new SparkCRC();
  }
}
