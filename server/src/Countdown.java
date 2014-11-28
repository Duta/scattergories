import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class Countdown extends DefaultLabel {
    private int max;
    private int seconds;
    private Timeline timeline;

    public Countdown(int maxSeconds) {
        super(128);
        seconds = max = maxSeconds;
        timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), event -> {
            seconds--;
            updateText();
            if(seconds <= 0) {
                stop();
                onComplete();
            }
        }));
        updateText();
        start();
    }

    protected void onComplete() {}

    private void updateText() {
        setText(seconds + "s");
    }

    public void start() {
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }

    public void reset() {
        seconds = max;
    }
}
