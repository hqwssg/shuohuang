#!/bin/sh
set -e

MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-1275410782h}
BUSINESS_SCHEMA_ONLY=${BUSINESS_SCHEMA_ONLY:-false}
DEPLOY_DIR=${DEPLOY_DIR:-/opt/ruoyi-deploy}
SQL_FILE=$DEPLOY_DIR/mysql/db/carbon_emissions.sql
BUSINESS_SCHEMA_FILE=$DEPLOY_DIR/mysql/db/schema_1.sql
LOGGING_MIGRATION_FILE=$DEPLOY_DIR/mysql/db/logging_migration.sql
MYSQL_SOCKET=${MYSQL_SOCKET:-/var/run/mysqld/mysqld.sock}
MYSQL_LOG=${MYSQL_LOG:-/var/log/ruoyi-mysql.log}

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

ensure_mysql() {
  if command -v mysqld >/dev/null 2>&1 || command -v mariadbd >/dev/null 2>&1; then
    return
  fi

  pkg_install mysql-server mysql || pkg_install mariadb-server mariadb
}

mysql_server_bin() {
  if command -v mariadbd >/dev/null 2>&1; then
    command -v mariadbd
  elif command -v mysqld >/dev/null 2>&1; then
    command -v mysqld
  else
    echo "ERROR: mysqld/mariadbd not found after package installation."
    exit 1
  fi
}

print_mysql_diagnostics() {
  echo "MySQL/MariaDB diagnostics:"
  for bin in mysqld mariadbd mysqld_safe mariadbd-safe; do
    if command -v "$bin" >/dev/null 2>&1; then
      path=$(command -v "$bin")
      echo "---- $bin: $path"
      ls -l "$path" 2>/dev/null || true
      ls -lL "$path" 2>/dev/null || true
      file "$path" 2>/dev/null || true
    fi
  done
  echo "---- process 1"
  ps -p 1 -o pid,comm,args 2>/dev/null || true
  echo "---- capabilities"
  grep -E 'Cap(Inh|Prm|Eff|Bnd|Amb)' /proc/1/status 2>/dev/null || true
  echo "---- mounts"
  mount 2>/dev/null | grep -E ' on / | on /usr |noexec' || true
  echo "---- selinux"
  getenforce 2>/dev/null || true
  echo "---- recent log"
  tail -120 "$MYSQL_LOG" 2>/dev/null || true
}

try_service_start() {
  if command -v systemctl >/dev/null 2>&1 && [ -d /run/systemd/system ]; then
    systemctl start mysqld 2>/dev/null || systemctl start mariadb 2>/dev/null || true
  fi
  if command -v service >/dev/null 2>&1; then
    service mysqld start 2>/dev/null || service mariadb start 2>/dev/null || true
  fi
}

try_direct_start() {
  if command -v mariadbd-safe >/dev/null 2>&1; then
    nohup mariadbd-safe --datadir=/var/lib/mysql --user=mysql --bind-address=0.0.0.0 --socket="$MYSQL_SOCKET" --port=3306 > "$MYSQL_LOG" 2>&1 &
  elif command -v mysqld_safe >/dev/null 2>&1; then
    nohup mysqld_safe --datadir=/var/lib/mysql --user=mysql --bind-address=0.0.0.0 --socket="$MYSQL_SOCKET" --port=3306 > "$MYSQL_LOG" 2>&1 &
  else
    MYSQLD_BIN=$(mysql_server_bin)
    nohup "$MYSQLD_BIN" --datadir=/var/lib/mysql --user=mysql --bind-address=0.0.0.0 --socket="$MYSQL_SOCKET" --port=3306 > "$MYSQL_LOG" 2>&1 &
  fi
}

start_mysql() {
  mkdir -p /var/lib/mysql /var/run/mysqld
  if id mysql >/dev/null 2>&1; then
    chown -R mysql:mysql /var/lib/mysql /var/run/mysqld
  fi

  if [ ! -d /var/lib/mysql/mysql ]; then
    MYSQLD_BIN=$(mysql_server_bin)
    if "$MYSQLD_BIN" --verbose --help 2>/dev/null | grep -q initialize; then
      "$MYSQLD_BIN" --initialize-insecure --user=mysql --datadir=/var/lib/mysql || true
    fi
    if [ ! -d /var/lib/mysql/mysql ] && command -v mariadb-install-db >/dev/null 2>&1; then
      mariadb-install-db --user=mysql --datadir=/var/lib/mysql
    elif [ ! -d /var/lib/mysql/mysql ] && command -v mysql_install_db >/dev/null 2>&1; then
      mysql_install_db --user=mysql --datadir=/var/lib/mysql
    fi
  fi

  if ! pgrep -x mysqld >/dev/null 2>&1 && ! pgrep -x mariadbd >/dev/null 2>&1; then
    try_service_start
    sleep 2
  fi

  if ! pgrep -x mysqld >/dev/null 2>&1 && ! pgrep -x mariadbd >/dev/null 2>&1; then
    try_direct_start
  fi

  i=0
  while ! mysqladmin -uroot ping --silent >/dev/null 2>&1 && \
        ! mysqladmin -uroot -p"$MYSQL_ROOT_PASSWORD" ping --silent >/dev/null 2>&1 && \
        ! mysqladmin --protocol=TCP -h127.0.0.1 -P3306 -uroot ping --silent >/dev/null 2>&1 && \
        ! mysqladmin --protocol=TCP -h127.0.0.1 -P3306 -uroot -p"$MYSQL_ROOT_PASSWORD" ping --silent >/dev/null 2>&1; do
    i=$((i + 1))
    if [ "$i" -ge 60 ]; then
      echo "ERROR: MySQL/MariaDB did not become ready in 60 seconds."
      print_mysql_diagnostics
      if grep -qi "Operation not permitted" "$MYSQL_LOG" 2>/dev/null; then
        echo "ERROR: The existing database container does not allow executing MySQL/MariaDB."
        echo "Recreate the database container from an openEuler image with proper runtime permissions."
      fi
      exit 1
    fi
    sleep 1
  done
}

mysql_exec() {
  sql=$1
  mysql -uroot -e "$sql" 2>/dev/null || \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "$sql" 2>/dev/null || \
    mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot -e "$sql" 2>/dev/null || \
    mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot -p"$MYSQL_ROOT_PASSWORD" -e "$sql"
}

mysql_import() {
  file=$1
  mysql -uroot < "$file" 2>/dev/null || \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < "$file" 2>/dev/null || \
    mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot < "$file" 2>/dev/null || \
    mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot -p"$MYSQL_ROOT_PASSWORD" < "$file"
}

ensure_mysql
start_mysql

mysql_exec "CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '$MYSQL_ROOT_PASSWORD';"
mysql_exec "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;"
mysql_exec "ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PASSWORD';" || true
mysql_exec "FLUSH PRIVILEGES;"
mysql_exec "CREATE DATABASE IF NOT EXISTS carbon_emissions DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

if [ "$BUSINESS_SCHEMA_ONLY" = "true" ]; then
  echo "Skipping RuoYi schema import (BUSINESS_SCHEMA_ONLY=true)."
else
  if [ -f "$SQL_FILE" ]; then
    echo "Importing RuoYi schema from $SQL_FILE ..."
    mysql_import "$SQL_FILE"
  else
    echo "WARN: SQL file not found: $SQL_FILE"
  fi
fi

if [ -f "$BUSINESS_SCHEMA_FILE" ]; then
  echo "Importing carbon business schema from $BUSINESS_SCHEMA_FILE ..."
  mysql_import "$BUSINESS_SCHEMA_FILE"
else
  echo "WARN: SQL file not found: $BUSINESS_SCHEMA_FILE"
fi

if [ -f "$LOGGING_MIGRATION_FILE" ]; then
  echo "Applying logging migration from $LOGGING_MIGRATION_FILE ..."
  mysql_import "$LOGGING_MIGRATION_FILE"
else
  echo "WARN: logging migration not found: $LOGGING_MIGRATION_FILE"
fi

cat >/usr/local/bin/start-ruoyi-db.sh <<'EOF'
#!/bin/sh
MYSQL_SOCKET=${MYSQL_SOCKET:-/var/run/mysqld/mysqld.sock}
MYSQL_LOG=${MYSQL_LOG:-/var/log/ruoyi-mysql.log}
mkdir -p /var/run/mysqld
if id mysql >/dev/null 2>&1; then
  chown -R mysql:mysql /var/run/mysqld
fi
if ! pgrep -x mysqld >/dev/null 2>&1 && ! pgrep -x mariadbd >/dev/null 2>&1; then
  if command -v mariadbd-safe >/dev/null 2>&1; then
    nohup mariadbd-safe --datadir=/var/lib/mysql --user=mysql --bind-address=0.0.0.0 --socket="$MYSQL_SOCKET" --port=3306 > "$MYSQL_LOG" 2>&1 &
  elif command -v mysqld_safe >/dev/null 2>&1; then
    nohup mysqld_safe --datadir=/var/lib/mysql --user=mysql --bind-address=0.0.0.0 --socket="$MYSQL_SOCKET" --port=3306 > "$MYSQL_LOG" 2>&1 &
  elif command -v mariadbd >/dev/null 2>&1; then
    nohup mariadbd --datadir=/var/lib/mysql --user=mysql --bind-address=0.0.0.0 --socket="$MYSQL_SOCKET" --port=3306 > "$MYSQL_LOG" 2>&1 &
  else
    nohup mysqld --datadir=/var/lib/mysql --user=mysql --bind-address=0.0.0.0 --socket="$MYSQL_SOCKET" --port=3306 > "$MYSQL_LOG" 2>&1 &
  fi
fi
EOF
chmod +x /usr/local/bin/start-ruoyi-db.sh

echo "Database container setup finished."
