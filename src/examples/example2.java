package examples;

import java.io.IOException;
import module.CompressModule;
import module.ImageModule;
import module.QrCode;

public class example2 {

  public static void main(String[] args) throws IOException {
    String text = "example2";
    QrCode qr = new QrCode(text)
        .addModule(new CompressModule(text))
        .addModule(new ImageModule("myQrCode", "jpg"));

    qr.initialize();
  }

}
