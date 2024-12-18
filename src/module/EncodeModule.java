package module;

import java.nio.ByteBuffer;

public class EncodeModule {
    private final byte[] data;

    public EncodeModule(String encodedQRCode) {
      this.data = encodedQRCode.getBytes();
    }

   public  ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(data);
    }
}
