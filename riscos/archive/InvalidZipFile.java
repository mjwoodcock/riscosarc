// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import riscos.archive.InvalidArchiveFile;

public class InvalidZipFile extends InvalidArchiveFile {

  public InvalidZipFile() {
    super("Invalid Zip file");
  }
}
