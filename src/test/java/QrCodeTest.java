
import module.QrCode;
import module.QrModule;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class QrCodeTest {

  @Test
  public void testSetData() {
    QrCode qrCode = new QrCode("initial1");
    qrCode.setData("newData");
    assertEquals("newData", qrCode.getData());
  }

  @Test
  public void testSetColor() {
    QrCode qrCode = new QrCode("test");
    qrCode.setColor(0xFF0000); // Red color
  }

  @Test
  public void testSetImage() {
    QrCode qrCode = new QrCode("test");
    qrCode.setImage("custom_image", "jpg");
  }

  @Test
  public void testAddModule() {
    QrCode qrCode = new QrCode("test");
    QrModule mockModule = (qr) -> {};
    qrCode.addModule(mockModule);
  }

  @Test
  public void testInitialize() throws IOException {
    QrCode qrCode = new QrCode("https://example.com");
    qrCode.setImage("test_qr_code", "png");
    qrCode.setColor(0x000000);
    qrCode.initialize();

    File file = new File("test_qr_code.png");
    assertTrue(file.exists());
    if (file.exists()) {
      file.delete();
    }
  }

  @Test
  public void testEncodeText() {
    QrCode qrCode = QrCode.encodeText("https://example.com", QrCode.Ecc.LOW);
    assertNotNull(qrCode);
  }

  @Test
  public void testEncodeBinary() {
    QrCode qrCode = QrCode.encodeBinary("binaryData".getBytes(), QrCode.Ecc.MEDIUM);
    assertNotNull(qrCode);
  }

  @Test
  public void testVersionAndSize() {
    QrCode qrCode = QrCode.encodeText("VersionTest", QrCode.Ecc.LOW);
    assertTrue(qrCode.version >= QrCode.MIN_VERSION && qrCode.version <= QrCode.MAX_VERSION);
    assertTrue(qrCode.size > 0);
  }

}
