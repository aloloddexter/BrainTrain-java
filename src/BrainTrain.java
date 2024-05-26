import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.*;

public class BrainTrain extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private Clip bgMusic;
    private Clip gameFinishMusic;
    private Clip gameStartMusic;
    private Clip difficultySelectedMusic;
    private Clip correctSound;
    private Clip incorrectSound;
    private Map<String, DifficultyLevel> difficultyLevels;
    private int currentQuestion;
    private int totalQuestions;
    private int score;
    private String currentDifficulty;
    private JLabel questionLabel;
    private JTextField answerField;
    private Timer timer;
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private Font customFont;
    private Color originalAnswerFieldColor;
    private final String dataPath = "data"; // Base path for sounds
    private JLabel countdownLabel;

    public BrainTrain() {
        setTitle("Brain Training");
        setSize(1366, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        loadCustomFont();
        
        mainPanel = new JPanel(new CardLayout());
        add(mainPanel);

        difficultyLevels = new HashMap<>();
        difficultyLevels.put("Easy", new DifficultyLevel(1, 10, new char[]{'+', '-', '*', '/'}, 5));
        difficultyLevels.put("Normal", new DifficultyLevel(10, 99, new char[]{'+', '-', '*', '/'}, 15));
        difficultyLevels.put("Hard", new DifficultyLevel(10, 100, new char[]{'+', '-', '*', '/'}, 30));
       
        initMainMenu();
        initDifficultySelection();
        initGameScreen();

        bgMusic = loadSound(dataPath + "/sounds/BGMusic.wav");
        gameFinishMusic = loadSound(dataPath + "/sounds/GameFinish.wav");
        gameStartMusic = loadSound(dataPath + "/sounds/GameStart.wav");
        difficultySelectedMusic = loadSound(dataPath + "/sounds/DifficultySelected.wav");
        
        playBackgroundMusic();
        
    }

    private void loadCustomFont() {
        try {
            customFont = Font.createFont(Font.TRUETYPE_FONT, new File("src/data/fonts/CustomFont.ttf")).deriveFont(24f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            customFont = new Font("Serif", Font.PLAIN, 24);
        }
    }

    private ImageIcon loadImage(String path) {
        File file = new File(path);
        if (file.exists()) {
            return new ImageIcon(path);
        } else {
            System.err.println("Image not found: " + path);
            return null;
        }
    }

    private void initMainMenu() {
        JPanel mainMenu = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(new ImageIcon(dataPath + "/images/background-3.jpg").getImage(), 0, 0, null);
            }
        };
        mainMenu.setLayout(null);

        JButton playButton = new JButton(loadImage(dataPath + "/images/Play.png"));
        if (playButton.getIcon() == null) {
            playButton.setText("Play");
            playButton.setFont(customFont);
        }
        playButton.setBounds(550, 260, 280, 120);
        playButton.addActionListener(e -> {
            stopBackgroundMusic();
            playSoundOnce(gameStartMusic);
            showDifficultySelection();
        });
        mainMenu.add(playButton);

        JButton exitButton = new JButton(loadImage(dataPath + "/images/Exit.png"));
        if (exitButton.getIcon() == null) {
            exitButton.setText("Exit");
            exitButton.setFont(customFont);
        }
        exitButton.setBounds(550, 450, 280, 120);
        exitButton.addActionListener(e -> showCloseConfirmation());
        mainMenu.add(exitButton);

        mainPanel.add(mainMenu, "MainMenu");
    }

    private void initDifficultySelection() {
        JPanel difficultySelection = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(new ImageIcon(dataPath + "/images/background-3.jpg").getImage(), 0, 0, null);
                playSoundOnce(difficultySelectedMusic);
            }
        };
        difficultySelection.setLayout(null);

        JButton easyButton = new JButton(loadImage(dataPath + "/images/Easy.png"));
        if (easyButton.getIcon() == null) {
            easyButton.setText("Easy");
            easyButton.setFont(customFont);
        }
        easyButton.setBounds(100, 250, 200, 100);
        easyButton.addActionListener(e -> {
            stopAllSounds();
            playSoundOnce(difficultySelectedMusic);
            startGame("Easy");
        });
        difficultySelection.add(easyButton);

        JButton normalButton = new JButton(loadImage(dataPath + "/images/Normal.png"));
        if (normalButton.getIcon() == null) {
            normalButton.setText("Normal");
            normalButton.setFont(customFont);
        }
        normalButton.setBounds(400, 250, 200, 100);
        normalButton.addActionListener(e -> {
            stopAllSounds();
            playSoundOnce(difficultySelectedMusic);
            startGame("Normal");
        });
        difficultySelection.add(normalButton);

        JButton hardButton = new JButton(loadImage(dataPath + "/images/Hard.png"));
        if (hardButton.getIcon() == null) {
            hardButton.setText("Hard");
            hardButton.setFont(customFont);
        }
        hardButton.setBounds(700, 250, 200, 100);
        hardButton.addActionListener(e -> {
            stopAllSounds();
            playSoundOnce(difficultySelectedMusic);
            startGame("Hard");
        });
        difficultySelection.add(hardButton);

        JButton backButton = new JButton(loadImage(dataPath + "/images/Back.png"));
        if (backButton.getIcon() == null) {
            backButton.setText("Back");
            backButton.setFont(customFont);
        }
        backButton.setBounds(50, 50, 100, 50);
        backButton.addActionListener(e -> {
            stopAllSounds();
            playBackgroundMusic();
            showMainMenu();
        });
        difficultySelection.add(backButton);

        mainPanel.add(difficultySelection, "DifficultySelection");
    }

    private void initGameScreen() {
        JPanel gameScreen = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(new ImageIcon(dataPath + "/images/background-3.jpg").getImage(), 0, 0, null);
            }
        };
        gameScreen.setLayout(null);

        scoreLabel = new JLabel("Score: 0", SwingConstants.LEFT);
        scoreLabel.setFont(customFont);
        scoreLabel.setBounds(50, 50, 200, 50);
        gameScreen.add(scoreLabel);

        timerLabel = new JLabel("Time: 0s", SwingConstants.RIGHT);
        timerLabel.setFont(customFont);
        timerLabel.setBounds(1116, 50, 200, 50);
        gameScreen.add(timerLabel);

        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setFont(customFont);
        questionLabel.setBounds(333, 150, 700, 50);
        gameScreen.add(questionLabel);

        answerField = new JTextField();
        answerField.setFont(customFont);
        answerField.setBounds(533, 250, 300, 50);
        gameScreen.add(answerField);

        JButton submitButton = new JButton(loadImage(dataPath + "/images/Submit.png"));
        if (submitButton.getIcon() == null) {
            submitButton.setText("Submit");
            submitButton.setFont(customFont);
        }
        submitButton.setBounds(568, 350, 230, 100);
        submitButton.addActionListener(e -> checkAnswer());
        gameScreen.add(submitButton);

        originalAnswerFieldColor = answerField.getBackground();

        mainPanel.add(gameScreen, "GameScreen");
    }

    private void playBackgroundMusic() {
        if (bgMusic != null) {
            bgMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void stopBackgroundMusic() {
        if (bgMusic != null && bgMusic.isRunning()) {
            bgMusic.stop();
        }
    }

    private void stopAllSounds() {
        if (bgMusic != null && bgMusic.isRunning()) {
            bgMusic.stop();
        }
        if (gameFinishMusic != null && gameFinishMusic.isRunning()) {
            gameFinishMusic.stop();
        }
        if (gameStartMusic != null && gameStartMusic.isRunning()) {
            gameStartMusic.stop();
        }
        if (difficultySelectedMusic != null && difficultySelectedMusic.isRunning()) {
            difficultySelectedMusic.stop();
        }
        if (correctSound != null && correctSound.isRunning()) {
            correctSound.stop();
        }
        if (incorrectSound != null && incorrectSound.isRunning()) {
            incorrectSound.stop();
        }
    }

   

    private Clip loadSound(String filepath) {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(filepath));
            clip.open(inputStream);
            return clip;
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void playSoundOnce(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void showMainMenu() {
        cardLayout = (CardLayout) mainPanel.getLayout();
        cardLayout.show(mainPanel, "MainMenu");
    }

    private void showDifficultySelection() {
        cardLayout = (CardLayout) mainPanel.getLayout();
        cardLayout.show(mainPanel, "DifficultySelection");
    }

    private void startGame(String difficulty) {
        System.out.println("Starting game with difficulty: " + difficulty);
        stopAllSounds();
        playSoundOnce(gameStartMusic);

        // Hide difficulty buttons and the select difficulty label
        for (Component component : mainPanel.getComponents()) {
            if (component instanceof JButton || component instanceof JLabel) {
                component.setVisible(false);
            }
        }

        countdownTimer(5, difficulty);
    }

    private void countdownTimer(int seconds, String difficulty) {
        countdownLabel = new JLabel("", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Helvetica", Font.BOLD, 40));
        countdownLabel.setBounds(333, 384, 700, 50);
        mainPanel.add(countdownLabel, "Countdown");
        cardLayout.show(mainPanel, "Countdown");
        updateTimer(seconds, difficulty);
    }

    private void updateTimer(int seconds, String difficulty) {
        if (seconds >= 0) {
            countdownLabel.setText("Game will start in " + seconds + " seconds!");
            Timer countdownTimer = new Timer(1000, e -> updateTimer(seconds - 1, difficulty));
            countdownTimer.setRepeats(false);
            countdownTimer.start();
        } else {
            mainPanel.remove(countdownLabel);
            initializeGame(difficulty);
        }
    }

    private void initializeGame(String difficulty) {
        System.out.println("Game started with " + difficulty + " difficulty.");
        currentDifficulty = difficulty;
        totalQuestions = 20; // Set total questions here
        currentQuestion = 1;
        score = 0;
        generateQuestion();
        cardLayout.show(mainPanel, "GameScreen");
    }

    private void generateQuestion() {
        if (currentQuestion <= totalQuestions) {
            DifficultyLevel level = difficultyLevels.get(currentDifficulty);
            Random random = new Random();
            int num1 = random.nextInt(level.rangeEnd - level.rangeStart + 1) + level.rangeStart;
            int num2 = random.nextInt(level.rangeEnd - level.rangeStart + 1) + level.rangeStart;
            char operation = level.operations[random.nextInt(level.operations.length)];
            String question = num1 + " " + operation + " " + num2;
            if (currentDifficulty.equals("Hard")) {
                int num3 = random.nextInt(level.rangeEnd - level.rangeStart + 1) + level.rangeStart;
                char operation2 = level.operations[random.nextInt(level.operations.length)];
                question = "(" + num1 + " " + operation + " " + num2 + ") " + operation2 + " " + num3;
            }
            questionLabel.setText("Question " + currentQuestion + "/" + totalQuestions + ": " + question + " = ?");
            answerField.setText("");
            startTimer(level.timeLimit);
        } else {
            endQuiz();
        }
    }

    private void startTimer(int seconds) {
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(1000, new ActionListener() {
            int timeRemaining = seconds;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (timeRemaining > 0) {
                    timerLabel.setText("Time: " + timeRemaining + "s");
                    timeRemaining--;
                } else {
                    ((Timer) e.getSource()).stop();
                    checkAnswer();
                }
            }
        });
        timer.start();
    }

    private void checkAnswer() {
        if (timer != null) {
            timer.stop();
        }
        try {
            String question = questionLabel.getText().split(": ")[1].split(" = ")[0];
            double correctAnswer = eval(question);
            double userAnswer = Double.parseDouble(answerField.getText());
            if (userAnswer == correctAnswer) {
                score++;
                scoreLabel.setText("Score: " + score);
                playSound(correctSound);
                showCorrectAnimation();
            } else {
                playSound(incorrectSound);
                showIncorrectAnimation();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
        currentQuestion++;
        if (currentQuestion > totalQuestions) {
            endQuiz();
        } else {
            generateQuestion();
        }
    }

    private void showCorrectAnimation() {
        Timer animationTimer = new Timer(100, new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count % 2 == 0) {
                    answerField.setBackground(Color.GREEN);
                } else {
                    answerField.setBackground(originalAnswerFieldColor);
                }
                count++;
                if (count > 5) {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        animationTimer.start();
    }

    private void showIncorrectAnimation() {
        Timer animationTimer = new Timer(100, new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count % 2 == 0) {
                    answerField.setBackground(Color.RED);
                } else {
                    answerField.setBackground(originalAnswerFieldColor);
                }
                count++;
                if (count > 5) {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        animationTimer.start();
    }

    private void endQuiz() {
        stopAllSounds();
        playSoundOnce(gameFinishMusic);
        JOptionPane.showMessageDialog(this, "Quiz ended. Your score is " + score + "/" + totalQuestions, "Quiz Ended", JOptionPane.INFORMATION_MESSAGE);
        showMainMenu();
        stopAllSounds();
        playBackgroundMusic();
    }

    private double eval(String expression) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }

    private void showCloseConfirmation() {
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BrainTrain().setVisible(true));
    }

    private static class DifficultyLevel {
        int rangeStart;
        int rangeEnd;
        char[] operations;
        int timeLimit;

        DifficultyLevel(int rangeStart, int rangeEnd, char[] operations, int timeLimit) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.operations = operations;
            this.timeLimit = timeLimit;
        }
    }
}
