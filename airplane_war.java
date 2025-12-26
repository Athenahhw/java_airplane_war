import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class airplane_war extends JPanel implements ActionListener, KeyListener {
    // 遊戲狀態
    enum GameState { MENU, PLAYING, GAME_OVER }
    private GameState state = GameState.MENU;
    
    // 玩家飛機
    private double playerX = 400;
    private double playerY = 300;
    private double playerAngle = -Math.PI / 2; // 初始朝上
    private double playerSpeed = 0;
    private final double MAX_SPEED = 5;
    private final double ACCELERATION = 0.2;
    private final double ROTATION_SPEED = 0.1;
    
    // 遊戲物件列表
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Asteroid> asteroids = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    
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
    
    public airplane_war() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
    }
    
    private void startGame() {
        state = GameState.PLAYING;
        playerX = 400;
        playerY = 300;
        playerAngle = -Math.PI / 2;
        playerSpeed = 0;
        bullets.clear();
        asteroids.clear();
        enemies.clear();
        asteroidsDestroyed = 0;
        enemiesDestroyed = 0;
        gameStartTime = System.currentTimeMillis();
        
        // 生成初始隕石
        for (int i = 0; i < 8; i++) {
            spawnAsteroid();
        }
    }
    
    private void spawnAsteroid() {
        double x = random.nextDouble() * 800;
        double y = random.nextDouble() * 600;
        // 確保不在玩家附近生成
        while (Math.hypot(x - playerX, y - playerY) < 100) {
            x = random.nextDouble() * 800;
            y = random.nextDouble() * 600;
        }
        asteroids.add(new Asteroid(x, y));
    }
    
    private void spawnEnemy() {
        double x = random.nextBoolean() ? -20 : 820;
        double y = random.nextDouble() * 600;
        enemies.add(new Enemy(x, y));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }
    
    private void updateGame() {
        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        
        // 處理玩家移動
        if (leftPressed) playerAngle -= ROTATION_SPEED;
        if (rightPressed) playerAngle += ROTATION_SPEED;
        
        if (upPressed) {
            playerSpeed = Math.min(playerSpeed + ACCELERATION, MAX_SPEED);
        } else if (downPressed) {
            playerSpeed = Math.max(playerSpeed - ACCELERATION, -MAX_SPEED / 2);
        } else {
            playerSpeed *= 0.98; // 摩擦力
        }
        
        playerX += Math.cos(playerAngle) * playerSpeed;
        playerY += Math.sin(playerAngle) * playerSpeed;
        
        // 邊界處理（環繞）
        if (playerX < 0) playerX = 800;
        if (playerX > 800) playerX = 0;
        if (playerY < 0) playerY = 600;
        if (playerY > 600) playerY = 0;
        
        // 發射子彈
        if (spacePressed && !spacePreviouslyPressed) {
            bullets.add(new Bullet(playerX, playerY, playerAngle));
            playShootSound();
        }
        spacePreviouslyPressed = spacePressed;
        
        // 更新子彈
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update();
            if (b.isOutOfBounds()) {
                bullets.remove(i);
            }
        }
        
        // 更新隕石
        for (Asteroid a : asteroids) {
            a.update();
        }
        
        // 15秒後開始生成敵機
        if (elapsedTime > 15000 && random.nextInt(100) < 2) {
            spawnEnemy();
        }
        
        // 更新敵機
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(playerX, playerY);
            
            // 敵機發射子彈
            if (random.nextInt(100) < 1) {
                double angleToPlayer = Math.atan2(playerY - enemy.y, playerX - enemy.x);
                bullets.add(new Bullet(enemy.x, enemy.y, angleToPlayer, true));
            }
            
            // 移除離開螢幕的敵機
            if (enemy.isOutOfBounds()) {
                enemies.remove(i);
            }
        }
        
        // 碰撞檢測 - 子彈打隕石
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (b.isEnemy) continue;
            
            for (int j = asteroids.size() - 1; j >= 0; j--) {
                Asteroid a = asteroids.get(j);
                if (Math.hypot(b.x - a.x, b.y - a.y) < a.size) {
                    bullets.remove(i);
                    asteroids.remove(j);
                    asteroidsDestroyed++;
                    playHitSound();
                    spawnAsteroid(); // 生成新隕石
                    break;
                }
            }
        }
        
        // 碰撞檢測 - 子彈打敵機
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
                    playHitSound();
                    break;
                }
            }
        }
        
        // 碰撞檢測 - 玩家與隕石
        for (Asteroid a : asteroids) {
            if (Math.hypot(playerX - a.x, playerY - a.y) < a.size + 10) {
                gameOver();
                return;
            }
        }
        
        // 碰撞檢測 - 玩家與敵機
        for (Enemy enemy : enemies) {
            if (Math.hypot(playerX - enemy.x, playerY - enemy.y) < 20) {
                gameOver();
                return;
            }
        }
        
        // 碰撞檢測 - 玩家與敵方子彈
        for (Bullet b : bullets) {
            if (b.isEnemy && Math.hypot(playerX - b.x, playerY - b.y) < 10) {
                gameOver();
                return;
            }
        }
    }
    
    private void gameOver() {
        state = GameState.GAME_OVER;
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
        }
    }
    
    private void drawStars(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        random.setSeed(12345); // 固定種子讓星星位置固定
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(800);
            int y = random.nextInt(600);
            g2d.fillOval(x, y, 2, 2);
        }
        random.setSeed(System.currentTimeMillis());
    }
    
    private void drawMenu(Graphics2D g2d) {
        drawStars(g2d);
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String title = "SPACE BATTLE";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, (800 - titleWidth) / 2, 200);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        String instruction = "Press ENTER to Start";
        int instWidth = g2d.getFontMetrics().stringWidth(instruction);
        g2d.drawString(instruction, (800 - instWidth) / 2, 350);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String[] controls = {
            "Controls:",
            "Arrow Keys - Move and Rotate",
            "SPACE - Shoot"
        };
        for (int i = 0; i < controls.length; i++) {
            int width = g2d.getFontMetrics().stringWidth(controls[i]);
            g2d.drawString(controls[i], (800 - width) / 2, 450 + i * 30);
        }
    }
    
    private void drawGame(Graphics2D g2d) {
        // 繪製隕石
        g2d.setColor(Color.GRAY);
        for (Asteroid a : asteroids) {
            g2d.fillOval((int)(a.x - a.size), (int)(a.y - a.size), (int)(a.size * 2), (int)(a.size * 2));
        }
        
        // 繪製敵機
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
        
        // 繪製子彈
        for (Bullet b : bullets) {
            g2d.setColor(b.isEnemy ? Color.RED : Color.CYAN);
            g2d.fillOval((int)b.x - 3, (int)b.y - 3, 6, 6);
        }
        
        // 繪製玩家
        g2d.setColor(Color.YELLOW);
        AffineTransform old = g2d.getTransform();
        g2d.translate(playerX, playerY);
        g2d.rotate(playerAngle);
        int[] xPoints = {15, -10, -10};
        int[] yPoints = {0, -10, 10};
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setTransform(old);
        
        // 顯示分數
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString("Asteroids: " + asteroidsDestroyed, 10, 25);
        g2d.drawString("Enemies: " + enemiesDestroyed, 10, 50);
    }
    
    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        String gameOver = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(gameOver);
        g2d.drawString(gameOver, (800 - width) / 2, 200);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        String[] stats = {
            "Statistics:",
            "Asteroids Destroyed: " + asteroidsDestroyed,
            "Enemies Destroyed: " + enemiesDestroyed,
            "",
            "Press ENTER to Restart"
        };
        for (int i = 0; i < stats.length; i++) {
            int statWidth = g2d.getFontMetrics().stringWidth(stats[i]);
            g2d.drawString(stats[i], (800 - statWidth) / 2, 300 + i * 40);
        }
    }
    
    // 簡單的音效生成
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
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (state == GameState.MENU || state == GameState.GAME_OVER) {
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
    
    // 子彈類別
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
            return x < 0 || x > 800 || y < 0 || y > 600;
        }
    }
    
    // 隕石類別
    class Asteroid {
        double x, y, dx, dy, size;
        
        Asteroid(double x, double y) {
            this.x = x;
            this.y = y;
            this.size = 15 + random.nextInt(20);
            double speed = 0.5 + random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
        }
        
        void update() {
            x += dx;
            y += dy;
            if (x < -size) x = 800 + size;
            if (x > 800 + size) x = -size;
            if (y < -size) y = 600 + size;
            if (y > 600 + size) y = -size;
        }
    }
    
    // 敵機類別
    class Enemy {
        double x, y, angle;
        
        Enemy(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        void update(double targetX, double targetY) {
            angle = Math.atan2(targetY - y, targetX - x);
            x += Math.cos(angle) * 2;
            y += Math.sin(angle) * 2;
        }
        
        boolean isOutOfBounds() {
            return x < -50 || x > 850 || y < -50 || y > 650;
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