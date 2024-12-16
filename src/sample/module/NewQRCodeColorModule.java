package sample.module;

import sample.NewQRCode;

/**
 * @author Theo
 * @since 2024/12/14
 */
// TODO: 복잡한 요구사항이 있을 수 있으므로 확장성을 고려하기
public class NewQRCodeColorModule implements QRCodeModule {
    private final String color;

    public NewQRCodeColorModule(String color) {
        this.color = color;
    }

    @Override
    public void applyModule(NewQRCode newQRCode) {
        newQRCode.setColor(color);
    }
}
