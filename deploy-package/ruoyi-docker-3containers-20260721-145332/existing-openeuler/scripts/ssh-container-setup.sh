#!/bin/sh
set -e

if [ -z "${CONTAINER_SSH_PASSWORD:-}" ]; then
  echo "ERROR: CONTAINER_SSH_PASSWORD is required."
  exit 1
fi

pkg_install() {
  if command -v dnf >/dev/null 2>&1; then
    dnf install -y "$@"
  elif command -v yum >/dev/null 2>&1; then
    yum install -y "$@"
  else
    echo "ERROR: dnf/yum not found in this openEuler container."
    exit 1
  fi
}

if ! command -v sshd >/dev/null 2>&1 || ! command -v chpasswd >/dev/null 2>&1; then
  pkg_install openssh-server openssh-clients passwd
fi

ssh-keygen -A
printf 'root:%s\n' "$CONTAINER_SSH_PASSWORD" | chpasswd

set_sshd_option() {
  option=$1
  value=$2
  config=/etc/ssh/sshd_config

  if grep -Eq "^[[:space:]#]*${option}[[:space:]]+" "$config"; then
    sed -i -E "s|^[[:space:]#]*${option}[[:space:]]+.*|${option} ${value}|" "$config"
  else
    printf '%s %s\n' "$option" "$value" >> "$config"
  fi
}

set_sshd_option PermitRootLogin yes
set_sshd_option PasswordAuthentication yes
set_sshd_option UsePAM yes

cat >/usr/local/bin/start-container-sshd.sh <<'EOF'
#!/bin/sh
mkdir -p /run/sshd
ssh-keygen -A
if ! pgrep -x sshd >/dev/null 2>&1; then
  /usr/sbin/sshd
fi
EOF
chmod +x /usr/local/bin/start-container-sshd.sh

sh /usr/local/bin/start-container-sshd.sh
/usr/sbin/sshd -t

echo "Container SSH setup finished."
