#!/usr/bin/env python3
"""Download the app's webfonts (Amiri, Fraunces, Noto Serif Bengali) from Google Fonts
into web/fonts/ and write a self-contained fonts.css with local url()s."""
import pathlib
import re
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "web" / "fonts"
OUT.mkdir(parents=True, exist_ok=True)

UA = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}
FAMS = [
    "family=Amiri:wght@400;700",
    "family=Fraunces:opsz,wght@9..144,400;9..144,600",
    "family=Noto+Serif+Bengali:wght@400;600",
]

state = {"i": 0, "fail": 0}


def fetch(url: str, timeout: int = 60) -> bytes:
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=timeout).read()


def repl(match: re.Match) -> str:
    url = match.group(1).strip("\"'")
    ext = url.rsplit(".", 1)[-1].split("?")[0]
    state["i"] += 1
    name = f"f{state['i']}.{ext}"
    try:
        (OUT / name).write_bytes(fetch(url))
        return f"url({name})"
    except Exception as exc:  # keep the remote url as fallback
        state["fail"] += 1
        print("FAIL", url, exc)
        return match.group(0)


def main() -> None:
    chunks = []
    for fam in FAMS:
        css = fetch(f"https://fonts.googleapis.com/css2?{fam}&display=swap", timeout=30).decode()
        css = re.sub(r"url\(([^)]+)\)", repl, css)
        chunks.append(css)
    (OUT / "fonts.css").write_text("\n".join(chunks), encoding="utf-8")
    print(f"faces={state['i']} fail={state['fail']} -> {OUT / 'fonts.css'}")


if __name__ == "__main__":
    main()
