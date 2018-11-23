// vim:ts=2:sw=2:expandtab:ai

/* A class to handle container files that do not hold CRC information */

package riscos.archive;

public class NullCRC extends CRC {

  public NullCRC() {
  }

  public void setDataLength(int crcSize) {
  }

  public void update(byte val) {
  }

  public void update(byte[] chunk, int offset, int length) {
  }

  public void update(byte[] chunk) {
  }

  public void reset() {
  }

  public long getValue() {
    return 0;
  }

  public boolean compare(long crcVal) {
    return true;
  }
}
