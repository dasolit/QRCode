package module;

public enum ColorList {
  RED(0xFF0000),
  GREEN(0x00FF00),
  BLUE(0x0000FF),
  WHITE(0xFFFFFF),
  BLACK(0x000000),
  CUSTOM(0x324556);

  public final int hexValue;

  ColorList(int hexValue) {
    this.hexValue = hexValue;
  }

  public int getHexValue() {
    return hexValue;
  }

}
