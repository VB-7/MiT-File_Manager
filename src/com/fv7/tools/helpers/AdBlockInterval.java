package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Rentang update AdBlock.
 * Nilai = kunci nama (URL dipetakan di Tools_AdBlock; tampil nama, bukan link).
 */
public enum AdBlockInterval implements OptionList<String> {
  Auto("Auto"),
  Daily("Daily"),
  Weekly("Weekly");

  private final String value;

  AdBlockInterval(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
