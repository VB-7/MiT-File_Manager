package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Pilihan penyedia AdBlock (Rule). Urutan = relevansi.
 * Nilai = kunci nama (URL dipetakan di Tools_AdBlock; tampil nama, bukan link).
 */
public enum AdBlockProviderRule implements OptionList<String> {
  Semua("Semua"),
  EasyList("EasyList"),
  UBOFilters("UBOFilters"),
  AdGuardBase("AdGuardBase"),
  IDMFilters("IDMFilters"),
  ABPindo("ABPindo"),
  EasyPrivacy("EasyPrivacy"),
  AntiAdblockKiller("AntiAdblockKiller"),
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
  ABPWarningRemoval("ABPWarningRemoval"),
  UBOUnbreak("UBOUnbreak"),
  AdGuardAnnoyances("AdGuardAnnoyances"),
  FanboyAnnoyance("FanboyAnnoyance"),
  UBOAnnoyances("UBOAnnoyances"),
  FanboyCookie("FanboyCookie"),
  PeterLowe("PeterLowe"),
  PeterLoweNoHtml("PeterLoweNoHtml"),
  OneHostsLite("OneHostsLite");

  private final String value;

  AdBlockProviderRule(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
