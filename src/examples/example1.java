package examples;

import java.io.IOException;
import module.ColorList;
import module.ColorModule;
import module.CompressModule;
import module.QrCode;

public class example1 {

  public static void main(String[] args) throws IOException {
    String text = "example1";
    QrCode qr = new QrCode(text)
        .addModule(new ColorModule(ColorList.GREEN.getHexValue()))
        .addModule(new CompressModule(text));

    qr.initialize();
  }

}
