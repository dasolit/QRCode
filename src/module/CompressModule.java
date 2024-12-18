package module;

import java.nio.ByteBuffer;

public class CompressModule {
    private final byte[] data;

    public CompressModule(String encodedQRCode) {
      this.data = encodedQRCode.getBytes();
    }

   public  ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(data);
    }
}
