import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;


// TODO: 여기 있는 함수를 Class 화 할 수 있는지 생각하기
public class QRCode {
    private final String data;
    private final int size;

    public QRCode(String data, int size) {
        this.data = data;
        this.size = size;
    }

    record QRCodeImage(Byte[] buffer) {
        public ByteBuffer byteBuffer() {
            /*return  convert bufferedImage to ByteBuffer */;
            return buffer
        }
    }

    // break changes
    protected QRCodeImage generateQRCode() {
        int[][] qrMatrix = new int[size][size];

        // 1. Finder 패턴 생성
        createFinderPattern(qrMatrix, 0, 0);
        createFinderPattern(qrMatrix, 0, size - 7);
        createFinderPattern(qrMatrix, size - 7, 0);
        int[] encodedData = encodeData(data);
        placeData(qrMatrix, encodedData);

        // 3. QR 코드 이미지화
        byte[] bytes = createImage(qrMatrix);
        return new QRCodeImage(bytes);
    }

    private static void createFinderPattern(int[][] matrix, int x, int y) {
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                if (i == 0 || i == 6 || j == 0 || j == 6 || (i >= 2 && i <= 4 && j >= 2 && j <= 4)) {
                    matrix[x + i][y + j] = 1; // 검정색
                } else {
                    matrix[x + i][y + j] = 0; // 흰색
                }
            }
        }
    }

    private static int[] encodeData(String data) {
        int[] binaryData = new int[data.length() * 8];
        for (int i = 0; i < data.length(); i++) {
            int ascii = data.charAt(i);
            for (int j = 0; j < 8; j++) {
                binaryData[i * 8 + j] = (ascii >> (7 - j)) & 1;
            }
        }
        return binaryData;
    }

    private static void placeData(int[][] matrix, int[] data) {
        int row = matrix.length - 1;
        int col = matrix.length - 1;
        int index = 0;

        while (index < data.length && row > 0) {
            if (matrix[row][col] == 0) { // 빈 칸에만 데이터 배치
                matrix[row][col] = data[index];
                index++;
            }
            col--;
            if (col < 0) {
                col = matrix.length - 1;
                row--;
            }
        }
    }

    private static byte createImage(int[][] matrix) {
        int size = matrix.length;
        int scale = 10;
        BufferedImage image = new BufferedImage(size * scale, size * scale, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int color = matrix[i][j] == 1 ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                for (int x = 0; x < scale; x++) {
                    for (int y = 0; y < scale; y++) {
                        image.setRGB(i * scale + x, j * scale + y, color);
                    }
                }
            }
        }
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("qrcode.png"));
            ByteBuffer buffer = ByteBuffer.allocate(4);
            ImageIO.write(image, "png", new File("qrcode.png"));
            return buffer.get();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
