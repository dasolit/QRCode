package sample.module;

import sample.NewQRCode;

/**
 * @author Theo
 * @since 2024/12/14
 */
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
