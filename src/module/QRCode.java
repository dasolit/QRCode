package module;

import java.util.ArrayList;

import java.util.List;

public class QRCode {
    private String color;
    private String data;
    private List<QRCodeModule> modules = new ArrayList<>();

    public void setColor(String color) {
        this.color = color;
    }

    public void setData(String data) {
        this.data = data;
    }

    public QRCode addModule(QRCodeModule module) {
        modules.add(module);
        return this;
    }

    public void initialize() {
        for (QRCodeModule module : modules) {
            module.applyModule(this);
        }
    }
}
