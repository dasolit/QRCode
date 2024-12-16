package sample;

import java.nio.ByteBuffer;

/**
 * @author Theo
 * @since 2024/12/14
 */

// TODO: 인코드도 Class 화 생각하기
public class EncodedNewQRCode {
    private final byte[] data;

    public EncodedNewQRCode(String encodedQRCode) {
        byte[] binaryData = new byte[encodedQRCode.length() * 8];
        for (int i = 0; i < encodedQRCode.length(); i++) {
            int ascii = encodedQRCode.charAt(i);
            for (int j = 0; j < 8; j++) {
                binaryData[i * 8 + j] = (ascii >> (7 - j)) & 1;
            }
        }
        data = binaryData;
    }

   public  ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(data);
    }
}
