// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.InvalidZipFile;
import riscos.archive.container.ArchiveEntry;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipEntryWrapper extends ArchiveEntry {

  private ZipFileWrapper zipFile;
  private ZipEntry zipEntry;
  private static final short EF_SPARK = 0x4341;
  private static final int SPARKID_2 = 0x30435241;

  public ZipEntryWrapper(String name, ZipFileWrapper zip, ZipEntry ze, boolean appendFiletype) {
    super(null, 0, appendFiletype);
    this.zipFile = zip;
    this.zipEntry = ze;
    this.name = name;
    this.localFilename = name;
    super.complen = (int)this.zipEntry.getCompressedSize();
    super.origlen = (int)this.zipEntry.getSize();
    super.crc = this.zipEntry.getCrc();
    if (ze.getExtra() != null) {
      parseExtraData(ze.getExtra());
      appendFiletype();
    }
  }

  private short byteArrayToShort(byte[] buf, int offset) {
    return (short)((buf[offset + 1] << 8) | (buf[offset] & 0xff));
  }

  private int byteArrayToInt(byte[] buf, int offset) {
    long l1 = (buf[offset + 3] << 24) & 0xff000000;
    long l2 = (buf[offset + 2] << 16) & 0xff0000;
    long l3 = (buf[offset + 1] << 8) & 0xff00;
    long l4 = buf[offset + 0] & 0xff;
    return (int)(l1 | l2 | l3 | l4);
  }

  private void parseExtraData(byte[] buf) {
    int id = byteArrayToShort(buf, 0);
    int len = byteArrayToShort(buf, 2);

    if (id == 0x4341) {
      if (len == 24 || len == 20) {
        int id2 = byteArrayToInt(buf, 4);
        if (id2 == SPARKID_2) {
          super.load = byteArrayToInt(buf, 8);
          super.exec = byteArrayToInt(buf, 12);
          super.attr = byteArrayToInt(buf, 16);

          calculateFileTime();
        }
      }
    }
  }

  public void readEntry(String curDir, long offset) throws IOException, InvalidZipFile {
  }

  public void printEntryData() {
  }

  public boolean isEof() {
    return false;
  }

  public long getNextEntryOffset() {
    return -1;
  }

  public ZipEntry getZipEntry() {
    return this.zipEntry;
  }
}
