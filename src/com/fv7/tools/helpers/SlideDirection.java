package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Arah animasi CallAnimateSlideIn. Nilai = arah kecil (tampil nama).
 */
public enum SlideDirection implements OptionList<String> {
  Left("left"),
  Right("right"),
  Top("top"),
  Bottom("bottom");

  private final String value;

  SlideDirection(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
