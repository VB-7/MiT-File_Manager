package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Pilihan penyedia AdBlock (White). Urutan = relevansi.
 * Nilai = kunci nama (URL dipetakan di Tools_AdBlock; tampil nama, bukan link).
 */
public enum AdBlockProviderWhite implements OptionList<String> {
  Semua("Semua"),
  UBOUnbreak("UBOUnbreak"),
  EasyList("EasyList"),
  UBOFilters("UBOFilters"),
  AdGuardBase("AdGuardBase"),
  EasyPrivacy("EasyPrivacy"),
  IDMFilters("IDMFilters"),
  ABPindo("ABPindo"),
  AntiAdblockKiller("AntiAdblockKiller"),
  ABPWarningRemoval("ABPWarningRemoval"),
  FuckFuckAdblock("FuckFuckAdblock"),
  AdGuardMobile("AdGuardMobile"),
  UBOBadware("UBOBadware"),
  UBOPrivacy("UBOPrivacy"),
  UBOResourceAbuse("UBOResourceAbuse"),
  URLhausMalware("URLhausMalware"),
  AdGuardTurkish("AdGuardTurkish"),
  AdGuardRussian("AdGuardRussian"),
  FanboyIndian("FanboyIndian"),
  EasyGermany("EasyGermany"),
  OISDBig("OISDBig"),
  AdGuardAnnoyances("AdGuardAnnoyances"),
  FanboyAnnoyance("FanboyAnnoyance"),
  UBOAnnoyances("UBOAnnoyances"),
  FanboyCookie("FanboyCookie"),
  PeterLowe("PeterLowe"),
  PeterLoweNoHtml("PeterLoweNoHtml"),
  OneHostsLite("OneHostsLite");

  private final String value;

  AdBlockProviderWhite(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
