import javafx.scene.control.Label;
import javafx.scene.paint.Paint;

public class DefaultLabel extends Label {
    public DefaultLabel() {
        this("");
    }

    public DefaultLabel(int size) {
        this("", size);
    }

    public DefaultLabel(String text) {
        this(text, 32);
    }

    public DefaultLabel(String text, int size) {
        super(text);
        setTextFill(Paint.valueOf("white"));
        setStyle("-fx-font-size: " + size + "px;");
    }
}
