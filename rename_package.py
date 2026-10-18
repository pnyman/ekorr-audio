"""
Byter rotpaket i ekorr-audio:

    se.camabomedia.ekorr.audio.playback  ->  se.camabomedia.ekorraudio.playback
    se.camabomedia.ekorr                 ->  se.camabomedia.ekorraudio

Flyttar katalogerna under src/main/java, src/test/java och src/main/resources
och uppdaterar paketnamn och resurssökvägar i källkod, FXML, pom.xml och .cmd-filer.

Kör från projektroten (där pom.xml ligger):

    python rename_package.py --dry-run   # visa vad som skulle göras
    python rename_package.py             # gör det
"""

import re
import shutil
import sys
from pathlib import Path

SOURCE_DIRS = ["src/main/java", "src/test/java", "src/main/resources"]

# Filtyper som kan innehålla paketnamn eller resurssökvägar
TEXT_SUFFIXES = {".java", ".fxml", ".css", ".html", ".xml", ".properties", ".org", ".cmd"}

# Ordningen spelar roll: playback först. \b gör att "ekorraudio" inte matchas igen.
REPLACEMENTS = [
    (rb"se\.camabomedia\.ekorr\.audio\.playback", b"se.camabomedia.ekorraudio.playback"),
    (rb"se\.camabomedia\.ekorr\b", b"se.camabomedia.ekorraudio"),
    (rb"se/camabomedia/ekorr/audio/playback", b"se/camabomedia/ekorraudio/playback"),
    (rb"se/camabomedia/ekorr\b", b"se/camabomedia/ekorraudio"),
]


def move_directories(root: Path, dry_run: bool) -> None:
    for d in SOURCE_DIRS:
        old = root / d / "se" / "camabomedia" / "ekorr"
        new = root / d / "se" / "camabomedia" / "ekorraudio"
        if not old.is_dir():
            continue
        if new.exists():
            sys.exit(f"Avbryter: {new} finns redan.")

        print(f"Flyttar {old.relative_to(root)} -> {new.relative_to(root)}")
        if not dry_run:
            shutil.move(str(old), str(new))

        base = old if dry_run else new
        playback = base / "audio" / "playback"
        if playback.is_dir():
            print(f"Flyttar {d}/.../audio/playback -> {d}/.../ekorraudio/playback")
            if not dry_run:
                shutil.move(str(playback), str(new / "playback"))
                audio = new / "audio"
                if not any(audio.iterdir()):
                    audio.rmdir()
                else:
                    print(f"  OBS: {audio.relative_to(root)} är inte tom, lämnas kvar")


def candidate_files(root: Path):
    yield root / "pom.xml"
    yield from root.glob("*.cmd")
    src = root / "src"
    for p in src.rglob("*"):
        if p.is_file() and p.suffix.lower() in TEXT_SUFFIXES:
            yield p


def update_files(root: Path, dry_run: bool) -> None:
    for path in candidate_files(root):
        if not path.is_file():
            continue
        # Arbeta på bytes så att teckenkodning och radslut (CRLF) bevaras exakt
        data = path.read_bytes()
        new_data = data
        count = 0
        for pattern, repl in REPLACEMENTS:
            new_data, n = re.subn(pattern, repl, new_data)
            count += n
        if count:
            print(f"{count:4d} ändringar i {path.relative_to(root)}")
            if not dry_run:
                path.write_bytes(new_data)


def main() -> None:
    dry_run = "--dry-run" in sys.argv
    root = Path.cwd()

    if not (root / "pom.xml").is_file():
        sys.exit("Hittar ingen pom.xml – kör skriptet från projektroten.")

    if dry_run:
        print("*** TORRKÖRNING – inget ändras ***\n")

    target = root / "target"
    if target.is_dir():
        print("Tar bort target/")
        if not dry_run:
            shutil.rmtree(target)

    move_directories(root, dry_run)
    # Vid torrkörning ligger filerna kvar på gamla platsen, men sökningen
    # går igenom hela src/ så resultatet blir detsamma.
    update_files(root, dry_run)

    print("\nKlart." if not dry_run else "\nTorrkörning klar.")
    if not dry_run:
        print("Kör nu: mvn test   (och starta run.cmd för att kontrollera att vyerna laddas)")


if __name__ == "__main__":
    main()
