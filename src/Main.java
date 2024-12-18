import module.ColorModule;
import module.QRCode;

public class Main {
  public static void main(String[] args) {
    QRCode QRCode = new QRCode()
        .addModule(new ColorModule("RED"));
    QRCode.initialize();

  }
}
