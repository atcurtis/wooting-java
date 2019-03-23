/*
 * Copyright 2019 Antony T Curtis <atcurtis@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.xiphis.wooting;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;


public class WootingRGB implements AutoCloseable {

  public static final int WOOTING_RGB_ROWS = 6;
  public static final int WOOTING_RGB_COLS = 21;

  private static final short WOOTING_VID = 0x03EB;
  private static final short WOOTING_ONE_PID = (short) 0xFF01;
  private static final short WOOTING_TWO_PID = (short) 0xFF02;

  private static final int WOOTING_COMMAND_SIZE = 8;
  private static final int WOOTING_REPORT_SIZE = 129;
  private static final byte RGB_RAW_BUFFER_SIZE = 96;

  private static final byte WOOTING_RAW_COLORS_REPORT = 11;
  private static final byte WOOTING_SINGLE_COLOR_COMMAND = 30;
  private static final byte WOOTING_SINGLE_RESET_COMMAND = 31;
  private static final byte WOOTING_RESET_ALL_COMMAND = 32;
  private static final byte WOOTING_COLOR_INIT_COMMAND = 33;

  private static final byte NIL = (byte) 0;
  private static final byte NOLED = (byte) 255;
  private static final byte LED_LEFT_SHIFT_ANSI = 9;
  private static final byte LED_LEFT_SHIFT_ISO = 7;
  private static final byte LED_ENTER_ANSI = 65;
  private static final byte LED_ENTER_ISO = 62;

  private static final byte[][] RGB_LED_INDEX = {
      { 0, NOLED, 11, 12, 23, 24, 36, 47, 85, 84, 49, 48, 59, 61, 73, 81, 80, 113, 114, 115, 116 },
      { 2, 1, 14, 13, 26, 25, 35, 38, 37, 87, 86, 95, 51, 63, 75, 72, 74, 96, 97, 98, 99 },
      { 3, 4, 15, 16, 27, 28, 39, 42, 40, 88, 89, 52, 53, 71, 76, 83, 77, 102, 103, 104, 100 },
      { 5, 6, 17, 18, 29, 30, 41, 46, 44, 90, 93, 54, 57, 65, NOLED, NOLED, NOLED, 105, 106, 107, NOLED },
      { 9, 8, 19, 20, 31, 34, 32, 45, 43, 91, 92, 55, NOLED, 66, NOLED, 78, NOLED, 108, 109, 110, 101 },
      { 10, 22, 21, NOLED, NOLED, NOLED, 33, NOLED, NOLED, NOLED, 94, 58, 67, 68, 70, 79, 82, NOLED, 111, 112, NOLED }
  };

  private static final int[] PWM_MEM_MAP = {
      0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd,
      0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d,
      0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d,
      0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x98, 0x99, 0x9a, 0x9b, 0x9c, 0x9d
  };

  private final HidDevice device;
  private final Bank[] banks;
  private boolean autoUpdate = true;

  public WootingRGB(HidDeviceInfo hidDeviceInfo) throws IOException {
    if (!vendorFilter(hidDeviceInfo)) {
      throw new IOException("Unsupported Vendor ID");
    }

    HidDevice device = PureJavaHidApi.openDevice(hidDeviceInfo);
    if (!sendFeature(device, WOOTING_COLOR_INIT_COMMAND, NIL, NIL, NIL, NIL)) {
      device.close();
      throw new IOException("Failed to send init command");
    }

    Bank[] banks = {
        new Bank(NIL, NIL),
        new Bank(NIL, RGB_RAW_BUFFER_SIZE),
        new Bank((byte) 1, NIL),
        new Bank((byte) 1, RGB_RAW_BUFFER_SIZE),
        new Bank((byte) 2, NIL),
    };


    device.setDeviceRemovalListener(this::deviceRemoved);
    device.setInputReportListener(this::inputReport);
    device.getFeatureReport(new byte[1], 0);

    this.device = device;
    this.banks = banks;
  }

  private void inputReport(HidDevice source, byte reportID, byte[] reportData, int reportLength) {
    System.out.printf("reportId=0x%02x length=%d\n", reportID, reportLength);

    for (int i = 0; i < reportLength; i++) {
      System.out.printf(" %02x", reportData[i]);
    }
  }

  private void deviceRemoved(HidDevice device) {

  }

  private static Map<String, List<HidDeviceInfo>> enumerateDevices() {
    return PureJavaHidApi.enumerateDevices().stream()
        .filter(WootingRGB::vendorFilter)
        .filter(WootingRGB::productFilter)
        .collect(Collectors.toMap(HidDeviceInfo::getDeviceId, Collections::singletonList,
            (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList())));
  }

  public static String[] listAll() {
    return enumerateDevices().keySet().toArray(new String[0]);
  }

  public static WootingRGB open(String deviceId) throws IOException {
    List<HidDeviceInfo> list = enumerateDevices().get(deviceId);
    if (list == null) {
      throw new NoSuchElementException();
    }
    return new WootingRGB(list.get(list.size() - 5));
  }

  public String getDeviceId() {
    return device.getHidDeviceInfo().getDeviceId();
  }

  public boolean isAutoUpdate() {
    return autoUpdate;
  }

  public void setAutoUpdate(boolean autoUpdate) {
    this.autoUpdate = autoUpdate;
    if (autoUpdate) {
      updateKeyboard();
    }
  }

  @Override
  public void close() {
    device.close();
  }

  public boolean resetRGB() {
    return sendFeature(device, WOOTING_RESET_ALL_COMMAND, NIL, NIL, NIL, NIL);
  }

  // Converts the array index to a memory location in the RGB buffers
  private static byte getSafeLEDIndex(int row, int column) {
    if (row >= 0 && column >= 0 && row < WOOTING_RGB_ROWS && column < WOOTING_RGB_COLS) {
      return RGB_LED_INDEX[row][column];
    } else {
      return NOLED;
    }
  }


  public boolean setDirectRGB(int row, int column, RGB rgb) {
    byte keyCode = getSafeLEDIndex(row, column);

    boolean update_ansi;
    boolean update_iso;

    if (keyCode == NOLED) {
      return false;
    }
    else if (keyCode == LED_LEFT_SHIFT_ANSI) {
      update_ansi = sendFeature(device, WOOTING_SINGLE_COLOR_COMMAND, LED_LEFT_SHIFT_ANSI, rgb.red, rgb.green, rgb.blue);
      update_iso = sendFeature(device, WOOTING_SINGLE_COLOR_COMMAND, LED_LEFT_SHIFT_ISO, rgb.red, rgb.green, rgb.blue);
    }
    else if (keyCode == LED_ENTER_ANSI) {
      update_ansi = sendFeature(device, WOOTING_SINGLE_COLOR_COMMAND, LED_ENTER_ANSI, rgb.red, rgb.green, rgb.blue);
      update_iso = sendFeature(device, WOOTING_SINGLE_COLOR_COMMAND, LED_ENTER_ISO, rgb.red, rgb.green, rgb.blue);
    }
    else {
      return sendFeature(device, WOOTING_SINGLE_COLOR_COMMAND, keyCode, rgb.red, rgb.green, rgb.blue);
    }
    return update_ansi && update_iso;
  }

  public boolean resetDirectRGB(int row, int column) {
    byte keyCode = getSafeLEDIndex(row, column);

    boolean update_ansi;
    boolean update_iso;

    if (keyCode == NOLED) {
      return false;
    }
    else if (keyCode == LED_LEFT_SHIFT_ANSI) {
      update_ansi = sendFeature(device, WOOTING_SINGLE_RESET_COMMAND, (byte) 0, (byte) 0, (byte) 0, LED_LEFT_SHIFT_ANSI);
      update_iso = sendFeature(device, WOOTING_SINGLE_RESET_COMMAND, (byte) 0, (byte) 0, (byte) 0, LED_LEFT_SHIFT_ISO);
    }
    else if (keyCode == LED_ENTER_ANSI) {
      update_ansi = sendFeature(device, WOOTING_SINGLE_RESET_COMMAND, (byte) 0, (byte) 0, (byte) 0, LED_ENTER_ANSI);
      update_iso = sendFeature(device, WOOTING_SINGLE_RESET_COMMAND, (byte) 0, (byte) 0, (byte) 0, LED_ENTER_ISO);
    }
    else {
      return sendFeature(device, WOOTING_SINGLE_RESET_COMMAND, (byte) 0, (byte) 0, (byte) 0, keyCode);
    }
    return update_ansi && update_iso;
  }

  public boolean setRGB(int row, int col, RGB rgb) {
    if (!setRGB0(row, col, rgb)) {
      return false;
    }

    if (autoUpdate) {
      return updateKeyboard();
    } else {
      return true;
    }
  }

  public boolean forceUpdate() {
    for (Bank bank : banks) {
      bank.changed = true;
    }
    return updateKeyboard();
  }

  public boolean updateKeyboard() {
    boolean success = true;
    for (Bank bank : banks) {
      success = success && bank.update();
    }
    return success;
  }

  private boolean setRGB0(int row, int col, RGB rgb) {
    byte led_index = getSafeLEDIndex(row, col);

    if (led_index == NOLED) {
      return false;
    }
    Bank bank = banks[led_index / 24];
    bank.setRGB(led_index % 24, rgb);

    if (led_index == LED_ENTER_ANSI) {
      bank.setRGB(LED_ENTER_ISO - 48, rgb);
    }

    if (led_index == LED_LEFT_SHIFT_ANSI) {
      bank.setRGB(LED_LEFT_SHIFT_ISO, rgb);
    }

    return true;
  }


  public static final class RGB {
    public byte red;
    public byte green;
    public byte blue;

    public int getRed() {
      return 0xff & red;
    }

    public int getGreen() {
      return 0xff & green;
    }

    public int getBlue() {
      return 0xff & blue;
    }

    public int toInteger() {
      int color = getRed();
      color <<= 8;
      color |= getGreen();
      color <<= 8;
      color |= getBlue();
      return color;
    }

    public static RGB of(int red, int green, int blue) {
      RGB color = new RGB();
      color.red = (byte) red;
      color.green = (byte) green;
      color.blue = (byte) blue;
      return color;
    }

    public static RGB of(int rgb) {
      return of(0xff & (rgb >>> 16), 0xff & (rgb >>> 8), 0xff & rgb);
    }

    @Override
    public String toString() {
      return String.format("%06x", toInteger());
    }
  }

  private class Bank {

    final byte[] buffer = new byte[WOOTING_REPORT_SIZE - 1];
    boolean changed;

    Bank(byte slave, byte start) {
      buffer[0] = (byte) 0xD0; // Magic word
      buffer[1] = (byte) 0xDA; // Magic word
      buffer[2] = WOOTING_RAW_COLORS_REPORT;
      buffer[3] = slave; // Slave nr
      buffer[4] = start; // Reg start address
    }

    synchronized boolean update() {
      if (changed) {
        short crc = getCrc16ccitt(CRC_INIT, buffer, WOOTING_REPORT_SIZE - 3);
        buffer[126] = (byte) crc;
        buffer[127] = (byte) (crc >>> 8);

        if (device.setOutputReport((byte) 0, buffer, WOOTING_REPORT_SIZE - 1) != WOOTING_REPORT_SIZE - 1) {
          return false;
        }
        changed = false;
      }
      return true;
    }

    int getRGBBuffer(int buffer_index) {
      int rgb = 0xff & buffer[buffer_index];
      rgb <<= 8;
      rgb |= 0xff & buffer[buffer_index + 0x10];
      rgb <<= 8;
      rgb |= 0xff & buffer[buffer_index + 0x20];
      return rgb;
    }

    synchronized void setRGB(int led_index, RGB rgb) {
      int buffer_index = 5 + PWM_MEM_MAP[led_index];

      if (getRGBBuffer(buffer_index) == rgb.toInteger()) {
        return;
      }

      buffer[buffer_index] = rgb.red;
      buffer[buffer_index + 0x10] = rgb.green;
      buffer[buffer_index + 0x20] = rgb.blue;
      changed = true;
    }
  }

  private static final short CRC_INIT = getCrc16ccitt((short) 0, new byte[1], 1);


  private static short getCrc16ccitt(short crc, byte[] buffer, int size) {
    int offset = 0;
    while (size-- > 0) {
      short value = buffer[offset++];
      value <<= 8;
      crc ^= value;

      for (int i = 0; i < 8; i++) {
        if ((crc & 0x8000) != 0) {
          crc <<= 1;
          crc ^= 0x1021;
        } else {
          crc <<= 1;
        }
      }
    }

    return crc;
  }

  private static boolean productFilter(HidDeviceInfo hidDeviceInfo) {
    switch (hidDeviceInfo.getProductId()) {
      case WOOTING_ONE_PID:
      case WOOTING_TWO_PID:
        return true;
      default:
        return false;
    }
  }

  private static boolean vendorFilter(HidDeviceInfo hidDeviceInfo) {
    return hidDeviceInfo.getVendorId() == WOOTING_VID;
  }

  private static final byte[] COMMAND_BUFFER = new byte[WOOTING_COMMAND_SIZE];

  private static boolean sendFeature(HidDevice device, byte commandId, byte parameter0, byte parameter1, byte parameter2, byte parameter3) {
    synchronized (COMMAND_BUFFER) {
      byte[] reportBuffer = COMMAND_BUFFER;
      reportBuffer[1] = (byte) 0xD0; // Magic word
      reportBuffer[2] = (byte) 0xDA; // Magic word
      reportBuffer[3] = commandId;
      reportBuffer[4] = parameter3;
      reportBuffer[5] = parameter2;
      reportBuffer[6] = parameter1;
      reportBuffer[7] = parameter0;
      return device.setFeatureReport(reportBuffer, WOOTING_COMMAND_SIZE) == WOOTING_COMMAND_SIZE;
    }
  }
}
