// vim:ts=2:sw=2:expandtab:ai
/* An abstract class to deal with calculating CRC data. */

package riscos.archive;

public abstract class CRC {

  /**
   * Update the CRC check with a character
   * @param c the character to add
   */
  public abstract void update(byte val);

  /**
   * Update the CRC check with an arrar of characters.  The data of
   * a specific length starts at a given offset.
   * @param chunk the buffer to use
   * @param offset the offset into the buffer
   * @param length the length of the data
   */
  public abstract void update(byte[] chunk, int offset, int length);

  /**
   * Update the CRC check with an arrar of characters.
   * a specific length starts at a given offset.
   * @param chunk the buffer to use
   */
  public abstract void update(byte[] chunk);

  /**
   * Resets the CRC value to its initial state.
   */
  public abstract void reset();

  /**
   * Get the calculated CRC value
   * @return the CRC value.
   */
  public abstract long getValue();

  /**
   * Sets the number of characters that will be used to calculate
   * the CRC.
   * @param crcSize the total number of bytes to be used in the CRC calculation
   */
  public abstract void setDataLength(int crcSize);

  /**
   * Compares the calculated CRC value with an expected CRC value.
   * @return true if the values are the same, false otherwise.
   */
  public abstract boolean compare(long crcVal);
}
