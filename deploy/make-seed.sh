#!/usr/bin/env bash
# ============================================================
# 生成部署用数据库种子（在你自己的电脑上执行）
# ------------------------------------------------------------
# 作用：把本地跑通的数据库整库导出成一个 sql 文件，
#      部署时由 MySQL 容器首次启动自动导入。
#
# 为什么不用项目里的 sql/*.sql：
#   1. load_word_data.sql 里的 LOAD DATA 写死了本机绝对路径
#      （D:/WorkBuddyFiles/task/cet6-build/word_import.tsv），
#      在服务器容器里必然找不到文件；
#   2. 那些脚本只建表、不含词库与文章数据，导完是个空壳；
#   3. 整库导出一次搞定 17 张表 + 全部内容数据 + 演示账号，
#      也不必再操心多个脚本的执行顺序。
#
# 用法：
#   bash deploy/make-seed.sh
#   密码不是 root 时：MYSQL_PASSWORD=你的密码 bash deploy/make-seed.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${SCRIPT_DIR}/seed/cet6_sprint.sql"

# 允许通过环境变量覆盖；默认按本机 MySQL 安装位置
MYSQL_BIN="${MYSQL_BIN:-/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DB="${MYSQL_DB:-cet6_sprint}"

[ -x "$MYSQL_BIN" ] || {
  echo "[ 失败 ] 找不到 mysqldump：$MYSQL_BIN"
  echo "         可用 MYSQL_BIN=/你的路径/mysqldump.exe bash deploy/make-seed.sh 指定"
  exit 1
}

mkdir -p "${SCRIPT_DIR}/seed"

echo "正在从本地 MySQL 导出 ${MYSQL_DB} ……"

# --databases      带上 CREATE DATABASE / USE，任何导入方式都自洽
# --single-transaction  一致性快照，导出期间不锁表
# --set-gtid-purged=OFF 避免目标库未开 GTID 时报错
# --skip-add-locks      导入时不必加表锁，提速
"$MYSQL_BIN" \
  -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  --default-character-set=utf8mb4 \
  --databases "$MYSQL_DB" \
  --single-transaction \
  --set-gtid-purged=OFF \
  --no-tablespaces \
  --skip-add-locks \
  --column-statistics=0 \
  --routines --triggers --events \
  > "$OUT" 2>/dev/null

SIZE=$(du -h "$OUT" | cut -f1)
TABLES=$(grep -c '^CREATE TABLE' "$OUT" || true)

echo "[ 完成 ] 已生成 ${OUT}"
echo "         体积：${SIZE}    表数量：${TABLES}"
echo
echo "         确认无误后，用 deploy/make-package.sh 打包上传。"
