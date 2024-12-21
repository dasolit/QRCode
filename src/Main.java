import java.io.IOException;
import module.ColorList;
import module.ColorModule;
import module.CompressModule;
import module.ImageModule;
import module.QrCode;

public class Main {

  public static void main(String[] args) throws IOException {

    String text = "hohihihihhi";
    QrCode qr = new QrCode(text)
        .addModule(new ColorModule(ColorList.GREEN.getHexValue()))
        .addModule(new CompressModule(text))
        .addModule(new ImageModule("qrcode", "png"));
    qr.initialize();
  }
}