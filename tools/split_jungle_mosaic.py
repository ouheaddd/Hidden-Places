from pathlib import Path
from PIL import Image
import argparse
import sys

TILE_SIZE = 16
TILES_WIDE = 7
TILES_HIGH = 5

EXPECTED_WIDTH = TILE_SIZE * TILES_WIDE      # 112
EXPECTED_HEIGHT = TILE_SIZE * TILES_HIGH     # 80

DEFAULT_SOURCE = Path("tools/jungle_mosaic_source.png")
DEFAULT_OUTPUT_DIR = Path("src/main/resources/assets/hidden_places/textures/block/jungle_mosaic")

PIECE_PREFIX = "piece_"


def get_project_root() -> Path:
    # tools/split_jungle_mosaic.py -> project root
    return Path(__file__).resolve().parents[1]


def fail(message: str) -> None:
    print(f"[ERROR] {message}")
    sys.exit(1)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Split a 112x80 jungle mosaic image into 35 Minecraft 16x16 texture pieces."
    )
    parser.add_argument(
        "source",
        nargs="?",
        default=str(DEFAULT_SOURCE),
        help="Source mosaic image. Default: tools/jungle_mosaic_source.png"
    )
    parser.add_argument(
        "--output",
        default=str(DEFAULT_OUTPUT_DIR),
        help="Output folder for piece_0.png ... piece_34.png"
    )
    args = parser.parse_args()

    project_root = get_project_root()

    source_path = Path(args.source)
    if not source_path.is_absolute():
        source_path = project_root / source_path

    output_dir = Path(args.output)
    if not output_dir.is_absolute():
        output_dir = project_root / output_dir

    if not source_path.exists():
        fail(f"Source image not found: {source_path}")

    output_dir.mkdir(parents=True, exist_ok=True)

    image = Image.open(source_path).convert("RGBA")

    if image.size != (EXPECTED_WIDTH, EXPECTED_HEIGHT):
        fail(
            f"Wrong image size: {image.size[0]}x{image.size[1]}. "
            f"Expected {EXPECTED_WIDTH}x{EXPECTED_HEIGHT}."
        )

    # Delete old generated pieces only.
    deleted = 0
    for old_file in output_dir.glob(f"{PIECE_PREFIX}*.png"):
        number_part = old_file.stem.removeprefix(PIECE_PREFIX)
        if number_part.isdigit():
            old_file.unlink()
            deleted += 1

    created = 0
    piece_index = 0

    for row in range(TILES_HIGH):
        for col in range(TILES_WIDE):
            left = col * TILE_SIZE
            top = row * TILE_SIZE
            right = left + TILE_SIZE
            bottom = top + TILE_SIZE

            tile = image.crop((left, top, right, bottom))
            tile.save(output_dir / f"{PIECE_PREFIX}{piece_index:02d}.png")

            piece_index += 1
            created += 1

    print("[OK] Jungle mosaic split complete.")
    print(f"Source:  {source_path}")
    print(f"Output:  {output_dir}")
    print(f"Deleted old pieces: {deleted}")
    print(f"Created new pieces: {created}")


if __name__ == "__main__":
    main()