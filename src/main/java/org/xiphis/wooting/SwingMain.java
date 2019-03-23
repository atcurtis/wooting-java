/*
 * Copyright 2019 Antony T Curtis <atcurtis@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.xiphis.wooting;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.*;


public class SwingMain extends JPanel implements Runnable {

  private final WootingRGB _wootingRGB;
  private boolean resetOnClose = true;

  private static final String[][] KEYBOARD = {
      { "Esc", null, "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "Prt", "Pau", "SLk", "\u278a", "\u278b", "\u278c", "\u2709"},
      { "`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "\u232b", "Ins", "Hom", "PUp", "\u12ed", "/", "*", "-" },
      { "\u21b9", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\", "Del", "End", "PDn", "7", "8", "9", "+" },
      { "\u21ea", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", null, "\u23ce", null, null, null, "4", "5", "6", null },
      { "\u21e7", null, "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", null, "\u21e7 ", null, " \u2191 ", null, "1", "2", "3", "\u2b76"},
      { "Ctrl", "\u2756", "Alt", null, null, null, " ", null, null, null, "Alt", "\u2756", "Fn", "Ctrl", " \u2190 ", " \u2193 ", " \u2192 ", null, "0 ", "\u2326", null}
  };


  public static void main(String[] args) throws IOException {
    for (String deviceName : WootingRGB.listAll()) {
      new SwingMain(WootingRGB.open(deviceName)).run();
    }
  }

  public SwingMain(WootingRGB wootingRGB) {
    this._wootingRGB = wootingRGB;
    setLayout(new GridBagLayout());

    Font monospace = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    Insets zero = new Insets(0, 0, 0, 0);
    Insets rowInsets = new Insets(0, 0, 7, 0);

    JColorChooser chooser = new JColorChooser();

    GridBagConstraints cRow = new GridBagConstraints();
    GridBagConstraints cButton = new GridBagConstraints();
    cRow.anchor = GridBagConstraints.WEST;
    cButton.ipady = 21;

    for (int row = 0; row < WootingRGB.WOOTING_RGB_ROWS; row++) {

      JPanel pRow = new JPanel(new GridBagLayout());
      cRow.gridy = row;

      for (int col = 0; col < WootingRGB.WOOTING_RGB_COLS; col++) {
        if (col >= KEYBOARD[row].length) {
          continue;
        }
        String legend = KEYBOARD[row][col];
        if (legend == null) {
          continue;
        }

        switch (legend) {
          case "\u21b9":
            cButton.ipadx = 28;
            break;
          case "\\":
            cButton.ipadx = 28;
            cButton.insets = new Insets(0, 0, 0, 7);
            break;
          case "\u21ea" :
            cButton.ipadx = 43;
            break;
          case "\u21e7":
            cButton.ipadx = 70;
            break;
          case "\u21e7 ":
            cButton.ipadx = 75;
            break;
          case " ":
            cButton.ipadx = 280;
            //cButton.insets = new Insets(0, 192, 0, 72);
            break;
          case " \u2190 ":
            cButton.ipadx = 7;
            cButton.insets = new Insets(0, 7, 0, 0);
            break;
          case " \u2192 ":
            cButton.ipadx = 7;
            cButton.insets = new Insets(0, 0, 0, 7);
            break;
          case " \u2191 ":
            cButton.ipadx = 7;
            cButton.insets = new Insets(0, 69, 0, 69);
            break;
          case "0 ": // 2spc
            cButton.ipadx = 49;
            cButton.insets = zero;
            break;
          case "\u232b":
            cButton.ipadx = 49;
            cButton.insets = new Insets(0, 0, 0, 7);
            break;
          case "\u23ce":
            cButton.ipadx = 61;
            cButton.insets = new Insets(0, 0, 0, 200);
            break;
          case "PUp":
          case "PDn":
            cButton.ipadx = 7;
            cButton.insets = new Insets(0, 0 , 0, 7);
            break;
          case "Ctrl":
          case "Alt":
            cButton.ipadx = 2;
            break;
          case "F1":
          case "F2":
          case "F3":
          case "F4":
          case "F5":
          case "F6":
          case "F7":
          case "F8":
          case "F9":
            cButton.ipadx = 6;
            cButton.insets = rowInsets;
            break;
          case "F10":
          case "F11":
            cButton.ipadx = 0;
            break;
          case "Esc":
          case "F12":
            cButton.ipadx = 0;
            cButton.insets = new Insets(0, 0 , 7, 7);
            break;
          case "SLk":
            cButton.ipadx = 7;
            cButton.insets = new Insets(0, 0 , 7, 7);
            break;
          case "\u12ed":
            cButton.ipadx = 6;
            cButton.insets = rowInsets;
            break;
          case "\u278a":
          case "\u278b":
          case "\u278c":
            cButton.ipadx = 2;
            cButton.insets = rowInsets;
            break;
          default:
            cButton.ipadx = 7;
            cButton.insets = rowInsets;
        }

        JButton button = new JButton(legend);
        button.setFont(monospace);
        button.setFocusable(false);
        pRow.add(button, cButton);

        int r = row;
        int c = col;

        button.addActionListener(event -> {
          WootingRGB.RGB rgb = WootingRGB.RGB.of(chooser.getColor().getRGB());
          //System.out.printf("row=%d col=%d color=%s success=%s\n", r, c, rgb, wootingRGB.setDirectRGB(r, c, rgb));
          System.out.printf("row=%d col=%d color=%s success=%s\n", r, c, rgb, wootingRGB.setRGB(r, c, rgb));
        });
      }
      add(pRow, cRow);
      rowInsets = zero;
    }

    GridBagConstraints cChooser = new GridBagConstraints();
    cChooser.fill = GridBagConstraints.HORIZONTAL;
    cChooser.insets = new Insets(0, 100, 0, 100);
    JPanel pRow = new JPanel(new GridBagLayout());
    pRow.add(chooser, cChooser);
    cRow.gridy = WootingRGB.WOOTING_RGB_ROWS;
    add(pRow, cRow);
  }

  @Override
  public void run() {
    JFrame frame = new JFrame("WootingRGB Keyboard " + _wootingRGB.getDeviceId());
    frame.add(this);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setResizable(false);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (resetOnClose) {
          _wootingRGB.resetRGB();
        }
        try {
          _wootingRGB.close();
        } finally {
          System.out.println("closed");
        }
      }
    });
  }
}
