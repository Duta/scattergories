import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;

public class CentralPane extends FlowPane {
    public CentralPane() {
        super(Orientation.VERTICAL, 20, 20);
        setAlignment(Pos.CENTER);
        setColumnHalignment(HPos.CENTER);
    }
}
