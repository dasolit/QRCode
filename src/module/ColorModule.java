package module;

public class ColorModule implements QRModule {
  private final int color;

  public ColorModule(int color) {
      this.color = color;
  }

  @Override
  public void applyModule(QrCode qr) {
    qr.setColor(color);
  }
}
