import java.io.IOException;
import module.ColorModule;
import module.QrCode;

public class Main {

  public static void main(String[] args) throws IOException {

    String text = "hohihihihhi";
    QrCode qr = QrCode.createQrCode(text).addModule(new ColorModule(0x325556));
    qr.initialize();
  }
}