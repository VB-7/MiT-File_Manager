package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Filter tipe file untuk OpenFilePicker/OpenMultiFilePicker. Nilai =
 * pola mime yang dikirim ke picker (tampil pola, bukan nama).
 */
public enum PickMimeType implements OptionList<String> {
  All("*/*"),
  Audio("audio/*"),
  Video("video/*"),
  Image("image/*"),
  Text("text/*"),
  Pdf("application/pdf"),
  Zip("application/zip"),
  Apk("application/vnd.android.package-archive");

  private final String value;

  PickMimeType(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
