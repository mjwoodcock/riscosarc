// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.CRC;
import riscos.archive.InvalidArchiveFile;
import riscos.archive.InvalidCompressionType;
import riscos.archive.InvalidZipFile;
import riscos.archive.ZipCRC;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipFileWrapper extends ArchiveFile {
  private ZipFile zipFile;
  private Vector<ArchiveEntry> entryList;
  private boolean appendFiletype;
  private int options;

  public ZipFileWrapper(String filename, String pass) throws IOException, InvalidArchiveFile {
    this(filename, pass, true, 0);
  }

  public ZipFileWrapper(String filename, String pass, boolean appendFiletype,
                        int options) throws IOException, InvalidArchiveFile {
    try {
      this.zipFile = new ZipFile(filename);
    } catch (ZipException e) {
      throw new InvalidZipFile();
    }
    this.entryList = new Vector<ArchiveEntry>();
    this.appendFiletype = appendFiletype;
    this.options = options;
  }

  public int read32() {
    return 0xffffffff;
  }

  public int read16() {
    return 0xffff;
  }

  public byte[] getPasswd() {
    return null;
  }

  public void openForRead() throws IOException, InvalidArchiveFile {
    Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
    while (entries.hasMoreElements()) {
      try {
        ZipEntry ze = entries.nextElement();
        if (!ze.isDirectory()) {
          this.entryList.add(new ZipEntryWrapper(ze.getName(), this, ze, this.appendFiletype));
        }
      } catch (Exception e) {
        if ((this.options & ArchiveFile.IGNORE_BAD_ZIP_ENTRY_OPT) == 0) {
          throw new InvalidZipFile();
        }
      }
    }
  }

  public Enumeration<ArchiveEntry> entries() {
    return this.entryList.elements();
  }

  public InputStream getInputStream(ArchiveEntry entry) throws InvalidArchiveFile, InvalidCompressionType {
    try {
      ZipEntryWrapper ze = (ZipEntryWrapper)entry;
      return this.zipFile.getInputStream(ze.getZipEntry());
    } catch (IOException e) {
      throw new InvalidZipFile();
    }
  }

  public InputStream getRawInputStream(ArchiveEntry entry) throws InvalidArchiveFile {
    return null;
  }

  public CRC getCRCInstance() {
    return new ZipCRC();
  }
}
