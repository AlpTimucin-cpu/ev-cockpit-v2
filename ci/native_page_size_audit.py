#!/usr/bin/env python3
"""Verify APK native ELF PT_LOAD alignment and ZIP alignment. No private keys."""
import struct
import sys
import zipfile

PAGE_SIZE = 16 * 1024
TARGET_ABIS = {"arm64-v8a", "x86_64"}


def load_segment_alignments(data):
    if data[:4] != b"\x7fELF" or len(data) < 64:
        raise ValueError("Geçersiz ELF dosyası")
    endian = "<" if data[5] == 1 else ">" if data[5] == 2 else None
    if endian is None:
        raise ValueError("ELF endianness bilinmiyor")
    if data[4] == 2:
        phoff = struct.unpack_from(endian + "Q", data, 32)[0]
        entsize, count = struct.unpack_from(endian + "HH", data, 54)
        align_offset, align_fmt = 48, "Q"
    elif data[4] == 1:
        phoff = struct.unpack_from(endian + "I", data, 28)[0]
        entsize, count = struct.unpack_from(endian + "HH", data, 42)
        align_offset, align_fmt = 28, "I"
    else:
        raise ValueError("ELF class bilinmiyor")
    if not (0 < entsize <= 256 and 0 < count <= 1000) or phoff + entsize * count > len(data):
        raise ValueError("ELF program header sınır dışı")
    alignments = []
    for i in range(count):
        offset = phoff + i * entsize
        if struct.unpack_from(endian + "I", data, offset)[0] == 1:  # PT_LOAD
            alignments.append(struct.unpack_from(endian + align_fmt, data,
                                                 offset + align_offset)[0])
    if not alignments:
        raise ValueError("PT_LOAD bulunamadı")
    return alignments


def zip_data_offset(file_handle, entry):
    file_handle.seek(entry.header_offset)
    header = file_handle.read(30)
    if len(header) != 30 or header[:4] != b"PK\x03\x04":
        raise ValueError("ZIP local header geçersiz")
    file_name_length, extra_length = struct.unpack_from("<HH", header, 26)
    return entry.header_offset + 30 + file_name_length + extra_length


def audit(path):
    errors, warnings, lines = [], [], []
    target_count = 0
    with open(path, "rb") as handle, zipfile.ZipFile(path) as apk:
        entries = [e for e in apk.infolist() if e.filename.startswith("lib/")
                   and e.filename.endswith(".so")]
        if not entries:
            errors.append("APK native .so içermiyor; denetim geçerli değil")
        for entry in entries:
            abi = entry.filename.split("/")[1]
            alignments = load_segment_alignments(apk.read(entry.filename))
            zip_offset = zip_data_offset(handle, entry)
            zip_ok = (entry.compress_type != zipfile.ZIP_STORED or
                      zip_offset % PAGE_SIZE == 0)
            elf_ok = all(v >= PAGE_SIZE for v in alignments)
            if abi in TARGET_ABIS:
                target_count += 1
                if not elf_ok: errors.append("TARGET ELF 16K FAIL: " + entry.filename)
            elif not elf_ok:
                warnings.append("NON_TARGET ELF 16K FAIL (destek riski): " + entry.filename)
            if not zip_ok:
                errors.append("ZIP ALIGNMENT FAIL: " + entry.filename)
            lines.append(f"{entry.filename} ELF_PT_LOAD={alignments} "
                         f"ZIP_OFFSET_MOD_16K={zip_offset % PAGE_SIZE} "
                         f"STATUS={'PASS' if elf_ok and zip_ok else 'RISK'}")
    if target_count == 0:
        errors.append("arm64-v8a / x86_64 hedef native kütüphanesi bulunamadı")
    lines.extend("UYARI: " + s for s in warnings)
    lines.extend("HATA: " + s for s in errors)
    lines.append(f"NATIVE_16K_ZIP={'PASS' if not any('ZIP ALIGNMENT' in e for e in errors) else 'FAIL'}")
    lines.append(f"NATIVE_16K_TARGET={'PASS' if not errors else 'FAIL'}")
    lines.append("NON_TARGET_ABI_RISK_COUNT=" + str(len(warnings)))
    return "\n".join(lines), not errors


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Kullanım: native_page_size_audit.py <app-debug.apk>")
        sys.exit(2)
    report, ok = audit(sys.argv[1])
    print(report)
    sys.exit(0 if ok else 1)
