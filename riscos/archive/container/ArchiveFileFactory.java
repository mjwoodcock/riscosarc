// vim:ts=2:sw=2:expandtab:ai

package riscos.archive.container;

import riscos.archive.InvalidArchiveFile;
import riscos.archive.container.ArchiveFile;

import java.io.IOException;

public class ArchiveFileFactory {

  private ArchiveFile archive;
  private static final int[] readerFiletypes = {0x3fb, 0x68e, 0xd96, 0xddc, 0xfca};
  private static final String[] readerFormatNames = {"ArcFS",
                                                     "PackDir",
                                                     "CFS",
                                                     "Spark/ZIP/MSDOS Arc",
                                                     "RISC OS Squash"};

  /** Constructs an ArchiveFileFactory to open a file without a
   * password.  RISC OS filetype information will be appended to the
   * local filename.
   * @param filename the file to open
   */
  public ArchiveFileFactory(String filename) throws IOException, InvalidArchiveFile {
    this(filename, null);
  }

  /** Constructs an ArchiveFileFactory to open a file with an optional
   * password.  RISC OS filetype information will be appended to the
   * local filename.
   * @param filename the file to open
   * @param pass the password.  Set to null for no password
   */
  public ArchiveFileFactory(String filename, String pass) throws IOException, InvalidArchiveFile {
    this(filename, pass, true);
  }

  /** Constructs an ArchiveFileFactory to open a file with an optional
   * password.  RISC OS filetype information will be appended to the
   * local filename.
   * @param filename the file to open
   * @param pass the password.  Set to null for no password
   * @param appendFiletype RISC OS filetype information is appended to the local filename if true
   */
  public ArchiveFileFactory(String filename, String pass, boolean appendFiletype) throws IOException, InvalidArchiveFile {
    this(filename, pass, appendFiletype, 0);
  }

  /** Constructs an ArchiveFileFactory to open a file with an optional
   * password.  The Factory will try to open the file using all
   * supported ArchiveFile types.
   * @param filename the file to open
   * @param pass the password.  Set to null for no password
   * @param appendFiletype RISC OS filetype information is appended to the local filename if true
   * @param options Option flags from ArchiveFile.*_OPT
   * @see ArchiveFile
   */
  public ArchiveFileFactory(String filename, String pass,
                            boolean appendFiletype,
                            int options) throws IOException,
                                                InvalidArchiveFile {
    try {
      SparkFile sfs = new SparkFile(filename, pass, appendFiletype, options);
      sfs.openForRead();
      archive = sfs;
      return;
    } catch (Exception ex) {
    }

    try {
      ArcFSFile afs = new ArcFSFile(filename, pass, appendFiletype, options);
      afs.openForRead();
      archive = afs;
      return;
    } catch (Exception ex) {
    }

    try {
      PackDirFile pd = new PackDirFile(filename, appendFiletype, options);
      pd.openForRead();
      archive = pd;
      return;
    } catch (Exception ex) {
    }

    try {
      SquashFile sf = new SquashFile(filename, appendFiletype, options);
      sf.openForRead();
      archive = sf;
      return;
    } catch (Exception ex) {
    }

    try {
      CFSFile cfs = new CFSFile(filename, appendFiletype, options);
      cfs.openForRead();
      archive = cfs;
      return;
    } catch (Exception ex) {
    }

    try {
      ZipFileWrapper zip = new ZipFileWrapper(filename, null, appendFiletype, options);
      zip.openForRead();
      archive = zip;
      return;
    } catch (Exception ex) {
    }

    try {
      ArcFile arc = new ArcFile(filename, null, options);
      arc.openForRead();
      archive = arc;
      return;
    } catch (Exception ex) {
    }

  }

  /** Gets the ArchiveFile opened by this Factory.
   * @return the ArchiveFile
   */
  public ArchiveFile getArchiveFile() {
    return archive;
  }

  /** Gets A list of RISC OS filetypes this library supports.
   * @return the filetype list
   */
  public static int[] getReaderFiletypes() {
    return readerFiletypes;
  }

  /** Gets A list of human readable descriptions of containers that this library supports.
   * @return the container name list
   */
  public static String[] getReaderFormatNames() {
    return readerFormatNames;
  }
}
