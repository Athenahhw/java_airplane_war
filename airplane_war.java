import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;

public class airplane_war extends JPanel implements ActionListener, KeyListener {
    // 遊戲狀態
    enum GameState { MENU, PLAYING, GAME_OVER, LEVEL_COMPLETE, WIN, CHOOSING_PATH }
    private GameState state = GameState.MENU;
    // 畫面尺寸
    private int screenWidth = 800;
    private int screenHeight = 600;
    // 玩家飛機
    private double playerX = 400;
    private double playerY = 300;
    private double playerAngle = -Math.PI / 2;
    private double playerSpeed = 0;
    private final double MAX_SPEED = 5;
    private final double ACCELERATION = 0.2;
    private final double ROTATION_SPEED = 0.1;
    // 遊戲物件列表
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Asteroid> asteroids = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Particle> particles = new ArrayList<>();
    private ArrayList<Point> stars = new ArrayList<>();
    // 按鍵狀態
    private boolean leftPressed, rightPressed, upPressed, downPressed, spacePressed;
    private boolean spacePreviouslyPressed = false;
    // 遊戲計時器
    private Timer gameTimer;
    private long gameStartTime;
    private Random random = new Random();
    // 統計數據
    private int asteroidsDestroyed = 0;
    private int enemiesDestroyed = 0;
    private int totalShots = 0;
    private int hits = 0;
    // 難度系統
    private double difficultyMultiplier = 1.0;
    // 關卡系統
    private int level = 1;
    private String[] planetNames = {"Neptune", "Uranus", "Saturn", "Jupiter", "Mars", "Earth", "Venus", "Mercury", "Sun"};
    private int[] planetSizes = {200, 200, 475, 560, 25, 50, 47, 19, 500}; // 相對直徑，地球=50
    private boolean targetVisible = false;
    private long levelStartTime;
    private ArrayList<String> collectedItems = new ArrayList<>();
    private ArrayList<SmallAsteroid> smallAsteroids = new ArrayList<>();
    private boolean choosingPath = false; // 對於地球的選擇
    // 音效
    public airplane_war() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }
    // 更新畫面尺寸
    private void updateScreenSize() {
        screenWidth = getWidth();
        screenHeight = getHeight();
    }
    // 開始遊戲
    private void startGame() {
        updateScreenSize();
        state = GameState.PLAYING;
        playerX = screenWidth / 2.0;
        playerY = screenHeight / 2.0;
        playerAngle = -Math.PI / 2;
        playerSpeed = 0;
        bullets.clear();
        asteroids.clear();
        enemies.clear();
        particles.clear();
        stars.clear();
        asteroidsDestroyed = 0;
        enemiesDestroyed = 0;
        totalShots = 0;
        hits = 0;
        difficultyMultiplier = 1.0 + (level - 1) * 0.1;
        levelStartTime = System.currentTimeMillis();
        targetVisible = false;
        choosingPath = false;
        smallAsteroids.clear();
        gameStartTime = System.currentTimeMillis();
        // 初始化星星
        for (int i = 0; i < 200; i++) {
            stars.add(new Point(random.nextInt(screenWidth), random.nextInt(screenHeight)));
        }
        // 生成遊戲物件
        int asteroidCount = 8 + (level - 1) * 1;
        for (int i = 0; i < asteroidCount; i++) {
            spawnAsteroid();
        }
        // 對於土星，添加密集的小衛星帶
        if (level == 3) {
            for (int i = 0; i < 50; i++) {
                smallAsteroids.add(new SmallAsteroid(random.nextInt(screenWidth), random.nextInt(screenHeight)));
            }
        }
    }
    // 生成遊戲物件
    private void spawnAsteroid() {
        double x = random.nextDouble() * screenWidth;
        double y = random.nextDouble() * screenHeight;
        while (Math.hypot(x - playerX, y - playerY) < 100) {
            x = random.nextDouble() * screenWidth;
            y = random.nextDouble() * screenHeight;
        }
        asteroids.add(new Asteroid(x, y));
    }
    // 更新遊戲
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            updateScreenSize();
            updateGame();
        }
        repaint();
    }
    // 更新遊戲
    private void updateGame() {
        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        // 難度系統
        if (leftPressed) playerAngle -= ROTATION_SPEED;
        if (rightPressed) playerAngle += ROTATION_SPEED;
        // 難度系統
        if (upPressed) {
            playerSpeed = Math.min(playerSpeed + ACCELERATION, MAX_SPEED);
        } else if (downPressed) {
            playerSpeed = Math.max(playerSpeed - ACCELERATION, -MAX_SPEED / 2);
        } else {
            playerSpeed *= 0.98;
        }
        // 難度系統
        playerX += Math.cos(playerAngle) * playerSpeed;
        playerY += Math.sin(playerAngle) * playerSpeed;
        
        if (playerX < 0) playerX = screenWidth;
        if (playerX > screenWidth) playerX = 0;
        if (playerY < 0) playerY = screenHeight;
        if (playerY > screenHeight) playerY = 0;
        
        if (spacePressed && !spacePreviouslyPressed) {
            bullets.add(new Bullet(playerX, playerY, playerAngle));
            totalShots++;
            playShootSound();
        }
        spacePreviouslyPressed = spacePressed;
        
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update();
            if (b.isOutOfBounds()) {
                bullets.remove(i);
            }
        }
        
        for (Asteroid a : asteroids) {
            a.update();
        }
        // 更新小隕石
        for (SmallAsteroid sa : smallAsteroids) {
            sa.update();
        }
        // 更新星星位置以模擬運動
        for (Point p : stars) {
            p.y += 1;
            if (p.y > screenHeight) {
                p.y = 0;
                p.x = random.nextInt(screenWidth);
            }
        }
        // 撞擊判定
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (b.isEnemy) continue;
            
            for (int j = asteroids.size() - 1; j >= 0; j--) {
                Asteroid a = asteroids.get(j);
                if (Math.hypot(b.x - a.x, b.y - a.y) < a.size) {
                    bullets.remove(i);
                    asteroids.remove(j);
                    asteroidsDestroyed++;
                    hits++;
                    createExplosion(a.x, a.y, Color.GRAY);
                    playHitSound();
                    if (a.level < 3) {
                        for (int k = 0; k < 2; k++) {
                            Asteroid small = new Asteroid(a.x + random.nextInt(20) - 10, a.y + random.nextInt(20) - 10, a.size * 0.6, a.level + 1);
                            small.dx = a.dx + random.nextDouble() * 2 - 1;
                            small.dy = a.dy + random.nextDouble() * 2 - 1;
                            asteroids.add(small);
                        }
                    } else {
                        spawnAsteroid();
                    }
                    break;
                }
            }
        }
        // 撞擊判定
        for (int i = bullets.size() - 1; i >= 0; i--) {
            if (i >= bullets.size()) continue;
            Bullet b = bullets.get(i);
            if (b.isEnemy) continue;
            
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                if (Math.hypot(b.x - enemy.x, b.y - enemy.y) < 15) {
                    bullets.remove(i);
                    enemies.remove(j);
                    enemiesDestroyed++;
                    hits++;
                    playHitSound();
                    break;
                }
            }
        }
        
        for (Asteroid a : asteroids) {
            if (Math.hypot(playerX - a.x, playerY - a.y) < a.size + 10) {
                createExplosion(playerX, playerY, new Color(61,89,171));
                gameOver();
                return;
            }
        }
        for (Bullet b : bullets) {
            if (b.isEnemy && Math.hypot(playerX - b.x, playerY - b.y) < 10) {
                createExplosion(playerX, playerY, new Color(61,89,171));
                gameOver();
                return;
            }
        }
        
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) {
                particles.remove(i);
            }
        }
        // 檢查是否到達目標星球
        if (targetVisible && playerY < 50) {
            levelComplete();
        }
        // 15秒後顯示終點
        if (!targetVisible && System.currentTimeMillis() - levelStartTime > 15000) {
            targetVisible = true;
        }
    }
    private void gameOver() {
        state = GameState.GAME_OVER;
        // 不重置level，讓玩家選擇
    }
    private void levelComplete() {
        // 添加特產
        if (level <= planetNames.length) {
            String item = getSouvenir(level);
            collectedItems.add(item);
        }
        
        if (level == 6) { // Earth
            choosingPath = true;
            state = GameState.CHOOSING_PATH;
        } else if (level >= planetNames.length - 1) {
            state = GameState.WIN;
        } else {
            state = GameState.LEVEL_COMPLETE;
            level++;
        }
    }
    private void createExplosion(double x, double y, Color color) {
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 3 + 1;
            particles.add(new Particle(x, y, angle, speed, color));
        }
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (state == GameState.MENU) {
            drawMenu(g2d);
        } else if (state == GameState.PLAYING) {
            drawStars(g2d);
            drawGame(g2d);
        } else if (state == GameState.GAME_OVER) {
            drawStars(g2d);
            drawGameOver(g2d);
        } else if (state == GameState.LEVEL_COMPLETE) {
            drawStars(g2d);
            drawLevelComplete(g2d);
        } else if (state == GameState.WIN) {
            drawStars(g2d);
            drawWin(g2d);
        } else if (state == GameState.CHOOSING_PATH) {
            drawStars(g2d);
            drawChoosingPath(g2d);
        }
    }
    private void drawStars(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        for (Point p : stars) {
            g2d.fillOval(p.x, p.y, 2, 2);
        }
    }
    
    private void drawMenu(Graphics2D g2d) {
        drawStars(g2d);
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(40, screenHeight / 10)));
        String title = "SPACE BATTLE";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, (screenWidth - titleWidth) / 2, screenHeight / 3);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(20, screenHeight / 20)));
        String instruction = "Press ENTER to Start";
        int instWidth = g2d.getFontMetrics().stringWidth(instruction);
        g2d.drawString(instruction, (screenWidth - instWidth) / 2, screenHeight / 2);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(16, screenHeight / 30)));
        String[] controls = {
            "Controls:",
            "Arrow Keys - Move and Rotate",
            "SPACE - Shoot"
        };
        int startY = screenHeight * 2 / 3;
        for (int i = 0; i < controls.length; i++) {
            int width = g2d.getFontMetrics().stringWidth(controls[i]);
            g2d.drawString(controls[i], (screenWidth - width) / 2, startY + i * 30);
        }
    }
    
    private void drawGame(Graphics2D g2d) {
        for (Particle p : particles) {
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int)(255 * p.life)));
            g2d.fillOval((int)p.x - 2, (int)p.y - 2, 4, 4);
        }   
        g2d.setColor(Color.GRAY);
        for (Asteroid a : asteroids) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(a.x, a.y);
            g2d.fill(a.shape);
            g2d.setTransform(old);
        }
        
        // 繪製小隕石
        g2d.setColor(Color.LIGHT_GRAY);
        for (SmallAsteroid sa : smallAsteroids) {
            g2d.fillOval((int)sa.x - 2, (int)sa.y - 2, 4, 4);
        }
        
        g2d.setColor(Color.RED);
        for (Enemy enemy : enemies) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(enemy.x, enemy.y);
            g2d.rotate(enemy.angle);
            int[] xPoints = {15, -10, -10};
            int[] yPoints = {0, -8, 8};
            g2d.fillPolygon(xPoints, yPoints, 3);
            g2d.setTransform(old);
        }
        
        for (Bullet b : bullets) {
            g2d.setColor(b.isEnemy ? Color.RED : Color.CYAN);
            g2d.fillOval((int)b.x - 3, (int)b.y - 3, 6, 6);
        }
        
        g2d.setColor(Color.YELLOW);
        AffineTransform old = g2d.getTransform();
        g2d.translate(playerX, playerY);
        g2d.rotate(playerAngle);
        int[] xPoints = {15, -10, -10};
        int[] yPoints = {0, -10, 10};
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setTransform(old);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Asteroids: " + asteroidsDestroyed, 10, 25);
        g2d.drawString("Enemies: " + enemiesDestroyed, 10, 50);
        g2d.drawString("Accuracy: " + (totalShots > 0 ? (hits * 100 / totalShots) : 0) + "%", 10, 75);
        g2d.drawString("Difficulty: x" + String.format("%.1f", difficultyMultiplier), 10, 100);
        g2d.drawString("Level: " + level + " - " + planetNames[level - 1], 10, 125);
        g2d.drawString("Target: " + (level < planetNames.length ? planetNames[level] : "Victory"), 10, 150);
        
        // 繪製目標星球 - 下半圓形在螢幕最上方
        if (targetVisible && level < planetNames.length) {
            int size = planetSizes[level];
            g2d.setColor(Color.YELLOW);
            g2d.fillArc(screenWidth / 2 - size / 2, -size / 2, size, size, 180, 180); // 下半圓形
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String targetName = planetNames[level];
            int nameWidth = g2d.getFontMetrics().stringWidth(targetName);
            g2d.drawString(targetName, screenWidth / 2 - nameWidth / 2, 20);
        }
    }
    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(40, screenHeight / 10)));
        String gameOver = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(gameOver);
        g2d.drawString(gameOver, (screenWidth - width) / 2, screenHeight / 3);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(20, screenHeight / 20)));
        String[] stats = {
            "Statistics:",
            "Asteroids Destroyed: " + asteroidsDestroyed,
            "Enemies Destroyed: " + enemiesDestroyed,
            "Total Shots: " + totalShots,
            "Accuracy: " + (totalShots > 0 ? (hits * 100 / totalShots) : 0) + "%",
            "Final Difficulty: x" + String.format("%.1f", difficultyMultiplier),
            "",
            "Press R to Restart from Level 1",
            "Press C to Continue from Level " + level
        };
        int startY = screenHeight / 2;
        for (int i = 0; i < stats.length; i++) {
            int statWidth = g2d.getFontMetrics().stringWidth(stats[i]);
            g2d.drawString(stats[i], (screenWidth - statWidth) / 2, startY + i * 40);
        }
    }
    
    private void drawLevelComplete(Graphics2D g2d) {
        g2d.setColor(Color.GREEN);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(40, screenHeight / 10)));
        String complete = "LEVEL " + (level - 1) + " COMPLETE!";
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(complete);
        g2d.drawString(complete, (screenWidth - width) / 2, screenHeight / 3);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(20, screenHeight / 20)));
        String next = "Press ENTER for Level " + level;
        int nextWidth = g2d.getFontMetrics().stringWidth(next);
        g2d.drawString(next, (screenWidth - nextWidth) / 2, screenHeight / 2);
    }
    
    private void drawWin(Graphics2D g2d) {
        g2d.setColor(new Color(255, 215, 0)); // Gold color
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(40, screenHeight / 10)));
        String win = "YOU WIN!";
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(win);
        g2d.drawString(win, (screenWidth - width) / 2, screenHeight / 3);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(20, screenHeight / 20)));
        String restart = "Press ENTER to Play Again";
        int restartWidth = g2d.getFontMetrics().stringWidth(restart);
        g2d.drawString(restart, (screenWidth - restartWidth) / 2, screenHeight / 2);
        
        // 顯示收集的物品
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Collected Souvenirs:", (screenWidth - g2d.getFontMetrics().stringWidth("Collected Souvenirs:")) / 2, screenHeight / 2 + 50);
        for (int i = 0; i < collectedItems.size(); i++) {
            g2d.drawString("- " + collectedItems.get(i), screenWidth / 2 - 100, screenHeight / 2 + 80 + i * 20);
        }
    }
    
    private void drawChoosingPath(Graphics2D g2d) {
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(30, screenHeight / 15)));
        String choose = "Choose Your Path!";
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(choose);
        g2d.drawString(choose, (screenWidth - width) / 2, screenHeight / 4);
        
        g2d.setColor(Color.GREEN);
        g2d.fillOval(screenWidth / 4 - 50, screenHeight / 2 - 50, 100, 100);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Earth", screenWidth / 4 - 20, screenHeight / 2 + 5);
        
        g2d.setColor(Color.GRAY);
        g2d.fillOval(3 * screenWidth / 4 - 50, screenHeight / 2 - 50, 100, 100);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Moon", 3 * screenWidth / 4 - 15, screenHeight / 2 + 5);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String instruction = "Press 1 for Earth, 2 for Moon";
        int instWidth = g2d.getFontMetrics().stringWidth(instruction);
        g2d.drawString(instruction, (screenWidth - instWidth) / 2, 3 * screenHeight / 4);
    }
    
    private void playShootSound() {
        playSound(800, 50);
    }
    
    private void playHitSound() {
        playSound(400, 100);
    }
    
    private void playSound(final int frequency, final int duration) {
        new Thread(() -> {
            try {
                byte[] buf = new byte[1];
                AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                for (int i = 0; i < duration * 8; i++) {
                    double angle = i / (8000f / frequency) * 2.0 * Math.PI;
                    buf[0] = (byte)(Math.sin(angle) * 80.0);
                    sdl.write(buf, 0, 1);
                }
                sdl.drain();
                sdl.stop();
                sdl.close();
            } catch (Exception e) {}
        }).start();
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (state == GameState.CHOOSING_PATH) {
            if (e.getKeyCode() == KeyEvent.VK_1) {
                // 選擇地球
                collectedItems.add("Earth Souvenir");
                level++;
                startGame();
            } else if (e.getKeyCode() == KeyEvent.VK_2) {
                // 選擇月球，拜訪嫦娥
                collectedItems.add("Moon Chang'e Pendant");
                collectedItems.add("Lunar Jade");
                level++;
                startGame();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (state == GameState.MENU || state == GameState.LEVEL_COMPLETE || state == GameState.WIN) {
                if (state == GameState.WIN) level = 1; // 勝利後重置關卡
                startGame();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_R) {
            if (state == GameState.GAME_OVER) {
                level = 1;
                startGame();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_C) {
            if (state == GameState.GAME_OVER) {
                startGame();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) spacePressed = true;
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) spacePressed = false;
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    class Bullet {
        double x, y, dx, dy;
        boolean isEnemy;
        
        Bullet(double x, double y, double angle) {
            this(x, y, angle, false);
        }
        
        Bullet(double x, double y, double angle, boolean isEnemy) {
            this.x = x;
            this.y = y;
            this.isEnemy = isEnemy;
            double speed = 8;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
        }
        
        void update() {
            x += dx;
            y += dy;
        }
        
        boolean isOutOfBounds() {
            return x < 0 || x > screenWidth || y < 0 || y > screenHeight;
        }
    }
    
    class Asteroid {
        double x, y, dx, dy, size;
        int level;
        Path2D shape;
        
        Asteroid(double x, double y) {
            this(x, y, 15 + random.nextInt(20), 1);
        }
        
        Asteroid(double x, double y, double size, int level) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.level = level;
            double speed = 0.5 + random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
            
            // 生成不規則形狀
            shape = new Path2D.Double();
            int points = 6 + random.nextInt(4);
            double angleStep = 2 * Math.PI / points;
            for (int i = 0; i < points; i++) {
                double ang = i * angleStep;
                double radius = size * (0.5 + random.nextDouble() * 0.5);
                double px = Math.cos(ang) * radius;
                double py = Math.sin(ang) * radius;
                if (i == 0) shape.moveTo(px, py);
                else shape.lineTo(px, py);
            }
            shape.closePath();
        }
        
        void update() {
            x += dx;
            y += dy;
            if (x < -size) x = screenWidth + size;
            if (x > screenWidth + size) x = -size;
            if (y < -size) y = screenHeight + size;
            if (y > screenHeight + size) y = -size;
        }
    }
    
    class Enemy {
        double x, y, angle;
        
        Enemy(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        void update(double targetX, double targetY) {
            angle = Math.atan2(targetY - y, targetX - x);
            x += Math.cos(angle) * 2 * difficultyMultiplier;
            y += Math.sin(angle) * 2 * difficultyMultiplier;
        }
        
        boolean isOutOfBounds() {
            return x < -50 || x > screenWidth + 50 || y < -50 || y > screenHeight + 50;
        }
    }
    
    class SmallAsteroid {
        double x, y, dx, dy;
        
        SmallAsteroid(double x, double y) {
            this.x = x;
            this.y = y;
            double speed = 0.2 + random.nextDouble() * 0.3;
            double angle = random.nextDouble() * Math.PI * 2;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
        }
        
        void update() {
            x += dx;
            y += dy;
            if (x < -5) x = screenWidth + 5;
            if (x > screenWidth + 5) x = -5;
            if (y < -5) y = screenHeight + 5;
            if (y > screenHeight + 5) y = -5;
        }
    }
    
    class Particle {
        double x, y, dx, dy, life;
        Color color;
        
        Particle(double x, double y, double angle, double speed, Color color) {
            this.x = x;
            this.y = y;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
            this.life = 1.0;
            this.color = color;
        }
        
        void update() {
            x += dx;
            y += dy;
            dx *= 0.95;
            dy *= 0.95;
            life -= 0.02;
        }
        
        boolean isDead() {
            return life <= 0;
        }
    }
    
    private Color getBackgroundColor(int level) {
        switch (level) {
            case 1: return new Color(0, 0, 100); // Neptune - blue
            case 2: return new Color(0, 100, 100); // Uranus - cyan
            case 3: return new Color(200, 150, 50); // Saturn - golden
            case 4: return new Color(150, 75, 0); // Jupiter - brown
            case 5: return new Color(200, 50, 50); // Mars - red
            case 6: return new Color(50, 100, 200); // Earth - blue-green
            case 7: return new Color(200, 150, 100); // Venus - yellow-brown
            case 8: return new Color(150, 150, 150); // Mercury - gray
            case 9: return new Color(255, 200, 0); // Sun - yellow
            default: return Color.BLACK;
        }
    }
    
    private String getSouvenir(int level) {
        switch (level) {
            case 1: return "Neptune's Trident";
            case 2: return "Uranus Ice Crystal";
            case 3: return "Saturn's Ring Fragment";
            case 4: return "Jupiter's Storm Eye";
            case 5: return "Mars Rover Model";
            case 6: return "Earth Soil Sample";
            case 7: return "Venus Flower";
            case 8: return "Mercury Meteorite";
            case 9: return "Solar Flare";
            default: return "Space Junk";
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Space Battle");
            airplane_war game = new airplane_war();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}