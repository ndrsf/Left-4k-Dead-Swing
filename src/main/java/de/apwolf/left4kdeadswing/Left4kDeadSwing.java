package de.apwolf.left4kdeadswing;

import javax.swing.JFrame;
import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

/**
 * Original code by Markus "Notch" Petersson
 *
 * Controls: ASDF for movement, R for reloading, left mouse for shooting<br>
 *
 */
public class Left4kDeadSwing extends Applet implements Runnable {
    private final boolean[] k = new boolean[32767];
    private int m;

    public static void main(String[] args) {
        Left4kDeadSwing game = new Left4kDeadSwing();
        game.start();

        JFrame window = new JFrame("Left4kDead");
        window.setContentPane(game);
        window.setSize(494, 514); // this is pixel perfect
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null); // center to screen
        window.setVisible(true);
    }

    @Override
    public void start() {
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            // Sleeping for a bit avoids a race condition where the Swing part is done before the Applet part which
            // would lead on a NullPointerException when drawing the game
            Thread.sleep(150L);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Impossibruh!", e);
        }

        BufferedImage image = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
        Graphics ogr = image.getGraphics();

        Random random = new Random();
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int[] sprites = new int[18 * 4 * 16 * 12 * 12];
        int pix = 0;
        for (int i = 0; i < 18; i++) {
            int skin = 0xFF9993;
            int clothes = 0xFFffff;

            if (i > 0) {
                skin = 0xa0ff90;
                clothes = (random.nextInt(0x1000000) & 0x7f7f7f);
            }
            for (int t = 0; t < 4; t++) {
                for (int d = 0; d < 16; d++) {
                    double dir = d * Math.PI * 2 / 16.0;

                    if (t == 1) dir += 0.5 * Math.PI * 2 / 16.0;
                    if (t == 3) dir -= 0.5 * Math.PI * 2 / 16.0;

                    double cos = Math.cos(dir);
                    double sin = Math.sin(dir);

                    for (int y = 0; y < 12; y++) {
                        int col = 0x000000;
                        for (int x = 0; x < 12; x++) {
                            int xPix = (int) (cos * (x - 6) + sin * (y - 6) + 6.5);
                            int yPix = (int) (cos * (y - 6) - sin * (x - 6) + 6.5);

                            if (i == 17) {
                                if (xPix > 3 && xPix < 9 && yPix > 3 && yPix < 9) {
                                    col = 0xff0000 + (t & 1) * 0xff00;
                                }
                            } else {
                                if (t == 1 && xPix > 1 && xPix < 4 && yPix > 3 && yPix < 8) col = skin;
                                if (t == 3 && xPix > 8 && xPix < 11 && yPix > 3 && yPix < 8) col = skin;

                                if (xPix > 1 && xPix < 11 && yPix > 5 && yPix < 8) {
                                    col = clothes;
                                }
                                if (xPix > 4 && xPix < 8 && yPix > 4 && yPix < 8) {
                                    col = skin;
                                }
                            }
                            sprites[pix++] = col;
                            if (col > 1) {
                                col = 1;
                            } else {
                                col = 0;
                            }
                        }
                    }
                }
            }
        }

        int score = 0;
        int hurtTime = 0;
        int bonusTime = 0;
        int xWin0 = 0;
        int yWin0 = 0;
        int xWin1 = 0;
        int yWin1 = 0;

        restart:
        while (true) {
            boolean gameStarted = false;
            int level = 0;
            int shootDelay = 0;
            int rushTime = 150;
            int damage = 20;
            int ammo = 20;
            int clips = 20;

            winLevel:
            while (true) {
                int tick = 0;
                level++;
                int[] map = new int[1024 * 1024];
                random = new Random(4329 + level);

                int[] monsterData = new int[320 * 16];
                {
                    int i = 0;
                    for (int y = 0; y < 1024; y++)
                        for (int x = 0; x < 1024; x++) {
                            int br = random.nextInt(32) + 112;
                            map[i] = (br / 3) << 16 | (br) << 8;
                            if (x < 4 || y < 4 || x >= 1020 || y >= 1020) {
                                map[i] = 0xFFFEFE;
                            }
                            i++;
                        }

                    for (i = 0; i < 70; i++) {
                        int w = random.nextInt(8) + 2;
                        int h = random.nextInt(8) + 2;
                        int xm = random.nextInt(64 - w - 2) + 1;
                        int ym = random.nextInt(64 - h - 2) + 1;

                        w *= 16;
                        h *= 16;

                        w += 5;
                        h += 5;
                        xm *= 16;
                        ym *= 16;

                        if (i == 68) {
                            monsterData[0] = xm + w / 2;
                            monsterData[1] = ym + h / 2;
                            monsterData[15] = 0x808080;
                            monsterData[11] = 1;
                        }

                        xWin0 = xm + 5;
                        yWin0 = ym + 5;
                        xWin1 = xm + w - 5;
                        yWin1 = ym + h - 5;
                        for (int y = ym; y < ym + h; y++)
                            for (int x = xm; x < xm + w; x++) {
                                int d = x - xm;
                                if (xm + w - x - 1 < d) d = xm + w - x - 1;
                                if (y - ym < d) d = y - ym;
                                if (ym + h - y - 1 < d) d = ym + h - y - 1;

                                map[x + y * 1024] = 0xFF8052;
                                if (d > 4) {
                                    int br = random.nextInt(16) + 112;
                                    if (((x + y) & 3) == 0) {
                                        br += 16;
                                    }
                                    map[x + y * 1024] = (br * 3 / 3) << 16 | (br * 4 / 4) << 8 | (br * 4 / 4);
                                }
                                if (i == 69) {
                                    map[x + y * 1024] &= 0xff0000;
                                }
                            }

                        for (int j = 0; j < 2; j++) {
                            int xGap = random.nextInt(w - 24) + xm + 5;
                            int yGap = random.nextInt(h - 24) + ym + 5;
                            int ww = 5;
                            int hh = 5;

                            xGap = xGap / 16 * 16 + 5;
                            yGap = yGap / 16 * 16 + 5;
                            if (random.nextInt(2) == 0) {
                                xGap = xm + (w - 5) * random.nextInt(2);
                                hh = 11;
                            } else {
                                ww = 11;
                                yGap = ym + (h - 5) * random.nextInt(2);
                            }
                            for (int y = yGap; y < yGap + hh; y++)
                                for (int x = xGap; x < xGap + ww; x++) {
                                    int br = random.nextInt(32) + 112 - 64;
                                    map[x + y * 1024] = (br * 3 / 3) << 16 | (br * 4 / 4) << 8 | (br * 4 / 4);
                                }
                        }
                    }

                    for (int y = 1; y < 1024 - 1; y++)
                        inloop:for (int x = 1; x < 1024 - 1; x++) {
                            for (int xx = x - 1; xx <= x + 1; xx++)
                                for (int yy = y - 1; yy <= y + 1; yy++)
                                    if (map[xx + yy * 1024] < 0xff0000) continue inloop;

                            map[x + y * 1024] = 0xffffff;
                        }
                }

                long lastTime = System.nanoTime();

                int[] lightmap = new int[240 * 240];
                int[] brightness = new int[512];

                double offs = 30;
                double playerDir = 0;
                for (int i = 0; i < 512; i++) {
                    brightness[i] = (int) (255.0 * offs / (i + offs));
                    if (i < 4) brightness[i] = brightness[i] * i / 4;
                }

                Graphics sg = getGraphics();
                random = new Random();
                while (true) {
                    if (gameStarted) {
                        tick++;
                        rushTime++;

                        if (rushTime >= 150) {
                            rushTime = -random.nextInt(2000);
                        }
                        // Move player:
                        int mouse = m;
                        playerDir = Math.atan2(mouse / 240 - 120, mouse % 240 - 120);

                        double shootDir = playerDir + (random.nextInt(100) - random.nextInt(100)) / 100.0 * 0.2;
                        double cos = Math.cos(-shootDir);
                        double sin = Math.sin(-shootDir);

                        int xCam = monsterData[0];
                        int yCam = monsterData[1];

                        for (int i = 0; i < 960; i++) {
                            int xt = i % 240 - 120;
                            int yt = (i / 240 % 2) * 239 - 120;

                            if (i >= 480) {
                                int tmp = xt;
                                xt = yt;
                                yt = tmp;
                            }

                            double dd = Math.atan2(yt, xt) - playerDir;
                            if (dd < -Math.PI) dd += Math.PI * 2;
                            if (dd >= Math.PI) dd -= Math.PI * 2;

                            int brr = (int) ((1 - dd * dd) * 255);

                            int dist = 120;
                            if (brr < 0) {
                                brr = 0;
                                dist = 32;
                            }
                            if (tick < 60) brr = brr * tick / 60;

                            int j = 0;
                            for (; j < dist; j++) {
                                int xx = xt * j / 120 + 120;
                                int yy = yt * j / 120 + 120;
                                int xm = xx + xCam - 120;
                                int ym = yy + yCam - 120;

                                if (map[(xm + ym * 1024) & (1024 * 1024 - 1)] == 0xffffff) break;

                                int xd = (xx - 120) * 256 / 120;
                                int yd = (yy - 120) * 256 / 120;

                                int ddd = (xd * xd + yd * yd) / 256;
                                int br = brightness[ddd] * brr / 255;

                                if (ddd < 16) {
                                    int tmp = 128 * (16 - ddd) / 16;
                                    br = br + tmp * (255 - br) / 255;
                                }

                                lightmap[xx + yy * 240] = br;
                            }
                        }
                        for (int y = 0; y < 240; y++) {
                            int xm = xCam - 120;
                            int ym = y + yCam - 120;
                            for (int x = 0; x < 240; x++) {
                                pixels[x + y * 240] = map[(xm + x + ym * 1024) & (1024 * 1024 - 1)];
                            }
                        }

                        int closestHitDist = 0;
                        for (int j = 0; j < 250; j++) {
                            int xm = xCam + (int) (cos * j / 2);
                            int ym = yCam - (int) (sin * j / 2);
                            if (map[(xm + ym * 1024) & (1024 * 1024 - 1)] == 0xffffff) break;
                            closestHitDist = j / 2;
                        }

                        boolean shoot = shootDelay-- < 0 && k[1];

                        {
                            int closestHit = 0;

                            nextMonster:
                            for (int m = 0; m < 256 + 16; m++) {
                                int xPos = monsterData[m * 16 + 0];
                                int yPos = monsterData[m * 16 + 1];
                                if (monsterData[m * 16 + 11] == 0) {
                                    xPos = (random.nextInt(62) + 1) * 16 + 8;
                                    yPos = (random.nextInt(62) + 1) * 16 + 8;

                                    int xd = xCam - xPos;
                                    int yd = yCam - yPos;

                                    if (xd * xd + yd * yd < 180 * 180) {
                                        xPos = 1;
                                        yPos = 1;
                                    }

                                    if (map[xPos + yPos * 1024] < 0xfffffe && (m <= 128 || rushTime > 0 || (m > 255 && tick == 1))) {
                                        monsterData[m * 16 + 0] = xPos;
                                        monsterData[m * 16 + 1] = yPos;
                                        monsterData[m * 16 + 15] = map[xPos + yPos * 1024];
                                        map[xPos + yPos * 1024] = 0xfffffe;
                                        monsterData[m * 16 + 9] = (rushTime > 0 || random.nextInt(3) == 0) ? 127 : 0;
                                        monsterData[m * 16 + 11] = 1;
                                        monsterData[m * 16 + 2] = m & 15;
                                    } else {
                                        continue;
                                    }
                                } else {
                                    int xd = xPos - xCam;
                                    int yd = yPos - yCam;

                                    if (m >= 255) {
                                        if (xd * xd + yd * yd < 8 * 8) {
                                            map[xPos + yPos * 1024] = monsterData[m * 16 + 15];
                                            monsterData[m * 16 + 11] = 0;
                                            bonusTime = 120;
                                            if ((m & 1) == 0) {
                                                damage = 20;
                                            } else {
                                                clips = 20;
                                            }
                                            continue;
                                        }
                                    } else if (xd * xd + yd * yd > 340 * 340) {
                                        map[xPos + yPos * 1024] = monsterData[m * 16 + 15];
                                        monsterData[m * 16 + 11] = 0;
                                        continue;
                                    }
                                }


                                int xm = xPos - xCam + 120;
                                int ym = monsterData[m * 16 + 1] - yCam + 120;

                                int d = monsterData[m * 16 + 2];
                                if (m == 0) {
                                    d = (((int) (playerDir / (Math.PI * 2) * 16 + 4.5 + 16)) & 15);
                                }

                                d += ((monsterData[m * 16 + 3] / 4) & 3) * 16;

                                int p = (0 * 16 + d) * 144;
                                if (m > 0) {
                                    p += ((m & 15) + 1) * 144 * 16 * 4;
                                }

                                if (m > 255) {
                                    p = (17 * 4 * 16 + ((m & 1) * 16 + (tick & 15))) * 144;
                                }

                                for (int y = ym - 6; y < ym + 6; y++)
                                    for (int x = xm - 6; x < xm + 6; x++) {
                                        int c = sprites[p++];
                                        if (c > 0 && x >= 0 && y >= 0 && x < 240 && y < 240) {
                                            pixels[x + y * 240] = c;
                                        }
                                    }


                                boolean moved = false;

                                if (monsterData[m * 16 + 10] > 0) {
                                    monsterData[m * 16 + 11] += random.nextInt(3) + 1;
                                    monsterData[m * 16 + 10] = 0;

                                    double rot = 0.25;
                                    int amount = 8;
                                    double poww = 32;


                                    if (monsterData[m * 16 + 11] >= 2 + level) {
                                        rot = Math.PI * 2;
                                        amount = 60;
                                        poww = 16;
                                        map[(xPos) + (yPos) * 1024] = 0xa00000;
                                        monsterData[m * 16 + 11] = 0;
                                        score += level;
                                    }
                                    for (int i = 0; i < amount; i++) {
                                        double pow = (random.nextInt(100) * random.nextInt(100)) * poww / 10000 + 4;
                                        double dir = (random.nextInt(100) - random.nextInt(100)) / 100.0 * rot;
                                        double xdd = (Math.cos(playerDir + dir) * pow) + random.nextInt(4) - random.nextInt(4);
                                        double ydd = (Math.sin(playerDir + dir) * pow) + random.nextInt(4) - random.nextInt(4);
                                        int col = (random.nextInt(128) + 120);
                                        bloodLoop:
                                        for (int j = 2; j < pow; j++) {
                                            int xd = (int) (xPos + xdd * j / pow);
                                            int yd = (int) (yPos + ydd * j / pow);
                                            int pp = ((xd) + (yd) * 1024) & (1024 * 1024 - 1);
                                            if (map[pp] >= 0xff0000) break bloodLoop;
                                            if (random.nextInt(2) != 0) {
                                                map[pp] = col << 16;
                                                col = col * 8 / 9;
                                            }
                                        }
                                    }

                                    continue nextMonster;
                                }

                                int xPlayerDist = xCam - xPos;
                                int yPlayerDist = yCam - yPos;

                                if (m <= 255) {
                                    double rx = -(cos * xPlayerDist - sin * yPlayerDist);
                                    double ry = cos * yPlayerDist + sin * xPlayerDist;

                                    if (rx > -6 && rx < 6 && ry > -6 && ry < 6 && m > 0) {
                                        damage++;
                                        hurtTime += 20;
                                    }
                                    if (rx > -32 && rx < 220 && ry > -32 && ry < 32 && random.nextInt(10) == 0) {
                                        monsterData[m * 16 + 9]++;
                                    }
                                    if (rx > 0 && rx < closestHitDist && ry > -8 && ry < 8) {
                                        closestHitDist = (int) (rx);
                                        closestHit = m;
                                    }

                                    dirLoop:
                                    for (int i = 0; i < 2; i++) {
                                        int xa = 0;
                                        int ya = 0;
                                        xPos = monsterData[m * 16 + 0];
                                        yPos = monsterData[m * 16 + 1];

                                        if (m == 0) {
                                            if (k[KeyEvent.VK_A]) xa--;
                                            if (k[KeyEvent.VK_D]) xa++;
                                            if (k[KeyEvent.VK_W]) ya--;
                                            if (k[KeyEvent.VK_S]) ya++;
                                        } else {
                                            if (monsterData[m * 16 + 9] < 8) continue nextMonster;

                                            if (monsterData[m * 16 + 8] != 12) {
                                                xPlayerDist = (monsterData[m * 16 + 8]) % 5 - 2;
                                                yPlayerDist = (monsterData[m * 16 + 8]) / 5 - 2;
                                                if (random.nextInt(10) == 0) {
                                                    monsterData[m * 16 + 8] = 12;
                                                }
                                            }

                                            double xxd = Math.sqrt(xPlayerDist * xPlayerDist);
                                            double yyd = Math.sqrt(yPlayerDist * yPlayerDist);
                                            if (random.nextInt(1024) / 1024.0 < yyd / xxd) {
                                                if (yPlayerDist < 0) ya--;
                                                if (yPlayerDist > 0) ya++;
                                            }
                                            if (random.nextInt(1024) / 1024.0 < xxd / yyd) {
                                                if (xPlayerDist < 0) xa--;
                                                if (xPlayerDist > 0) xa++;
                                            }

                                            moved = true;
                                            double dir = Math.atan2(yPlayerDist, xPlayerDist);
                                            monsterData[m * 16 + 2] = (((int) (dir / (Math.PI * 2) * 16 + 4.5 + 16)) & 15);
                                        }

                                        ya *= i;
                                        xa *= 1 - i;

                                        if (xa != 0 || ya != 0) {
                                            map[xPos + yPos * 1024] = monsterData[m * 16 + 15];
                                            for (int xx = xPos + xa - 3; xx <= xPos + xa + 3; xx++)
                                                for (int yy = yPos + ya - 3; yy <= yPos + ya + 3; yy++)
                                                    if (map[xx + yy * 1024] >= 0xfffffe) {
                                                        map[xPos + yPos * 1024] = 0xfffffe;
                                                        monsterData[m * 16 + 8] = random.nextInt(25);
                                                        continue dirLoop;
                                                    }

                                            moved = true;
                                            monsterData[m * 16 + 0] += xa;
                                            monsterData[m * 16 + 1] += ya;
                                            monsterData[m * 16 + 15] = map[(xPos + xa) + (yPos + ya) * 1024];
                                            map[(xPos + xa) + (yPos + ya) * 1024] = 0xfffffe;
                                        }
                                    }
                                    if (moved) {
                                        monsterData[m * 16 + 3]++;
                                    }
                                }
                            }

                            if (shoot) {
                                if (ammo >= 220) {
                                    shootDelay = 2;
                                    k[1] = false;
                                } else {
                                    shootDelay = 1;
                                    ammo += 4;
                                }
                                if (closestHit > 0) {
                                    monsterData[closestHit * 16 + 10] = 1;
                                    monsterData[closestHit * 16 + 9] = 127;
                                }
                                int glow = 0;
                                for (int j = closestHitDist; j >= 0; j--) {
                                    int xm = +(int) (cos * j) + 120;
                                    int ym = -(int) (sin * j) + 120;
                                    if (xm > 0 && ym > 0 && xm < 240 && ym < 240) {
                                        if (random.nextInt(20) == 0 || j == closestHitDist) {
                                            pixels[xm + ym * 240] = 0xffffff;
                                            glow = 200;
                                        }
                                        lightmap[xm + ym * 240] += glow * (255 - lightmap[xm + ym * 240]) / 255;
                                    }
                                    glow = glow * 20 / 21;
                                }

                                if (closestHitDist < 120) {
                                    closestHitDist -= 3;
                                    int xx = (int) (120 + cos * closestHitDist);
                                    int yy = (int) (120 - sin * closestHitDist);

                                    for (int x = -12; x <= 12; x++) {
                                        for (int y = -12; y <= 12; y++) {
                                            int xd = xx + x;
                                            int yd = yy + y;
                                            if (xd >= 0 && yd >= 0 && xd < 240 && yd < 240) {
                                                lightmap[xd + yd * 240] += 2000 / (x * x + y * y + 10) * (255 - lightmap[xd + yd * 240]) / 255;
                                            }
                                        }
                                    }

                                    for (int i = 0; i < 10; i++) {
                                        double pow = random.nextInt(100) * random.nextInt(100) * 8.0 / 10000;
                                        double dir = (random.nextInt(100) - random.nextInt(100)) / 100.0;
                                        int xd = (int) (xx - Math.cos(playerDir + dir) * pow) + random.nextInt(4) - random.nextInt(4);
                                        int yd = (int) (yy - Math.sin(playerDir + dir) * pow) + random.nextInt(4) - random.nextInt(4);
                                        if (xd >= 0 && yd >= 0 && xd < 240 && yd < 240) {
                                            if (closestHit > 0) {
                                                pixels[xd + yd * 240] = 0xff0000;
                                            } else {
                                                pixels[xd + yd * 240] = 0xcacaca;
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        if (damage >= 220) {
                            k[1] = false;
                            hurtTime = 255;
                            continue restart;
                        }
                        if (k[KeyEvent.VK_R] && ammo > 20 && clips < 220) {
                            shootDelay = 30;
                            ammo = 20;
                            clips += 10;
                        }

                        if (xCam > xWin0 && xCam < xWin1 && yCam > yWin0 && yCam < yWin1) {
                            continue winLevel;
                        }
                    }

                    bonusTime = bonusTime * 8 / 9;
                    hurtTime /= 2;

                    for (int y = 0; y < 240; y++) {
                        for (int x = 0; x < 240; x++) {
                            int noise = random.nextInt(16) * random.nextInt(16) / 16;
                            if (!gameStarted) noise *= 4;

                            int c = pixels[x + y * 240];
                            int l = lightmap[x + y * 240];
                            lightmap[x + y * 240] = 0;
                            int r = ((c >> 16) & 0xff) * l / 255 + noise;
                            int g = ((c >> 8) & 0xff) * l / 255 + noise;
                            int b = ((c) & 0xff) * l / 255 + noise;

                            r = r * (255 - hurtTime) / 255 + hurtTime;
                            g = g * (255 - bonusTime) / 255 + bonusTime;
                            pixels[x + y * 240] = r << 16 | g << 8 | b;
                        }
                        if (y % 2 == 0 && (y >= damage && y < 220)) {
                            for (int x = 232; x < 238; x++) {
                                pixels[y * 240 + x] = 0x800000;
                            }
                        }
                        if (y % 2 == 0 && (y >= ammo && y < 220)) {
                            for (int x = 224; x < 230; x++) {
                                pixels[y * 240 + x] = 0x808000;
                            }
                        }
                        if (y % 10 < 9 && (y >= clips && y < 220)) {
                            for (int x = 221; x < 222; x++) {
                                pixels[y * 240 + 221] = 0xffff00;
                            }
                        }
                    }

                    ogr.drawString("" + score, 4, 232);
                    if (!gameStarted) {
                        ogr.drawString("Left 4k Dead", 80, 70);
                        if (k[1] && hurtTime == 0) {
                            score = 0;
                            gameStarted = true;
                            k[1] = false;
                        }
                    } else if (tick < 60) {
                        ogr.drawString("Level " + level, 90, 70);
                    }

                    // Getting a NullPointerException right here? Increase the startup delay at the beginning!
                    sg.drawImage(image, 0, 0, 480, 480, 0, 0, 240, 240, null);
                    do {
                        Thread.yield();
                    }
                    while (System.nanoTime() - lastTime < 0);

                    lastTime += (1000000000 / 30);
                }
            }
        }
    }

    @Override
    public void processEvent(AWTEvent e) {
        boolean down = false;
        switch (e.getID()) {
            case KeyEvent.KEY_PRESSED:
                down = true;
            case KeyEvent.KEY_RELEASED:
                k[((KeyEvent) e).getKeyCode()] = down;
                break;
            case MouseEvent.MOUSE_PRESSED:
                down = true;
            case MouseEvent.MOUSE_RELEASED:
                k[((MouseEvent) e).getButton()] = down;
            case MouseEvent.MOUSE_MOVED:
            case MouseEvent.MOUSE_DRAGGED:
                m = ((MouseEvent) e).getX() / 2 + ((MouseEvent) e).getY() / 2 * 240;
        }
    }
}

