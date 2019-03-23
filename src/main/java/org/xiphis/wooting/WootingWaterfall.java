/*
 * Copyright 2019 Antony T Curtis <atcurtis@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.xiphis.wooting;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Random;


public class WootingWaterfall {


  final int xsize = 48;
  final int ysize = 32;

  final WootingRGB.RGB[] palette;
  final int[][] fire = new int[ysize][xsize];
  private final WootingRGB wooting;
  private static final OperatingSystemMXBean OSMBEAN = ManagementFactory.getOperatingSystemMXBean();

  public WootingWaterfall(WootingRGB wooting) {
    this.wooting = wooting;
    this.palette = new WootingRGB.RGB[512];
    for (int i = 0; i < 256; i++) {
      palette[i] = WootingRGB.RGB.of(Math.min(255, i * 2), Math.max(0, (i - 128) * 2), 0);
    }
    for (int i = 0; i < 256; i++) {
      palette[i + 256] = WootingRGB.RGB.of(Math.min(255, 512 - i * 2), Math.min(255, 512 - i * 2), i);
    }
  }

  private int get(int x, int y) {
    return fire[Math.min(Math.max(y, 0), ysize - 1)][x % xsize];
  }

  private void convolveFlame() {
    for (int y = ysize - 1; y > 0; y--) {
      for (int x = xsize - 1; x >= 0; x--) {
        fire[y][x] = Math.max(0,
            ((get(x - 1 + xsize, y - 1)
            + get(x,y - 1)
            + get(x + 1, y - 1)
            + get(x, y - 2))
            * 32) / 129 - 8);
      }
    }
  }

  private void randomInit() {
    //double multiplier = OSMBEAN.getSystemLoadAverage();
    double loadAverage = OSMBEAN.getSystemLoadAverage();
    int level = Math.max(0, Math.round((float)(256f * loadAverage)));
    Random rnd = new Random();
    for (int x = 0; x < xsize; x++) {
      fire[0][x] = rnd.nextInt(level);
    }
  }




  public void run() {

    try {
      while (true) {
        randomInit();

        convolveFlame();
        convolveFlame();

        wooting.setAutoUpdate(false);
        for (int ky = 0; ky < WootingRGB.WOOTING_RGB_ROWS; ky++) {
          for (int kx = 0; kx < WootingRGB.WOOTING_RGB_COLS; kx++) {

            int y = (ky * ysize + WootingRGB.WOOTING_RGB_ROWS - 1) / WootingRGB.WOOTING_RGB_ROWS;
            int x = (kx * xsize + WootingRGB.WOOTING_RGB_COLS / 2) / WootingRGB.WOOTING_RGB_COLS;

            wooting.setRGB(ky, kx, palette[Math.max(0, Math.min(palette.length - 1, get(x, ysize - y)))]);
          }
        }
        wooting.setAutoUpdate(true);

        Thread.sleep(100);
      }
    } catch (InterruptedException ex) {

    } finally {
      wooting.resetRGB();
    }


  }

  public static void main(String[] args) throws IOException {
    System.setProperty("java.awt.headless", "true");

    try (WootingRGB wooting = WootingRGB.open(WootingRGB.listAll()[0])) {
      wooting.resetRGB();
      new WootingWaterfall(wooting).run();
    }
  }
}
