#!/usr/bin/env bash
# =============================================================================
# push-ghcr.sh — Build & push all Aferent service images to GitHub Container Registry
#
# Prerequisites:
#   docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin <<< "$GHCR_TOKEN"
#   (GHCR_TOKEN = a GitHub Personal Access Token with write:packages scope)
#
# Usage:
#   ./scripts/push-ghcr.sh                      # push all, tag "latest"
#   ./scripts/push-ghcr.sh v1.0.0               # push all, tag "v1.0.0"
#   ./scripts/push-ghcr.sh latest auth-service  # one service, tag "latest"
# =============================================================================
set -euo pipefail

# Find project root (where docker-compose.yml is)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

GHCR_OWNER="nathimn"   # GitHub username (must be lowercase)

TAG="${1:-latest}"
shift 2>/dev/null || true

ALL_SERVICES=(
  auth-service
  api-gateway
  patient-service
  doctor-service
  notification-service
  appointment-service
  payment-service
  telemedicine-service
  document-service
  symptom-detector
)

SERVICES=("${ALL_SERVICES[@]}")
if [[ $# -gt 0 ]]; then
  SERVICES=("$@")
fi

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
log()  { echo -e "${GREEN}[✓]${NC} $*"; }
fail() { echo -e "${RED}[✗]${NC} $*"; exit 1; }

echo ""
echo "================================================"
echo "  Push to GHCR  (tag: $TAG)"
echo "================================================"
echo ""

docker compose build "${SERVICES[@]}"
echo ""

for svc in "${SERVICES[@]}"; do
  tag="ghcr.io/${GHCR_OWNER}/aferent-${svc}:${TAG}"
  docker tag "aferent-${svc}" "$tag"
  docker push "$tag" && log "$tag" || fail "Push failed for $tag"
done

echo ""
log "Done. Images are at ghcr.io/${GHCR_OWNER}/"
