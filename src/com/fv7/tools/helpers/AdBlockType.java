package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Jenis data AdBlock.
 * Nilai = kunci nama (URL dipetakan di Tools_AdBlock; tampil nama, bukan link).
 */
public enum AdBlockType implements OptionList<String> {
  All("All"),
  Host("Host"),
  Rule("Rule"),
  White("White"),
  Cosmetic("Cosmetic");

  private final String value;

  AdBlockType(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
