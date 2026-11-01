#!/usr/bin/env bash
# One-time setup for signed BrinaGram release builds on GitHub Actions.
#
# Creates a private release keystore OUTSIDE the repository (~/.brinagram) and stores it,
# plus its passwords, as GitHub repository secrets used by .github/workflows/release.yml.
#
# Usage:  bash Tools/ci/setup_signing.sh [owner/repo]
# Needs:  keytool (any JDK), gh (logged in), node or openssl (for a random password).
#
# Optional: export TELEGRAM_APP_ID / TELEGRAM_APP_HASH (from https://my.telegram.org)
# before running, and they are stored as secrets too.
set -euo pipefail

REPO="${1:-ozyern/BrinaGram}"
DIR="$HOME/.brinagram"
KS="$DIR/brinagram-release.jks"
ALIAS="brinagram"

KEYTOOL="$(command -v keytool || true)"
if [ -z "$KEYTOOL" ]; then
  for k in "$HOME"/.jdks/*/bin/keytool "$HOME"/.jdks/*/bin/keytool.exe; do
    [ -x "$k" ] && KEYTOOL="$k" && break
  done
fi
[ -n "$KEYTOOL" ] || { echo "keytool not found (install a JDK)"; exit 1; }

mkdir -p "$DIR"
if [ -f "$KS" ]; then
  echo "Using existing keystore $KS"
  PASS="$(grep '^store password:' "$DIR/CREDENTIALS.txt" | cut -d' ' -f3)"
else
  if command -v node >/dev/null; then
    PASS="$(node -e "console.log(require('crypto').randomBytes(24).toString('base64url'))")"
  else
    PASS="$(openssl rand -base64 24 | tr -d '/+=')"
  fi
  "$KEYTOOL" -genkeypair -keystore "$KS" -storetype PKCS12 -alias "$ALIAS" \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$PASS" -keypass "$PASS" -dname "CN=BrinaGram, O=ozyern" >/dev/null
  printf 'BrinaGram release signing key -- BACK THIS UP. Losing it means installed apps cannot be updated.\nkeystore: %s\nalias: %s\nstore password: %s\nkey password: %s\n' \
    "$KS" "$ALIAS" "$PASS" "$PASS" > "$DIR/CREDENTIALS.txt"
  echo "Created $KS (credentials in $DIR/CREDENTIALS.txt)"
fi

base64 < "$KS" | tr -d '\n' | gh secret set BRINA_KEYSTORE_BASE64 -R "$REPO"
printf '%s' "$PASS"  | gh secret set BRINA_KEYSTORE_PASSWORD -R "$REPO"
printf '%s' "$PASS"  | gh secret set BRINA_KEY_PASSWORD -R "$REPO"
printf '%s' "$ALIAS" | gh secret set BRINA_KEY_ALIAS -R "$REPO"

if [ -n "${TELEGRAM_APP_ID:-}" ] && [ -n "${TELEGRAM_APP_HASH:-}" ]; then
  printf '%s' "$TELEGRAM_APP_ID"   | gh secret set TELEGRAM_APP_ID -R "$REPO"
  printf '%s' "$TELEGRAM_APP_HASH" | gh secret set TELEGRAM_APP_HASH -R "$REPO"
  echo "Stored Telegram API credentials."
fi

echo "Done. Secrets now on $REPO:"
gh secret list -R "$REPO"
