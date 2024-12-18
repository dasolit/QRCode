package module;

public class ColorModule implements QRCodeModule {
    private final String color;

    public ColorModule(String color) {
        this.color = color;
    }

    @Override
    public void applyModule(QRCode QRCode) {
        QRCode.setColor(color);
    }
}
