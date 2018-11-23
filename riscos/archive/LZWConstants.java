// vim:ts=2:sw=2:expandtab:ai

package riscos.archive;

public class LZWConstants {
  public static final int COMPRESS = 0;
  public static final int SQUASH = 1;
  public static final int CRUNCH = 2;
  public static final int UNIX_COMPRESS = 3;
  public static final int ZOO = 4;
  public static final int PACKDIR = 5;
  public static final int MAX_SUPPORTED_TYPE = PACKDIR;

  public static final int CRUNCH_BITS = 12;
  public static final int SQUASH_BITS = 13;
  public static final int COMPRESS_BITS = 16;
  public static final int FIRST = 257; /* First free entry */
  public static final int Z_EOF = 257; /* ZOO EOF */
  public static final int CLEAR = 256; /* Table clear input code */
}
