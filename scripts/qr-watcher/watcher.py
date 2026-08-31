#!/usr/bin/env python3
"""
Cloudflare Quick Tunnel watcher + QR code generator.

Runs cloudflared as a subprocess, captures the generated tunnel URL,
writes it to a shared volume, and generates a QR code PNG that encodes
the customer-facing request-form page.
"""

import os
import re
import signal
import subprocess
import sys
from pathlib import Path

URL_PATTERN = re.compile(r"(https://[a-zA-Z0-9\-]+\.trycloudflare\.com)")

OUTPUT_DIR = Path(os.environ.get("QR_OUTPUT_DIR", "/qr-data"))
TUNNEL_HOST = os.environ.get("TUNNEL_HOST", "http://app:8080")
FORM_PATH = os.environ.get("FORM_PATH", "/request-form")
POLL_INTERVAL = int(os.environ.get("POLL_INTERVAL", "5"))

URL_FILE = OUTPUT_DIR / "tunnel-url.txt"
QR_FILE = OUTPUT_DIR / "qr-code.png"
QR_URL_FILE = OUTPUT_DIR / "qr-url.txt"


def log(msg: str) -> None:
    print(f"[qr-watcher] {msg}", flush=True)


def generate_qr(url: str) -> None:
    import qrcode
    from qrcode.image.pil import PilImage

    qr = qrcode.QRCode(
        version=None,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=10,
        border=4,
    )
    qr.add_data(url)
    qr.make(fit=True)

    img: PilImage = qr.make_image(fill_color="black", back_color="white")
    img.save(str(QR_FILE))
    log(f"QR code saved to {QR_FILE}")


def write_url_files(url: str) -> None:
    URL_FILE.write_text(url, encoding="utf-8")
    log(f"Tunnel URL written to {URL_FILE}")

    qr_url = url.rstrip("/") + FORM_PATH
    QR_URL_FILE.write_text(qr_url, encoding="utf-8")
    log(f"QR target URL ({qr_url}) written to {QR_URL_FILE}")


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    log(f"Starting cloudflared tunnel -> {TUNNEL_HOST}")
    log("Waiting for tunnel URL ...")

    proc = subprocess.Popen(
        ["cloudflared", "tunnel", "--url", TUNNEL_HOST],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )

    # Graceful shutdown
    def _shutdown(signum, _frame):
        log(f"Received signal {signum}, shutting down cloudflared ...")
        proc.send_signal(signal.SIGINT)
        sys.exit(0)

    signal.signal(signal.SIGTERM, _shutdown)
    signal.signal(signal.SIGINT, _shutdown)

    url_found = False

    try:
        for line in proc.stdout:  # type: ignore[union-attr]
            stripped = line.rstrip()
            if stripped:
                log(f"[cloudflared] {stripped}")

            match = URL_PATTERN.search(stripped)
            if match and not url_found:
                tunnel_url = match.group(1)
                log(f"Tunnel URL discovered: {tunnel_url}")

                write_url_files(tunnel_url)

                qr_url = tunnel_url.rstrip("/") + FORM_PATH
                generate_qr(qr_url)

                log("QR generation complete. Tunnel is live.")
                url_found = True
    except (KeyboardInterrupt, BrokenPipeError):
        log("Interrupted.")
    finally:
        if proc.poll() is None:
            proc.send_signal(signal.SIGINT)
            proc.wait(timeout=10)
        log("Stopped.")


if __name__ == "__main__":
    main()
