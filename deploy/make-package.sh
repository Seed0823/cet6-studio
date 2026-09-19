#!/usr/bin/env bash
# ============================================================
# 打包部署包（在你自己的电脑上执行）
# ------------------------------------------------------------
# 把「服务器上真正需要的东西」打成一个 tar.gz：
#   源码 + 构建产物 + 部署配置 + 数据库种子
# 并排除 node_modules、.git 等只对开发有意义的内容。
#
# 用法：
#   bash deploy/make-package.sh
# 产物：
#   项目根目录下的 cet6-deploy.tar.gz
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT="${PROJECT_DIR}/cet6-deploy.tar.gz"

cd "$PROJECT_DIR"

echo "构建产物自检……"
[ -f "backend/target/cet6-sprint-backend-1.0.0.jar" ] \
  || { echo "[ 失败 ] 缺少后端 jar，请先执行构建（见部署指南第 5 步）"; exit 1; }
[ -f "frontend/dist/index.html" ] \
  || { echo "[ 失败 ] 缺少前端产物，请先执行 npm run build"; exit 1; }
ls deploy/seed/*.sql >/dev/null 2>&1 \
  || { echo "[ 失败 ] 缺少 deploy/seed/*.sql，请先执行 bash deploy/make-seed.sh"; exit 1; }
echo "[ 完成 ] 三项产物齐备"

rm -f "$OUT"

echo "开始打包……"
tar -czf "$OUT" \
  --exclude='./frontend/node_modules' \
  --exclude='./frontend/.vite' \
  --exclude='./.git' \
  --exclude='./.idea' \
  --exclude='./.vscode' \
  --exclude='*.log' \
  ./backend ./frontend ./deploy ./sql \
  ./.gitignore ./.dockerignore

SIZE=$(du -h "$OUT" | cut -f1)

echo "[ 完成 ] 已生成 cet6-deploy.tar.gz（${SIZE}）"
echo
echo "接下来上传到服务器："
echo "  scp cet6-deploy.tar.gz root@<你的公网IP>:/root/"
echo
echo "（若用阿里云 WorkBench 网页终端，直接用界面上的「文件上传」按钮传这个文件即可）"
