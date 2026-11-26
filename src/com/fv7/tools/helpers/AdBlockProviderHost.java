package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Pilihan penyedia AdBlock (Host). Urutan = relevansi.
 * Nilai = kunci nama (URL dipetakan di Tools_AdBlock; tampil nama, bukan link).
 */
public enum AdBlockProviderHost implements OptionList<String> {
  PeterLowe("PeterLowe"),
  PeterLoweNoHtml("PeterLoweNoHtml"),
  OneHostsLite("OneHostsLite"),
  OISDBig("OISDBig"),
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
  ABPWarningRemoval("ABPWarningRemoval"),
  UBOUnbreak("UBOUnbreak"),
  AdGuardAnnoyances("AdGuardAnnoyances"),
  FanboyAnnoyance("FanboyAnnoyance"),
  UBOAnnoyances("UBOAnnoyances"),
  FanboyCookie("FanboyCookie");

  private final String value;

  AdBlockProviderHost(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
