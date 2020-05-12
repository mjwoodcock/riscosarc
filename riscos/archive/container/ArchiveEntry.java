// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.RandomAccessInputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class ArchiveEntry {

  protected boolean isDir;
  protected RandomAccessInputStream inFile;
  protected int dataStart;
  protected int comptype;
  protected String name;
  protected String localFilename;
  protected int complen;
  protected int date;
  protected int time;
  protected long crc;
  protected int origlen;
  protected int load;
  protected int exec;
  protected int attr;
  protected long seek;
  protected int maxbits;
  protected long fileDate;
  protected boolean isDel;
  protected boolean isEof;
  protected boolean doAppendFiletype;


  /** Constructs and ArchiveEntry.
   * @param in an input stream to read the data from
   * @param datStart the location in the stream of the compressed data
   * @param appendFiletype true if the filetype should be appended to the filename.  False otherwise
   */
  ArchiveEntry(RandomAccessInputStream in, int datStart, boolean doAppendFiletype) {
    this.inFile = in;
    this.dataStart = datStart;
    this.doAppendFiletype = doAppendFiletype;
  }

  /**
   * Calculates the file time from the RISC OS load and exec values
   */
  protected void calculateFileTime() {
/*
    long high = (load & 0xff) - 0x33 & 0xffffffffL;
    long low;
    long lexec = ((long)exec) & 0xffffffffL;

    low = (lexec - 0x6e996a00L) & 0xffffffffL;
    fileDate = (high * 42949673 + low / 100) & 0xffffffffL;
    fileDate *= 1000;
*/
/*
    long t1 = ((long)exec) & 0xffffffffL;
    long t2 = ((long)load) & 0xff;

    System.out.println("mjw... load " + Long.toHexString(load));
    System.out.println("mjw... exec " + Long.toHexString(exec));
    System.out.println("mjw... t1 " + Long.toHexString(t1));
    long tc = 0x6e996a00L;
    System.out.println("mjw... tc " + Long.toHexString(tc));
    if (t1 < tc)
      t2--;
    t1 -= tc;
    t1 &= 0xffffffffL;
    System.out.println("mjw... t1 " + Long.toHexString(t1));
    t2 -= 0x33;
    t2 &= 0xffffffffL;
    System.out.println("mjw... " + Long.toHexString(t2));

    t1 = (t1 / 100) + (t2 * 42949673L);
    t1 &= 0xffffffffL;
    System.out.println("mjw... " + Long.toHexString(t1));
    fileDate = t1 - t2;
    fileDate *= 1000;
    System.out.println("mjw... " + localFilename + " " + Long.toHexString(fileDate));
*/
    long dt = (((long)load & 0xff) << 32) + exec;
    dt -= 0x336E996A00L;
    dt *= 10;
    fileDate = dt;
    System.out.println("mjw... " + localFilename + " " + Long.toHexString(fileDate));
  }

  /**
   * Deletes a stale file from the disk.
   */
  public void cleanOldFile() {
    File f = new File(localFilename);

    if (f.isFile()) {
      f.delete();
    }
  }

  /**
   * Stamps the file with the correct date stamp.
   */
  public void setFileTime() {
    File f = new File(localFilename);

    f.setLastModified(fileDate);
  }

  /**
   * Gets the RISC OS filetype from the RISC OS load value
   * @return the filetype
   */
  protected int getFileType() {
    int filetype = -1;
    if ((load & 0xfff00000) == 0xfff00000) {
      filetype = (load >> 8) & 0xfff;
    }

    return filetype;
  }

  /**
   * Appends the RISC OS filetype to the filename if necessary.
   */
  protected void appendFiletype() {
    if (doAppendFiletype) {
      if ((load & 0xfff00000) == 0xfff00000) {
        int filetype = (load >> 8) & 0xfff;
        localFilename += "," + Integer.toHexString(filetype);
      }
    }
  }

  /**
   * Translates the filename from a RISC OS representation to the
   * local representation.
   * @param roname the filename according to RISC OS
   * @return the filename suitable for the local filesystem
   */
  protected static String translateFilename(String roname) {
    String localname = "";

    for (int i = 0; i < roname.length(); i++ ) {
      switch (roname.charAt(i)) {
        case '/':
          localname += '.';
          break;
        case '.':
          localname += '/';
          break;
        default:
          localname += roname.charAt(i);
          break;
      }
    }

    return localname;
  }

  /** Gets the length of the compressed data in the containing ArchiveFile
   * @return the compressed data length
   * @see ArchiveFile
   */
  public int getCompressedLength() {
    return complen;
  }

  /** Gets the offset of the compressed data in the containing ArchiveFile
   * @return the offset of the data
   * @see ArchiveFile
   */
  public long getOffset() {
    return seek;
  }

  /** Gets the length of the uncompressed data of this ArchiveEntry
   * @return the uncompressed file length
   */
  public int getUncompressedLength() {
    return origlen;
  }

  /** Creates the directory structire needed to extract the file
   * @param prefix the base directory to create the structure in
   */
  public void mkDir(String prefix) {
    File f = new File(prefix + File.separator + localFilename);
    File parent = f.getParentFile();

    if (parent != null && !parent.isDirectory()) {
      if (!parent.isFile()) {
        parent.mkdirs();
      }
    }
  }

  /** Creates the directory structire (under the current directory)
   * needed to extract the file
   */
  public void mkDir() {
    mkDir("");
  }

  public boolean isDir() {
    return isDir;
  }

  /** Gets the filename as it would appear on the local filesystem
   * @return the local filename
   */
  public String getLocalFilename() {
    return localFilename;
  }

  public void printEntryData() {
  }

  public int getMaxBits() {
    return maxbits;
  }

  public int getCompressType() {
    return comptype;
  }

  /* Gets the name as it would appear on RISC OS
   * @return the filename
   */
  public String getName() {
    return name;
  }

  /* Gets the CRC value
   * @return the crc value
   * @see CRC
   */
  public long getCrcValue() {
    return crc;
  }
}
