#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Install the nginx reverse-proxy config for the Profession Aptitude API.

Required environment variables:
  LIGHTSAIL_HOST    Public Lightsail static IP or hostname.

Optional environment variables:
  SSH_USER          Lightsail SSH user. Defaults to ec2-user.
  SSH_KEY           Path to the private SSH key for the Lightsail instance.
  BACKEND_PORT      Local Spring Boot port. Defaults to 8080.
  EXTRA_SERVER_NAMES
                    Extra nginx server_name values, separated by spaces.

Example:
  LIGHTSAIL_HOST=44.222.68.217 ./scripts/configure-nginx.sh
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

require_env LIGHTSAIL_HOST
require_command ssh

SSH_USER="${SSH_USER:-ec2-user}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
EXTRA_SERVER_NAMES="${EXTRA_SERVER_NAMES:-}"
SSH_TARGET="$SSH_USER@$LIGHTSAIL_HOST"
SSH_ARGS=()

if [[ -n "${SSH_KEY:-}" ]]; then
  SSH_ARGS=(-i "$SSH_KEY")
fi

echo "Installing nginx config on $SSH_TARGET..."

ssh "${SSH_ARGS[@]}" "$SSH_TARGET" \
  "LIGHTSAIL_HOST='$LIGHTSAIL_HOST' BACKEND_PORT='$BACKEND_PORT' EXTRA_SERVER_NAMES='$EXTRA_SERVER_NAMES' bash -s" <<'REMOTE'
set -euo pipefail

server_names="$LIGHTSAIL_HOST"
if [[ -n "$EXTRA_SERVER_NAMES" ]]; then
  server_names="$server_names $EXTRA_SERVER_NAMES"
fi

tmp_config="$(mktemp)"
cat > "$tmp_config" <<NGINX
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;
    return 444;
}

server {
    listen 80;
    listen [::]:80;
    server_name $server_names;

    location / {
        proxy_pass http://127.0.0.1:$BACKEND_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$server_name;
        proxy_set_header X-Forwarded-Host \$host;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }
}
NGINX

timestamp="$(date +%Y%m%d%H%M%S)"
backup_dir="/etc/nginx/conf.d/backup-$timestamp"
target_config="/etc/nginx/conf.d/profession-aptitude-api.conf"
sudo mkdir -p "$backup_dir"

if compgen -G "/etc/nginx/conf.d/*.conf" >/dev/null; then
  sudo cp /etc/nginx/conf.d/*.conf "$backup_dir"/
fi

sudo install -m 0644 "$tmp_config" "$target_config"
rm -f "$tmp_config"

if ! sudo nginx -t; then
  echo "nginx config test failed; restoring previous config." >&2
  if compgen -G "$backup_dir/*.conf" >/dev/null; then
    sudo cp "$backup_dir"/*.conf /etc/nginx/conf.d/
  else
    sudo rm -f "$target_config"
  fi
  sudo nginx -t
  exit 1
fi

sudo systemctl reload nginx
REMOTE

echo "nginx config installed."
