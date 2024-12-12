public class Main {
  public static void main(String[] args) {

    QRCode qr = new QRCode("TestData", 21);
    if(qr.generateQRCode()) {
      System.out.println("QR Code Generated");
    }


  }
}
