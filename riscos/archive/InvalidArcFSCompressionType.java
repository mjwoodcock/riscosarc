// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

public class InvalidArcFSCompressionType extends InvalidCompressionType {

  public InvalidArcFSCompressionType() {
    super("Invalid ArcFS compression type");
  }

  public InvalidArcFSCompressionType(String msg) {
    super("Invalid ArcFS compression type: " + msg);
  }
}
