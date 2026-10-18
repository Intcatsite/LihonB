#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Builds md3_clock.lbp plugin.
"""

import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SAMPLE_DIR = ROOT / "sample_plugin" / "md3_clock"
BUILD_DIR = SAMPLE_DIR / "build"
CLASSES_DIR = BUILD_DIR / "classes"
DEX_DIR = BUILD_DIR / "dex"

ANDROID_JAR = Path(r"C:\Users\bobik\AppData\Local\Android\Sdk\platforms\android-34\android.jar")
LIHONB_CLASSES = ROOT / "LihonB" / "build" / "classes"
R8_JAR = ROOT / "tools" / "r8.jar"

OUT_LBP = ROOT / "md3_clock.lbp"
DESKTOP_LBP = Path(r"C:\Users\bobik\Desktop\md3_clock.lbp")
DOWNLOADS_DIR = Path(r"C:\Users\bobik\Downloads")
DOWNLOADS_FILES_DIR = DOWNLOADS_DIR / "files"
WEB_DIR = ROOT / "src" / "web"
WEB_LIHONB_DIR = WEB_DIR / "lihonb"


def main():
    print("=" * 60)
    print("Building MD3 Clock Plugin (.lbp)")
    print("=" * 60)

    if BUILD_DIR.exists():
        shutil.rmtree(BUILD_DIR)
    CLASSES_DIR.mkdir(parents=True, exist_ok=True)
    DEX_DIR.mkdir(parents=True, exist_ok=True)

    # 1. Compile Java
    java_files = list((SAMPLE_DIR / "src").rglob("*.java"))
    cp = f"{ANDROID_JAR};{LIHONB_CLASSES}"

    javac_cmd = [
        "javac",
        "--release", "8",
        "-cp", cp,
        "-d", str(CLASSES_DIR),
        *[str(f) for f in java_files]
    ]
    print(f"-> Compiling {len(java_files)} Java source files...")
    res = subprocess.run(javac_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if res.returncode != 0:
        print("javac ERROR:\n", res.stdout, res.stderr)
        sys.exit(1)

    # 2. Convert to classes.dex with modern D8
    class_files = list(CLASSES_DIR.rglob("*.class"))
    print(f"-> Generating classes.dex from {len(class_files)} class files...")
    d8_cmd = [
        "java", "-cp", str(R8_JAR),
        "com.android.tools.r8.D8",
        "--release",
        "--min-api", "26",
        "--output", str(DEX_DIR),
        "--lib", str(ANDROID_JAR),
        "--lib", str(LIHONB_CLASSES),
        *[str(f) for f in class_files]
    ]
    res = subprocess.run(d8_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if res.returncode != 0:
        print("d8 ERROR:\n", res.stdout, res.stderr)
        sys.exit(1)

    # 3. Create .lbp ZIP archive containing manifest.json and classes.dex
    manifest_file = SAMPLE_DIR / "manifest.json"
    dex_file = DEX_DIR / "classes.dex"

    print("-> Packaging .lbp archive...")
    with zipfile.ZipFile(OUT_LBP, "w", zipfile.ZIP_DEFLATED) as z:
        z.write(manifest_file, "manifest.json")
        z.write(dex_file, "classes.dex")

    # Copy to Desktop
    shutil.copyfile(OUT_LBP, DESKTOP_LBP)

    # Copy to Downloads and Downloads/files
    if DOWNLOADS_DIR.exists():
        shutil.copyfile(OUT_LBP, DOWNLOADS_DIR / "md3_clock.lbp")
    if DOWNLOADS_FILES_DIR.exists():
        shutil.copyfile(OUT_LBP, DOWNLOADS_FILES_DIR / "md3_clock.lbp")

    # Copy to web directories
    if WEB_DIR.exists():
        shutil.copyfile(OUT_LBP, WEB_DIR / "md3_clock.lbp")
    if WEB_LIHONB_DIR.exists():
        shutil.copyfile(OUT_LBP, WEB_LIHONB_DIR / "md3_clock.lbp")

    size_kb = OUT_LBP.stat().st_size / 1024
    print("=" * 60)
    print(f"SUCCESS! Built {OUT_LBP.name} ({size_kb:.2f} KB)")
    print(f"Copied to Desktop: {DESKTOP_LBP}")
    print(f"Copied to Downloads: {DOWNLOADS_DIR / 'md3_clock.lbp'}")
    if DOWNLOADS_FILES_DIR.exists():
        print(f"Copied to Downloads/files: {DOWNLOADS_FILES_DIR / 'md3_clock.lbp'}")
    print("=" * 60)


if __name__ == "__main__":
    main()
