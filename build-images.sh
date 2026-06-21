#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="${AWS_REGION:-eu-west-2}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text)}"
ECR_REGISTRY="${ECR_REGISTRY:-${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com}"
TAG="${TAG:-dev-$(git rev-parse --short HEAD)}"
PLATFORM="${PLATFORM:-linux/amd64}"

SERVICES=(auth-service user-service crud-service)
PUSH=false

usage() {
  cat <<USAGE
Usage: ./build-images.sh [--push] [--tag TAG] [--platform PLATFORM]

Examples:
  ./build-images.sh
  ./build-images.sh --push
  ./build-images.sh --push --tag dev-$(git rev-parse --short HEAD)
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push) PUSH=true; shift ;;
    --tag) TAG="$2"; shift 2 ;;
    --platform) PLATFORM="$2"; shift 2 ;;
    --help|-h) usage; exit 0 ;;
    *) echo "Unknown argument: $1"; usage; exit 1 ;;
  esac
done

if [[ "$PUSH" == true ]]; then
  aws ecr get-login-password --region "$AWS_REGION" \
    | docker login --username AWS --password-stdin "$ECR_REGISTRY"
fi

for service in "${SERVICES[@]}"; do
  image="${ECR_REGISTRY}/${service}:${TAG}"

  docker buildx build \
    --platform "$PLATFORM" \
    --tag "$image" \
    --file "services/${service}/Dockerfile" \
    "$([[ "$PUSH" == true ]] && echo --push || echo --load)" \
    "services/${service}"
done

echo "Built ${#SERVICES[@]} images with tag: ${TAG}"
