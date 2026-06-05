#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Build and deploy the What Is My Destiny app to AWS.

Required environment variables:
  AWS_PROFILE       AWS CLI profile to use.
  BUCKET            S3 bucket that hosts the React app.
  LIGHTSAIL_HOST    Lightsail static IP or hostname.

Optional environment variables:
  AWS_REGION        AWS region. Defaults to us-east-1.
  SSH_USER          Lightsail SSH user. Defaults to ec2-user.
  SSH_KEY           Path to the private SSH key for the Lightsail instance.
  BACKEND_API_URL   Public backend API base URL.
                    Defaults to http://$LIGHTSAIL_HOST/api/v1.
  SERVICE_NAME      systemd service name.
                    Defaults to profession-aptitude-api.

Example:
  AWS_PROFILE=AdministratorAccess-305814652382 \
  BUCKET=what-is-your-destiny-acorobceanu \
  LIGHTSAIL_HOST=203.0.113.10 \
  ./scripts/deploy-aws.sh
EOF
}

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    echo >&2
    usage >&2
    exit 1
  fi
}

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_env AWS_PROFILE
require_env BUCKET
require_env LIGHTSAIL_HOST

require_command aws
require_command curl
require_command mvn
require_command npm
require_command ssh

AWS_REGION="${AWS_REGION:-us-east-1}"
SSH_USER="${SSH_USER:-ec2-user}"
SERVICE_NAME="${SERVICE_NAME:-profession-aptitude-api}"
BACKEND_API_URL="${BACKEND_API_URL:-http://${LIGHTSAIL_HOST}/api/v1}"
SSH_TARGET="$SSH_USER@$LIGHTSAIL_HOST"
SSH_ARGS=()

if [[ -n "${SSH_KEY:-}" ]]; then
  SSH_ARGS=(-i "$SSH_KEY")
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UI_DIR="$ROOT_DIR/what-is-my-destiny"
JAR_NAME="profession-aptitude-api-0.0.1-SNAPSHOT.jar"
JAR_PATH="$ROOT_DIR/target/$JAR_NAME"
REMOTE_JAR_PATH="/opt/profession-aptitude-api/profession-aptitude-api.jar"
REMOTE_TMP_JAR_PATH="/tmp/profession-aptitude-api.jar.new"
S3_JAR_KEY="backend/profession-aptitude-api.jar"
S3_JAR_URI="s3://$BUCKET/$S3_JAR_KEY"
S3_WEBSITE_HOST="$BUCKET.s3-website-$AWS_REGION.amazonaws.com"
PUBLIC_JAR_URL="http://$S3_WEBSITE_HOST/$S3_JAR_KEY"

echo "Deploying with:"
echo "  AWS profile:      $AWS_PROFILE"
echo "  AWS region:       $AWS_REGION"
echo "  S3 bucket:        $BUCKET"
echo "  Lightsail host:   $LIGHTSAIL_HOST"
echo "  Backend API URL:  $BACKEND_API_URL"
echo

echo "Checking AWS access..."
aws sts get-caller-identity \
  --profile "$AWS_PROFILE" \
  --region "$AWS_REGION" >/dev/null

echo "Building backend jar..."
cd "$ROOT_DIR"
mvn package -DskipTests

echo "Uploading backend jar temporarily to S3..."
aws s3 cp "$JAR_PATH" "$S3_JAR_URI" \
  --profile "$AWS_PROFILE" \
  --region "$AWS_REGION"

cleanup() {
  echo "Removing temporary backend jar from S3..."
  aws s3 rm "$S3_JAR_URI" \
    --profile "$AWS_PROFILE" \
    --region "$AWS_REGION" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Installing backend jar on Lightsail and restarting service..."
ssh "${SSH_ARGS[@]}" "$SSH_TARGET" \
  "curl -fsSL '$PUBLIC_JAR_URL' -o '$REMOTE_TMP_JAR_PATH' && sudo systemctl stop '$SERVICE_NAME' && sudo install -m 0644 '$REMOTE_TMP_JAR_PATH' '$REMOTE_JAR_PATH' && rm -f '$REMOTE_TMP_JAR_PATH' && sudo systemctl start '$SERVICE_NAME' && sudo systemctl is-active --quiet '$SERVICE_NAME'"

echo "Building frontend with backend API URL..."
cd "$UI_DIR"
VITE_API_BASE_URL="$BACKEND_API_URL" npm run build

echo "Syncing frontend dist to S3..."
aws s3 sync "$UI_DIR/dist" "s3://$BUCKET" \
  --delete \
  --profile "$AWS_PROFILE" \
  --region "$AWS_REGION"

echo
echo "Deployment complete."
echo "Frontend:"
echo "  http://$S3_WEBSITE_HOST"
echo "Backend API:"
echo "  $BACKEND_API_URL"
