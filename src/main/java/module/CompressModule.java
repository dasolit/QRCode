package module;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressModule implements QrModule {

  private String data;

  public CompressModule(String data) {
    this.data = data;
  }

  public String compress() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
      gzipOutputStream.write(this.data.getBytes());
    }
    return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
  }

  public static String decompress(String compressedData) throws IOException {
    byte[] compressedBytes = Base64.getDecoder().decode(compressedData);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);
    try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        InputStreamReader reader = new InputStreamReader(gzipInputStream);
        BufferedReader bufferedReader = new BufferedReader(reader)) {
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        result.append(line);
      }
      return result.toString();
    }
  }

  @Override
  public void applyModule(QrCode qr) throws IOException {
    qr.setData(compress());
  }
}
