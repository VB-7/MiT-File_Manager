package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Satuan ukuran file untuk GetFileSize/RequestFileSize/ConvertFileSize.
 * Nilai = label kanonik (basis 1024). Soket tetap terima teks bebas
 * ("Auto" untuk RequestFileSize; alias tak dikenal = -1).
 */
public enum SizeUnit implements OptionList<String> {
  B("B"),
  KB("KB"),
  MB("MB"),
  GB("GB"),
  TB("TB"),
  PB("PB"),
  EB("EB"),
  ZB("ZB"),
  YB("YB");

  private final String value;

  SizeUnit(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
