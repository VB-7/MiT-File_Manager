package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Mode update mesin AdBlock global. Nilai = nama mode kanonik.
 */
public enum AdBlockUpdateMode implements OptionList<String> {
  Manual("Manual"),
  TiapBuka("TiapBuka"),
  Harian("Harian"),
  Mingguan("Mingguan"),
  Auto("Auto");

  private final String value;

  AdBlockUpdateMode(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
