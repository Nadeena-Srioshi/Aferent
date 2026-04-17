#!/usr/bin/env bash
# =============================================================================
# env-to-secrets.sh — Parse .env and populate k8s/secrets.yaml with base64 values
#
# This script:
#   1. Reads .env file from project root
#   2. Skips commented lines (starting with #)
#   3. Converts each value to base64
#   4. Updates k8s/secrets.yaml with the encoded values
#
# Usage:
#   ./scripts/env-to-secrets.sh
#
# CAUTION: This modifies k8s/secrets.yaml in-place. Review before committing!
# =============================================================================

set -euo pipefail

# Find project root (where .env is)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

ENV_FILE=".env"
SECRETS_FILE="k8s/secrets.yaml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Error: $ENV_FILE not found in $PROJECT_ROOT"
  exit 1
fi

if [[ ! -f "$SECRETS_FILE" ]]; then
  echo "Error: $SECRETS_FILE not found"
  exit 1
fi

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[✓]${NC} $*"; }
warn() { echo -e "${YELLOW}[!]${NC} $*"; }

TEMP_SECRETS=$(mktemp)
cp "$SECRETS_FILE" "$TEMP_SECRETS"

echo "Reading from $ENV_FILE..."
echo ""

COUNT=0
while IFS='=' read -r key value; do
  # Skip empty lines and lines starting with #
  [[ -z "$key" || "$key" =~ ^# ]] && continue
  
  key=$(echo "$key" | xargs)  # trim whitespace
  value=$(echo "$value" | xargs)  # trim whitespace
  
  [[ -z "$key" || -z "$value" ]] && continue
  
  # Encode to base64 (single line — no wrapping)
  encoded=$(echo -n "$value" | base64 -w 0)
  
  # Update secrets file: find and replace the value for this key
  if grep -q "^\s*$key:" "$TEMP_SECRETS"; then
    sed -i "s|^\(\s*$key:\) .*$|\1 $encoded|" "$TEMP_SECRETS"
    log "Updated $key"
    COUNT=$((COUNT + 1))
  else
    warn "Key '$key' not found in secrets.yaml (skipped)"
  fi
done < "$ENV_FILE"

mv "$TEMP_SECRETS" "$SECRETS_FILE"
echo ""
log "Done. Updated $COUNT keys in $SECRETS_FILE"
echo ""
echo "Next steps:"
echo "  1. Review the secrets file: cat $SECRETS_FILE"
echo "  2. Apply to cluster: kubectl apply -k k8s/"
