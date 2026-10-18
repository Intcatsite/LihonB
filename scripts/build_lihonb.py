#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LihonB Beta Compiler Pipeline
Builds a native, lightweight Material Design 3 Google WebView browser APK from scratch.
"""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LIHONB_DIR = ROOT / "LihonB"
BUILD_DIR = LIHONB_DIR / "build"
GEN_DIR = LIHONB_DIR / "gen"
OUT_DIR = ROOT / "ComplectAPK"
OUT_DIR.mkdir(parents=True, exist_ok=True)

FINAL_APK = OUT_DIR / "lihonb-1.0-beta.apk"
DESKTOP_APK = Path(r"C:\Users\bobik\Desktop\lihonb-1.0-beta.apk")

# SDK paths
SDK_BUILD_TOOLS = Path(r"C:\Users\bobik\AppData\Local\Android\Sdk\build-tools\34.0.0")
AAPT2 = SDK_BUILD_TOOLS / "aapt2.exe"
D8 = SDK_BUILD_TOOLS / "d8.bat"
ANDROID_JAR = Path(r"C:\Users\bobik\AppData\Local\Android\Sdk\platforms\android-34\android.jar")
UBER_SIGNER = ROOT / "tools" / "uber-apk-signer.jar"


def run_cmd(cmd: list[str | Path], desc: str) -> None:
    print(f"-> {desc}...")
    res = subprocess.run([str(c) for c in cmd], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if res.returncode != 0:
        print(f"ERROR in {desc}:")
        print(res.stdout)
        print(res.stderr)
        sys.exit(res.returncode)


def main() -> None:
    print("=" * 60)
    print("Building LihonB 1.0.0-beta (Material Design 3 WebView Browser)")
    print("=" * 60)

    # Clean previous build artifacts
    if BUILD_DIR.exists():
        shutil.rmtree(BUILD_DIR)
    if GEN_DIR.exists():
        shutil.rmtree(GEN_DIR)

    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    GEN_DIR.mkdir(parents=True, exist_ok=True)
    classes_dir = BUILD_DIR / "classes"
    classes_dir.mkdir(parents=True, exist_ok=True)
    dex_dir = BUILD_DIR / "dex"
    dex_dir.mkdir(parents=True, exist_ok=True)

    # 1. Compile resources with aapt2
    compiled_res = BUILD_DIR / "resources.zip"
    run_cmd([
        AAPT2, "compile",
        "--dir", LIHONB_DIR / "res",
        "-o", compiled_res
    ], "Compiling resources with aapt2")

    # 2. Link resources and generate R.java
    unaligned_apk = BUILD_DIR / "unaligned.apk"
    run_cmd([
        AAPT2, "link",
        "-I", ANDROID_JAR,
        "--manifest", LIHONB_DIR / "AndroidManifest.xml",
        "-o", unaligned_apk,
        "--java", GEN_DIR,
        "--auto-add-overlay",
        compiled_res
    ], "Linking APK package and generating R.java")

    # 3. Find all java sources (R.java + src)
    java_files = list((LIHONB_DIR / "src").rglob("*.java")) + list(GEN_DIR.rglob("*.java"))
    print(f"   Found {len(java_files)} Java source files")

    # 4. Compile Java sources with javac
    run_cmd([
        "javac",
        "--release", "8",
        "-cp", ANDROID_JAR,
        "-d", classes_dir,
        *java_files
    ], "Compiling Java bytecode with javac")

    # 5. Convert class files to classes.dex with modern R8/D8
    class_files = list(classes_dir.rglob("*.class"))
    print(f"   D8 processing {len(class_files)} class files")
    r8_jar = ROOT / "tools" / "r8.jar"
    run_cmd([
        "java", "-cp", str(r8_jar),
        "com.android.tools.r8.D8",
        "--lib", str(ANDROID_JAR),
        "--output", str(dex_dir),
        *[str(cf) for cf in class_files]
    ], "Generating classes.dex with modern D8")

    # 6. Inject classes.dex into unaligned.apk
    dex_file = dex_dir / "classes.dex"
    if not dex_file.exists():
        print("ERROR: classes.dex was not generated!")
        sys.exit(1)

    print("-> Packing classes.dex into unaligned.apk...")
    with zipfile.ZipFile(unaligned_apk, "a") as z:
        z.write(dex_file, "classes.dex")

        # Package assets
        assets_dir = LIHONB_DIR / "assets"
        if assets_dir.exists():
            for f in assets_dir.rglob("*"):
                if f.is_file():
                    arcname = "assets/" + f.relative_to(assets_dir).as_posix()
                    z.write(f, arcname)

    # 7. Sign APK with uber-apk-signer
    print("-> Signing APK with uber-apk-signer...")
    run_cmd([
        "java", "-jar", UBER_SIGNER,
        "--apks", unaligned_apk,
        "--out", BUILD_DIR / "signed"
    ], "Signing APK")

    # Find the signed output
    signed_apks = list((BUILD_DIR / "signed").glob("*.apk"))
    if not signed_apks:
        print("ERROR: Signed APK not found!")
        sys.exit(1)

    final_signed = signed_apks[0]
    shutil.copy2(final_signed, FINAL_APK)
    shutil.copy2(final_signed, DESKTOP_APK)

    size_mb = FINAL_APK.stat().st_size / (1024 * 1024)
    print("=" * 60)
    print("SUCCESS! LihonB 1.0.0-beta built successfully!")
    print(f"Output APK: {FINAL_APK} ({size_mb:.2f} MB)")
    print(f"Copied to Desktop: {DESKTOP_APK}")
    print("=" * 60)


if __name__ == "__main__":
    main()
