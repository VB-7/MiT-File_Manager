package com.fv7.tools.helpers;

import com.google.appinventor.components.common.OptionList;

/**
 * Kategori file untuk GetFilesByType/GetExtensionsForType. Nilai = nama
 * kategori kanonik (alias Indonesia/Inggris tetap bisa via blok teks).
 * pdf = sub-tipe dari document (filter khusus pdf saja; document tetap
 * mencakup pdf seperti dulu).
 */
public enum FileCategory implements OptionList<String> {
  Image("image"),
  Audio("audio"),
  Video("video"),
  Text("text"),
  Archive("archive"),
  Document("document"),
  Pdf("pdf"),
  Apk("apk"),
  Unknown("unknown");

  private final String value;

  FileCategory(String value) {
    this.value = value;
  }

  @Override
  public String toUnderlyingValue() {
    return value;
  }
}
