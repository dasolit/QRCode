package module;

import java.io.IOException;

public class ImageModule implements QrModule {

  public String imageName;
  public String imageFormat;

  public ImageModule(String imageName, String imageFormat) {
    this.imageName = imageName;
    this.imageFormat = imageFormat;
  }


  @Override
  public void applyModule(QrCode qr) throws IOException {
    qr.setImage(imageName, imageFormat);
  }
}
