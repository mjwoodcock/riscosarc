// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

public class CorruptLZWData extends Exception {

  public CorruptLZWData(String err) {
    super("LZW data is corrupt: " + err);
  }
}
