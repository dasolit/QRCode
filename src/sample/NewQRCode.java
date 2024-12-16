package sample;

/**
 * @author Theo
 * @since 2024/12/14
 */

import sample.module.NewQRCodeColorModule;
import sample.module.QRCodeModule;

import java.util.List;

/**
 * 정말 많은 요구사항
 *  - 색깔은 흰색 대신에 빨간색으로 넣고, 검은색 대신에 보라색을 넣어야 되고, 그런데 이 값을 사용자가 마음껏 커스터마이징도 되어야 함.
 *  - 파일 이름도 사용자가 원하는 규칙으로 런타임에 만들 수 있어야 한다.
 *    -> 예를 들어서, "QRCode_2024_12_14_14_00_00.png" 이런 식으로 만들 수 있어야 한다.
 */
public class NewQRCode {
    private String color;
    private List<QRCodeModule> modules = List.of();

    public void setColor(String color) {
        this.color = color;
    }

    public NewQRCode addModule(QRCodeModule module) {
        modules.add(module);
        return this;
    }

    public void initialize() {
        for (QRCodeModule module : modules) {
            module.applyModule(this);
        }
    }

    public EncodedNewQRCode encode() {
        return new EncodedNewQRCode("encodedQRCode");
    }

    public static void main(String[] args) {
        NewQRCode newQRCode = new NewQRCode()
                .addModule(new NewQRCodeColorModule("RED"));
        newQRCode.initialize();
        // newQRCode.setData("A");

        EncodedNewQRCode encodedNewQRCode = newQRCode.encode();
    }
}
