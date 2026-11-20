package com.fv7.tools;

import com.google.appinventor.components.common.OptionList;

/**
 * Daftar algoritma hash untuk GetFileHash. Muncul sebagai Helper merah
 * "Type" + dropdown di editor blok (mekanisme OptionList MIT).
 */
public enum Type implements OptionList<String> {
  MD5("MD5"),
  SHA1("SHA-1"),
  SHA224("SHA-224"),
  SHA256("SHA-256"),
  SHA384("SHA-384"),
  SHA512("SHA-512");

  private final String value;

  Type(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
