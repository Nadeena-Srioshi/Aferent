#!/usr/bin/env bash
# =============================================================================
# push-dockerhub.sh — Build & push all Aferent service images to Docker Hub
#
# Prerequisites:
#   docker login -u YOUR_DOCKERHUB_USERNAME
#   (enter your Docker Hub password or access token when prompted)
#
# Usage:
#   ./scripts/push-dockerhub.sh                 # push all, tag "latest"
#   ./scripts/push-dockerhub.sh v1.0.0          # push all, tag "v1.0.0"
#   ./scripts/push-dockerhub.sh latest auth-svc # one service, tag "latest"
# =============================================================================
set -euo pipefail

# Find project root (where docker-compose.yml is)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

DOCKERHUB_OWNER="nathimn"   # Docker Hub username — change if different

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
echo "  Push to Docker Hub  (tag: $TAG)"
echo "================================================"
echo ""

docker compose build "${SERVICES[@]}"
echo ""

for svc in "${SERVICES[@]}"; do
  tag="${DOCKERHUB_OWNER}/aferent-${svc}:${TAG}"
  docker tag "aferent-${svc}" "$tag"
  docker push "$tag" && log "$tag" || fail "Push failed for $tag"
done

echo ""
log "Done. Images are at hub.docker.com/u/${DOCKERHUB_OWNER}/"
