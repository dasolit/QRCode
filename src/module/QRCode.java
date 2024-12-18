package module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

public class QRCode {
    private String color;
    private Map<Object, Object> data = new HashMap<>();
    private List<QRCodeModule> modules = new ArrayList<>();

    public void setColor(String color) {
        this.color = color;
    }

    public void setData(Map<Object, Object> data) {
        this.data = data;
    }

    public void addData(Object key, Object value) {
        this.data.put(key, value);
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
