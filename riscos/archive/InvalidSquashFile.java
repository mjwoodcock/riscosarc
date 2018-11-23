// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

public class InvalidSquashFile extends InvalidArchiveFile {

  public InvalidSquashFile(String msg) {
    super("Invalid Squash file " + msg);
  }
}
