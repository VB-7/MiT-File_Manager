package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Nama scriptlet global AdBlock. Nilai = nama kecil (tampil nama).
 */
public enum ScriptletName implements OptionList<String> {
  Popup("popup"),
  AntiAdblock("antiadblock"),
  Overlay("overlay"),
  Redirect("redirect"),
  Privacy("privacy");

  private final String value;

  ScriptletName(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
