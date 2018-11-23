// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import java.lang.Exception;

public abstract class InvalidArchiveFile extends Exception {

  public InvalidArchiveFile(String msg) {
    super(msg);
  }
}
