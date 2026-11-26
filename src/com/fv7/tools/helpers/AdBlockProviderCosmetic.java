package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Pilihan penyedia AdBlock (Cosmetic). Urutan = relevansi.
 * Nilai = kunci nama (URL dipetakan di Tools_AdBlock; tampil nama, bukan link).
 */
public enum AdBlockProviderCosmetic implements OptionList<String> {
  AdGuardAnnoyances("AdGuardAnnoyances"),
  FanboyAnnoyance("FanboyAnnoyance"),
  UBOAnnoyances("UBOAnnoyances"),
  FanboyCookie("FanboyCookie"),
  ABPWarningRemoval("ABPWarningRemoval"),
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
  UBOUnbreak("UBOUnbreak"),
  PeterLowe("PeterLowe"),
  PeterLoweNoHtml("PeterLoweNoHtml"),
  OneHostsLite("OneHostsLite");

  private final String value;

  AdBlockProviderCosmetic(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
