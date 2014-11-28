import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Player {
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String name;
    private int score;
    private int numCategories;
    private List<String> answers;

    public Player(Socket socket) throws IOException {
        this.socket = socket;
        answers = new ArrayList<>();
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        name = in.readLine();
    }

    public String getName() {
        return name;
    }

    public boolean isAlive() {
        return queryPlayer("alive");
    }

    public void resetScore() {
        score = 0;
    }

    public boolean sendCategories(List<String> categories) {
        out.println("categories");
        out.println(numCategories = categories.size());
        for(String category : categories) {
            out.println(category);
        }
        return queryPlayer("received");
    }

    private boolean queryPlayer(String query) {
        out.println(query + "?");
        try {
            return "yes".equals(in.readLine());
        } catch(IOException e) {
            return false;
        }
    }

    public boolean sendLetter(char letter) {
        out.println("letter");
        out.println(letter);
        return queryPlayer("received");
    }

    public boolean requestAnswers() {
        out.println("answers");
        answers = new ArrayList<>();
        for(int i = 0; i < numCategories; i++) {
            try {
                String answer = in.readLine();
                if(answer == null) {
                    return false;
                } else {
                    answers.add(answer);
                }
            } catch(IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public String getAnswer(int ans) {
        return answers.get(ans);
    }

    public void incrementScore() {
        score++;
    }

    public int getScore() {
        return score;
    }
}
