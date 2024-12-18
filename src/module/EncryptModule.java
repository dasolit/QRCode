package module;

import java.nio.ByteBuffer;

public class EncryptModule {
    private final byte[] data;

    public EncryptModule(String encodedQRCode) {
      this.data = encodedQRCode.getBytes();
    }

   public  ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(data);
    }
}
