package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Tipe layout dinamis (cocok persis dengan nama komponen MIT).
 * Nilai = nama tipe (tampil nama, bukan kode).
 */
public enum LayoutType implements OptionList<String> {
  Vertical("VerticalArrangement"),
  Horizontal("HorizontalArrangement"),
  VerticalScroll("VerticalScrollArrangement"),
  HorizontalScroll("HorizontalScrollArrangement");

  private final String value;

  LayoutType(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
