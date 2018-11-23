// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

public abstract class InvalidCompressionType extends Exception {

  public InvalidCompressionType(String msg) {
    super(msg);
  }
}
