// tools/gen_facade.mjs — generate Tools_All_in_One.java dari 12 pemilik.
// JANGAN EDIT FACADE MANUAL. Ubah pemilik, lalu:
//   node tools/gen_facade.mjs --regen   (tulis ulang facade)
//   node tools/gen_facade.mjs --check   (gate CI: exit 1 bila facade basi)
// Bisa juga dijalankan dari harness worker lewat globalThis.__FACADE_IO__
// ({read(path)->string|Promise, write(path,content), args:[...]}).

const OWNERS = ["Audio", "Base", "Compress", "Core", "Downloader", "File",
  "Image", "Layout", "Sheet", "Style", "Task", "Video"];
const FIELD = {};
for (const o of OWNERS) FIELD[o] = o.charAt(0).toLowerCase() + o.slice(1);
const OWNER_PATH = o => "src/com/fv7/tools/Tools_" + o + ".java";
const FACADE_PATH = "src/com/fv7/tools/Tools_All_in_One.java";

// Kolisi nama yang disengaja: hanyaSATU blok boleh hidup di facade.
// (TypeImage: Core="Image" vs File="image" — pertahankan File.)
const KEEP_OWNER = { TypeImage: "File" };
// LastError x12 identik -> delegasi langsung ke Shared (bukan owner).
// ErrorOccurred x12 identik -> satu cermin event.

const FACADE_DESC_LINES = [
  "12 Tools_* (Audio/Base/Compress/Core/Downloader/File/Image/Layout/Sheet/",
  "Style/Task/Video). Fasad: semua blok diteruskan ke mesin aslinya, semua ",
  "event dicerminkan ke sini. Cocok bila tidak mau banyak komponen di daftar ",
  "blok. JANGAN pasang bareng 12 Tools_* dalam satu aplikasi (event dobel + ",
  "boros memori). Konvensi awalan: Is = boolean (colok ke if), Call = async + ",
  "hasil lewat event, Set = tanpa feedback sukses (gagal via ErrorOccurred), ",
  "Get = nilai balik sinkron. Pilihan tetap (colok ke soket yang ",
  "cocok). FILE INI DIGENERATE OTOMATIS oleh tools/gen_facade.mjs — jangan ",
  "edit manual.",
];

function parseMembers(src) {
  const members = [];
  const re = /^  @(SimpleFunction|SimpleEvent|SimpleProperty|DesignerProperty)\(/gm;
  let m;
  while ((m = re.exec(src)) !== null) {
    const annotations = [];
    let pos = m.index + 2; // setelah 2 spasi
    for (;;) {
      const at = src.indexOf("@", pos);
      const open = src.indexOf("(", at);
      const name = src.slice(at + 1, open);
      let depth = 0, i = open, inStr = false;
      for (; i < src.length; i++) {
        const ch = src[i];
        if (inStr) {
          if (ch === "\\") { i++; continue; }
          if (ch === '"') inStr = false;
          continue;
        }
        if (ch === '"') { inStr = true; continue; }
        if (ch === "(") depth++;
        else if (ch === ")") {
          depth--;
          if (depth === 0) break;
        }
      }
      const text = src.slice(at, i + 1);
      annotations.push({ name, text });
      pos = i + 1;
      const rest = src.slice(pos);
      const nm = rest.match(/^\s*(@)\s*/);
      if (nm && rest[nm[0].length] !== undefined
          && /^@(Simple|Designer)/.test(rest.slice(nm[0].length - 1))) {
        pos = pos + nm[0].length - 1;
        continue;
      }
      break;
    }
    const sig = src.slice(pos).match(
      /^\s*public\s+([\w.<>\[\], ]+?)\s+(\w+)\s*\(/);
    if (!sig) throw new Error("gagal parse signature setelah: "
      + annotations.map(a => a.name).join("+"));
    const ret = sig[1].trim().replace(/\s+/g, " ");
    const mname = sig[2];
    let pstart = pos + sig[0].length - 1; // posisi '('
    let depth = 0, i = pstart, inStr = false;
    for (; i < src.length; i++) {
      const ch = src[i];
      if (inStr) {
        if (ch === "\\") { i++; continue; }
        if (ch === '"') inStr = false;
        continue;
      }
      if (ch === '"') { inStr = true; continue; }
      if (ch === "(") depth++;
      else if (ch === ")") {
        depth--;
        if (depth === 0) break;
      }
    }
    const ptext = src.slice(pstart + 1, i);
    const params = [];
    let cur = "", d2 = 0, s2 = false;
    const push = () => {
      const t = cur.trim();
      if (t.length > 0) {
        const parts = t.split(/\s+/);
        const pname = parts.pop();
        params.push({ type: parts.join(" ").replace(/\bfinal\b/g, "").trim(),
          name: pname, full: t });
      }
      cur = "";
    };
    for (let k = 0; k < ptext.length; k++) {
      const ch = ptext[k];
      if (s2) { cur += ch; if (ch === '"') s2 = false; continue; }
      if (ch === '"') { s2 = true; cur += ch; continue; }
      if (ch === "<") d2++;
      if (ch === ">") d2--;
      if (ch === "," && d2 === 0) { push(); continue; }
      cur += ch;
    }
    push();
    members.push({ annotations, ret, name: mname, params });
    re.lastIndex = i + 1;
  }
  return members;
}

function sigKey(mb) {
  return mb.name + "(" + mb.params.map(p => p.type).join(",") + ")";
}

function normAnn(text) {
  // rapikan indentasi lanjutan jadi 6 spasi (gaya facade lama)
  return text.split("\n").map((ln, ix) =>
    ix === 0 ? ln.trim() : "      " + ln.trim()).join("\n");
}

async function generate(IO) {
  const perOwner = {};
  const permOrder = [];
  const permSeen = {};
  let minSdk = 21;
  let hasOptions = false;
  const helperImports = [];
  for (const o of OWNERS) {
    const src = await IO.read(OWNER_PATH(o));
    if (src.indexOf("@Options(") >= 0) {
      hasOptions = true;
    }
    const hm = src.match(/^import (com\.fv7\.tools\.helpers\.\w+);/gm) || [];
    for (const h of hm) {
      const imp = h.slice(7, -1);
      if (helperImports.indexOf(imp) < 0) {
        helperImports.push(imp);
      }
    }
    perOwner[o] = parseMembers(src);
    const pm = src.match(/@UsesPermissions\(\{([^}]*)\}\)/);
    if (pm) {
      const parts = pm[1].match(/"[^"]+"/g) || [];
      for (const p of parts) {
        if (!permSeen[p]) { permSeen[p] = 1; permOrder.push(p); }
      }
    }
    const sdk = src.match(/androidMinSdk\s*=\s*(\d+)/);
    if (sdk && +sdk[1] > minSdk) minSdk = +sdk[1];
  }

  const seenSig = {};   // sigKey -> owner pertama
  const skipped = [];
  const out = [];
  out.push("// DIGENERATE OTOMATIS oleh tools/gen_facade.mjs — JANGAN EDIT MANUAL.");
  out.push("// Ubah 12 pemilik (Tools_Audio..Tools_Video), lalu jalan ulang --regen.");
  out.push("package com.fv7.tools;");
  out.push("");
  for (const hi of helperImports) {
    out.push("import " + hi + ";");
  }
  out.push("import com.google.appinventor.components.annotations.DesignerComponent;");
  out.push("import com.google.appinventor.components.annotations.DesignerProperty;");
  if (hasOptions) {
    out.push("import com.google.appinventor.components.annotations.Options;");
  }
  out.push("import com.google.appinventor.components.annotations.PropertyCategory;");
  out.push("import com.google.appinventor.components.annotations.SimpleEvent;");
  out.push("import com.google.appinventor.components.annotations.SimpleFunction;");
  out.push("import com.google.appinventor.components.annotations.SimpleObject;");
  out.push("import com.google.appinventor.components.annotations.SimpleProperty;");
  out.push("import com.google.appinventor.components.annotations.UsesPermissions;");
  out.push("import com.google.appinventor.components.common.ComponentCategory;");
  out.push("import com.google.appinventor.components.common.PropertyTypeConstants;");
  out.push("import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;");
  out.push("import com.google.appinventor.components.runtime.AndroidViewComponent;");
  out.push("import com.google.appinventor.components.runtime.Component;");
  out.push("import com.google.appinventor.components.runtime.ComponentContainer;");
  out.push("import com.google.appinventor.components.runtime.EventDispatcher;");
  out.push("import com.google.appinventor.components.runtime.util.YailList;");
  out.push("");
  out.push("import java.util.List;");
  out.push("");
  out.push("@DesignerComponent(");
  out.push("    version = 22,");
  out.push("    description = \"Tools_All_in_One: SATU komponen berisi SEMUA blok \"");
  for (let li = 0; li < FACADE_DESC_LINES.length; li++) {
    const end = li === FACADE_DESC_LINES.length - 1 ? '",' : '"';
    out.push("        + \"" + FACADE_DESC_LINES[li] + end);
  }
  out.push("    category = ComponentCategory.EXTENSION,");
  out.push("    nonVisible = true,");
  out.push("    androidMinSdk = " + minSdk + ",");
  out.push('    iconName = "aiwebres/tools_all_in_one.jpg",');
  out.push('    helpUrl = "https://github.com/VB-7/MiT-File_Manager")');
  out.push("@SimpleObject(external = true)");
  if (permOrder.length > 0) {
    out.push("@UsesPermissions({");
    out.push("    " + permOrder.join(",\n    "));
    out.push("})");
  }
  out.push("public class Tools_All_in_One extends AndroidNonvisibleComponent {");
  out.push("");
  for (const o of OWNERS) {
    out.push("  private final Tools_" + o + " " + FIELD[o] + ";");
  }
  out.push("");
  out.push("  public Tools_All_in_One(ComponentContainer container) {");
  out.push("    super(container.$form());");
  for (const o of OWNERS) {
    out.push("    " + FIELD[o] + " = new Tools_" + o + "(container);");
  }
  out.push("    Shared.registerAllInOne(this);");
  out.push("  }");
  out.push("");

  for (const o of OWNERS) {
    out.push("  // ================= " + o + " =================");
    out.push("");
    for (const mb of perOwner[o]) {
      const key = sigKey(mb);
      const ann0 = mb.annotations[0].name;
      if (mb.name === "LastError") {
        if (seenSig[key]) { skipped.push(o + "." + key + " (LastError)"); continue; }
        seenSig[key] = o;
        // 12 fungsi -> 1: deskripsi gabungan (daftar semua Op*), bukan anotasi pemilik pertama.
        const ops = [];
        for (const oo of OWNERS) {
          for (const x of perOwner[oo]) {
            if (x.name.indexOf("Op") === 0 && x.params.length === 0
                && x.annotations[0].name === "SimpleProperty"
                && ops.indexOf(x.name) < 0) ops.push(x.name);
          }
        }
        out.push('  @SimpleFunction(description = "Fallback polling untuk event ErrorOccurred "');
        const opList = ops.join("/");
        let rest = "gabungan 12 pemilik: pesan error terakhir operasi ini (teks kosong bila belum ada). Isi operation dengan " + opList
          + " untuk if. Contoh: simpan LastError(" + ops[0] + ") ke variabel bila ErrorOccurred menembak. ";
        while (rest.length > 0) {
          let cut = Math.min(90, rest.length);
          if (cut < rest.length) {
            const sp = rest.lastIndexOf(" ", cut);
            cut = sp > 40 ? sp : cut;
          }
          const piece = rest.slice(0, cut);
          rest = rest.slice(cut);
          const last = rest.length === 0;
          out.push("      + \"" + piece + "\"" + (last ? ")" : ""));
        }
        out.push("  public " + mb.ret + " LastError(String operation) {");
        out.push("    return Shared.lastError(operation);");
        out.push("  }");
        out.push("");
        continue;
      }
      if (mb.name === "ErrorOccurred") {
        if (seenSig[key]) { skipped.push(o + "." + key + " (ErrorOccurred)"); continue; }
        seenSig[key] = o;
        // 12 event -> 1: tulis deskripsi gabungan (daftar semua Op*), bukan anotasi pemilik.
        const ops = [];
        for (const oo of OWNERS) {
          for (const x of perOwner[oo]) {
            if (x.name.indexOf("Op") === 0 && x.params.length === 0
                && x.annotations[0].name === "SimpleProperty"
                && ops.indexOf(x.name) < 0) ops.push(x.name);
          }
        }
        out.push('  @SimpleEvent(description = "SATU-SATUNYA error gabungan 12 pemilik. "');
        const opList = ops.join("/");
        let rest = "component = nama operasi (isi dengan " + opList
          + " untuk if). Contoh: jika component = " + ops[0]
          + " maka tampilkan message. id = id terkait (0 bila statis). ";
        let first = true;
        while (rest.length > 0) {
          let cut = Math.min(90, rest.length);
          if (cut < rest.length) {
            const sp = rest.lastIndexOf(" ", cut);
            cut = sp > 40 ? sp : cut;
          }
          const piece = rest.slice(0, cut);
          rest = rest.slice(cut);
          const last = rest.length === 0;
          out.push("      + \"" + piece + "\"" + (last ? ")" : ""));
          first = false;
        }
        mb.__annDone = true;
      } else if (seenSig[key]) {
        const keep = KEEP_OWNER[mb.name];
        if (!(keep && o === keep)) {
          skipped.push(o + "." + key + " (kalah vs " + seenSig[key] + ")");
          continue;
        }
        // override: ganti pemilik sebelumnya — tandai, render yang ini
        skipped.push(seenSig[key] + "." + key + " (diganti " + o + ")");
        seenSig[key] = o;
      } else {
        seenSig[key] = o;
        if (KEEP_OWNER[mb.name] && KEEP_OWNER[mb.name] !== o) {
          // pemilik ini bukan pilihan — lewati, tunggu pemilik benar
          skipped.push(o + "." + key + " (tunggu " + KEEP_OWNER[mb.name] + ")");
          // jangan tandai seen agar pemilik benar bisa masuk — kembalikan
          delete seenSig[key];
          continue;
        }
      }
      const isEvent = ann0 === "SimpleEvent";
      const isSetter = !isEvent && mb.ret === "void" && mb.params.length === 1
        && mb.annotations.some(a => a.name === "SimpleProperty");
      // anotasi: untuk setter pakai DesignerProperty pemilik pertama + SimpleProperty
      if (!mb.__annDone) {
        for (const a of mb.annotations) out.push("  " + normAnn(a.text));
      }
      const pdecl = mb.params.map(p => p.full).join(", ");
      out.push("  public " + mb.ret + " " + mb.name + "(" + pdecl + ") {");
      const args = mb.params.map(p => p.name).join(", ");
      if (isEvent) {
        out.push('    EventDispatcher.dispatchEvent(this, "' + mb.name + '"'
          + (args.length > 0 ? ", " + args : "") + ");");
      } else if (isSetter) {
        // fan-out ke SEMUA pemilik yang punya setter ini
        const who = OWNERS.filter(
          oo => perOwner[oo].some(
            x => x.name === mb.name && x.ret === "void" && x.params.length === 1));
        for (const w of who) {
          out.push("    " + FIELD[w] + "." + mb.name + "(" + args + ");");
        }
      } else if (mb.ret === "void") {
        out.push("    " + FIELD[o] + "." + mb.name + "(" + args + ");");
      } else {
        out.push("    return " + FIELD[o] + "." + mb.name + "(" + args + ");");
      }
      out.push("  }");
      out.push("");
    }
  }
  out.push("}");
  out.push("");
  return { text: out.join("\n"), skipped };
}

let IO = null;
if (typeof process !== "undefined" && process.versions && process.versions.node) {
  const fs = await import("node:fs");
  IO = {
    read: p => fs.readFileSync(p, "utf8"),
    write: (p, c) => fs.writeFileSync(p, c),
    args: process.argv.slice(2),
  };
} else {
  IO = globalThis.__FACADE_IO__;
}

const args = IO.args || [];
const { text, skipped } = await generate(IO);
if (args.includes("--check")) {
  const cur = await IO.read(FACADE_PATH);
  if (cur === text) {
    if (typeof process !== "undefined" && process.versions
        && process.versions.node) process.exit(0);
  } else {
    const a = cur.split("\n"), b = text.split("\n");
    let i = 0;
    while (i < a.length && i < b.length && a[i] === b[i]) i++;
    const msg = "FACADE BASI: beda di baris " + (i + 1) + "\n- " + (a[i] || "")
      + "\n+ " + (b[i] || "") + "\nskipped: " + skipped.join("; ");
    if (typeof process !== "undefined" && process.versions
        && process.versions.node) {
      console.error(msg);
      process.exit(1);
    } else {
      throw new Error(msg);
    }
  }
} else {
  await IO.write(FACADE_PATH, text);
}
if (!(typeof process !== "undefined" && process.versions
    && process.versions.node)) {
  globalThis.__FACADE_RESULT__ = { chars: text.length, skipped };
}
