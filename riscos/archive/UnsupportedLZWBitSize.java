// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

import java.lang.Exception;

public class UnsupportedLZWBitSize extends Exception {

  public UnsupportedLZWBitSize() {
    super("LZW bit size must be in the range 9..16");
  }
}
