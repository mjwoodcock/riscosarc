// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

public class UnsupportedLZWType extends Exception {

  public UnsupportedLZWType() {
    super("LZW type not supported by this class");
  }

}
