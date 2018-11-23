// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import riscos.archive.LZWConstants;

import java.lang.Exception;

public class UnsupportedLZWFile extends Exception {

  public UnsupportedLZWFile() {
    super("LZW file type must be in the range 1.." + LZWConstants.MAX_SUPPORTED_TYPE);
  }
}
