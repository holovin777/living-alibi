"""Run on a native x64 CI runner with Python, Maven and JDK 21."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
import tarfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "4.6.1-stable"
WINDOWS = sys.platform == "win32"
PLATFORM = "windows-x64" if WINDOWS else "linux-x64"
OUT = ROOT / "dist"
PACKAGE = OUT / ("LivingAlibi-" + PLATFORM)
GODOT_RELEASE = f"https://github.com/godotengine/godot/releases/download/{VERSION}/"


def run(*args, env=None):
    print(">", *map(str, args), flush=True)
    subprocess.run([str(a) for a in args], cwd=ROOT, env=env, check=True)


def download(url, destination):
    print("Downloading", url, flush=True)
    with urllib.request.urlopen(url, timeout=120) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)


def godot_run(exe, *args):
    result = subprocess.run([str(exe), *map(str, args)], cwd=ROOT,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                            encoding="utf-8", errors="replace", timeout=180)
    print(result.stdout, flush=True)
    if result.returncode or any(word in result.stdout for word in
                               ("SCRIPT ERROR", "Parse Error", "Failed loading resource", "Export failed")):
        raise RuntimeError("Godot import/export failed")


def build():
    if PACKAGE.exists():
        raise RuntimeError(f"Remove previous staging folder before building: {PACKAGE}")
    PACKAGE.mkdir(parents=True)
    jdk = Path(os.environ["JAVA_HOME"])
    suffix = ".exe" if WINDOWS else ""
    maven = shutil.which("mvn.cmd" if WINDOWS else "mvn")
    if not maven:
        raise RuntimeError("Maven is required on the build machine")
    run(maven, "-B", "-ntp", "-DskipTests", "install")
    run(maven, "-B", "-ntp", "-f", "server/pom.xml", "test", "package")
    shutil.copy2(ROOT / "server/target/living-alibi-server-1.16.0.jar", PACKAGE / "server.jar")
    # A complete Java SE module set plus commonly required runtime providers.
    run(jdk / "bin" / ("jlink" + suffix), "--add-modules",
        "java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.charsets,jdk.management,jdk.jfr,jdk.localedata,jdk.naming.dns",
        "--strip-debug", "--no-header-files", "--no-man-pages", "--output", PACKAGE / "runtime")
    with tempfile.TemporaryDirectory(prefix="living-alibi-build-") as work:
        work = Path(work)
        classes = work / "classes"
        classes.mkdir()
        run(jdk / "bin" / ("javac" + suffix), "--release", "21", "-d", classes,
            ROOT / "packaging/Launcher.java")
        run(jdk / "bin" / ("jar" + suffix), "--create", "--file", PACKAGE / "launcher.jar",
            "--main-class", "Launcher", "-C", classes, ".")
        asset = f"Godot_v{VERSION}_" + ("win64.exe.zip" if WINDOWS else "linux.x86_64.zip")
        editor_zip = work / "editor.zip"
        download(GODOT_RELEASE + asset, editor_zip)
        with zipfile.ZipFile(editor_zip) as z:
            z.extractall(work / "editor")
        editor_name = f"Godot_v{VERSION}_" + ("win64_console.exe" if WINDOWS else "linux.x86_64")
        exe = next((work / "editor").rglob(editor_name))
        if not WINDOWS:
            exe.chmod(0o755)
        template_zip = work / "templates.tpz"
        download(GODOT_RELEASE + f"Godot_v{VERSION}_export_templates.tpz", template_zip)
        if WINDOWS:
            template_root = Path(os.environ["APPDATA"]) / "Godot/export_templates"
        else:
            template_root = Path(os.environ.get("XDG_DATA_HOME", Path.home() / ".local/share")) / "godot/export_templates"
        target = template_root / "4.6.1.stable"
        target.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(template_zip) as z:
            # Only the selected platform template is needed.
            template = "windows_release_x86_64.exe" if WINDOWS else "linux_release.x86_64"
            with z.open("templates/" + template) as source, (target / template).open("wb") as dest:
                shutil.copyfileobj(source, dest)
            if not WINDOWS:
                (target / template).chmod(0o755)
            # Version marker is used by the editor to locate installed templates.
            (target / "version.txt").write_text("4.6.1.stable\n", encoding="utf-8")
        game = PACKAGE / ("LivingAlibi.exe" if WINDOWS else "LivingAlibi.x86_64")
        godot_run(exe, "--headless", "--path", ROOT / "godot", "--editor", "--quit")
        godot_run(exe, "--headless", "--path", ROOT / "godot", "--export-release",
                  "Windows Desktop" if WINDOWS else "Linux", game)
        if not game.is_file():
            raise RuntimeError("Godot export is missing")
        if not WINDOWS:
            game.chmod(0o755)
        # Preserve the upstream engine's notices alongside its redistributed binary.
        for name in ("LICENSE.txt", "COPYRIGHT.txt"):
            download(f"https://raw.githubusercontent.com/godotengine/godot/{VERSION}/{name}",
                     PACKAGE / ("Godot-" + name))
        shutil.copy2(ROOT / "packaging/README.txt", PACKAGE / "README.txt")
        if WINDOWS:
            shutil.copy2(ROOT / "packaging/Play.cmd", PACKAGE / "Play.cmd")
        else:
            shutil.copy2(ROOT / "packaging/play.sh", PACKAGE / "play.sh")
            (PACKAGE / "play.sh").chmod(0o755)
        # Run the actual exported package with the bundled Java and a fresh save.
        env = os.environ.copy()
        env["LIVING_ALIBI_DATA_DIR"] = str(work / "fresh-world")
        try:
            run(PACKAGE / "runtime/bin" / ("java" + suffix), "-jar", PACKAGE / "launcher.jar", "--smoke", env=env)
        except subprocess.CalledProcessError:
            for log in ("server.log", "game.log"):
                p = Path(env["LIVING_ALIBI_DATA_DIR"]) / log
                if p.is_file():
                    print(p.read_text(encoding="utf-8", errors="replace"))
            raise

    if WINDOWS:
        archive = OUT / (PACKAGE.name + ".zip")
        with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
            for item in sorted(PACKAGE.rglob("*")):
                if item.is_file():
                    z.write(item, item.relative_to(OUT))
    else:
        archive = OUT / (PACKAGE.name + ".tar.gz")
        with tarfile.open(archive, "w:gz") as tar:
            tar.add(PACKAGE, arcname=PACKAGE.name)
    digest = hashlib.file_digest(archive.open("rb"), "sha256").hexdigest()
    (OUT / (archive.name + ".sha256")).write_text(f"{digest}  {archive.name}\n", encoding="ascii")
    print("Ready:", archive)


if __name__ == "__main__":
    build()
