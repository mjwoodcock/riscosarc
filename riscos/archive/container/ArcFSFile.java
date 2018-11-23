// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.GarbleInputStream;
import riscos.archive.InvalidArcFSCompressionType;
import riscos.archive.InvalidArcFSFile;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidCompressionType;
import riscos.archive.LimitInputStream;
import riscos.archive.NcompressLZWInputStream;
import riscos.archive.NullCRC;
import riscos.archive.PackInputStream;
import riscos.archive.RandomAccessInputStream;
import riscos.archive.UnsupportedLZWType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

public class ArcFSFile extends ArchiveFile {

  public static final int ARCFS_STORE = 0x82;
  public static final int ARCFS_PACK = 0x83;
  public static final int ARCFS_CRUNCH = 0x88;
  public static final int ARCFS_COMPRESS = 0xff;

  private static final byte ARCFS_HEADER_SIZE = 36;
  private RandomAccessInputStream inFile;
  private String archiveFile;
  private int headerLength;
  private int dataStart;
  private int version;
  private int rwVersion;
  private int arcFormat;
  private String currentDir;
  private Vector<ArchiveEntry> entryList;
  private byte[] passwd;
  private boolean appendFiletype;

  public ArcFSFile(String filename, String pass) {
    this(filename, pass, true);
  }

  public ArcFSFile(String filename, String pass, boolean appendFiletype) {
    this.archiveFile = filename;
    this.entryList = new Vector<ArchiveEntry>();
    this.currentDir = "";
    this.appendFiletype = appendFiletype;
    if (pass != null) {
      this.passwd = pass.getBytes();
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
      byte b;

      b = (byte)inFile.read();
      if (b == 'A') {
        byte[] h = new byte[7];
        inFile.read(h);
        if (!(h[0] == 'r'
            && h[1] == 'c'
            && h[2] == 'h'
            && h[3] == 'i'
            && h[4] == 'v'
            && h[5] == 'e'
            && h[6] == 0)) {
          throw new InvalidArcFSFile();
        }
      } else {
        throw new InvalidArcFSFile();
      }
    } catch (IOException e) {
      throw new InvalidArcFSFile();
    }
  }

  private void readArcfsHeader() throws IOException, InvalidArchiveFile {
    headerLength = read32();
    dataStart = read32();
    version = read32();
    if (version > 40) {
      throw new InvalidArcFSFile();
    }
    rwVersion = read32();
    arcFormat = read32();
    for (int i = 0; i < 17; i++) {
      read32(); // reserved
    }

  }

  public void openForRead() throws IOException, InvalidArchiveFile {
    inFile = new RandomAccessInputStream(archiveFile);

    readHeader();
    readArcfsHeader();

    long offset = inFile.getFilePointer();
    int numEntries = headerLength / 36;
    for (int i = 0; i < numEntries; i++) {
      ArcFSEntry fse = new ArcFSEntry(this, inFile, dataStart, this.appendFiletype);
      try {
        fse.readEntry(currentDir, offset);
        if (fse.isEof()) {
          break;
        }
        if (fse.getCompressType() == ArcFSEntry.ARCFS_ENDDIR) {
          offset += 2;
          int idx = currentDir.lastIndexOf('/');
          if (idx > -1) {
            currentDir = currentDir.substring(0, idx);
          } else {
            currentDir = "";
          }
        } else if (fse.getCompressType() != ArcFSEntry.ARCFS_DELETED) {
          if (fse.isDir()) {
            if (currentDir != "") {
              currentDir = currentDir + "/" + fse.getName();
            } else {
              currentDir = fse.getName();
            }
          } else {
            entryList.add(fse);
          }
        }
        offset = fse.getNextEntryOffset();
      } catch (IOException e) {
        System.err.println(e.toString());
      }
    }
  }

  public Enumeration<ArchiveEntry> entries() {
    return entryList.elements();
  }

  public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidArcFSFile();
    }

    LimitInputStream lis = new LimitInputStream(inFile, entry.getCompressedLength());
    GarbleInputStream gis = new GarbleInputStream(lis, passwd);

    try {
      switch (entry.getCompressType()) {
        case ARCFS_STORE:
          return gis;
        case ARCFS_COMPRESS:
          return new NcompressLZWInputStream(gis, 0, riscos.archive.LZWConstants.COMPRESS, entry.getMaxBits());
        case ARCFS_PACK:
          return new PackInputStream(gis);
        case ARCFS_CRUNCH:
          return new PackInputStream(new NcompressLZWInputStream(gis, 0, riscos.archive.LZWConstants.CRUNCH, entry.getMaxBits()));
        default:
          throw new InvalidArcFSCompressionType();
      }
    } catch (UnsupportedLZWType e) {
      throw new InvalidArcFSCompressionType(e.toString());
    }
  }

  public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile {
    try {
      inFile.seek(entry.getOffset());
    } catch (IOException e) {
      throw new InvalidArcFSFile();
    }

    return new LimitInputStream(inFile, entry.getCompressedLength());
  }

  public void printArcFSInfo() {
    System.out.println("Header length = " + headerLength);
    System.out.println("Data start = " + dataStart);
    System.out.println("Version = " + version / 100 + "." + version % 100);
    System.out.println("RW Version = " + rwVersion / 100 + "." + rwVersion % 100);
    System.out.println("Arc format = " + arcFormat);
  }

  public CRC getCRCInstance() {
    /* I'm not sure what CRC algorithm ArcFS uses */
    return new NullCRC();
  }
}
