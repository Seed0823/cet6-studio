#!/usr/bin/env bash
# ============================================================
# CET6 Studio 一键部署脚本
# ------------------------------------------------------------
# 用法（在项目根目录执行）：
#   bash deploy/deploy.sh
#
# 脚本会依次完成：环境自检 → 产物自检 → 配置自检 → 启动 → 等待就绪 → 验证
# 任一步不满足都会明确告知缺什么，不会带着问题硬启动。
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

info() { printf '\033[32m[ 完成 ]\033[0m %s\n' "$1"; }
warn() { printf '\033[33m[ 注意 ]\033[0m %s\n' "$1"; }
fail() { printf '\033[31m[ 失败 ]\033[0m %s\n' "$1"; exit 1; }

echo
echo "==============================================="
echo "  CET6 Studio 部署"
echo "==============================================="
echo

# ------------------------------------------------------------
# 1. 运行环境
# ------------------------------------------------------------
command -v docker >/dev/null 2>&1 || fail "未检测到 docker，请先安装 Docker（见部署指南第 3 步）"
docker info >/dev/null 2>&1 || fail "Docker 未在运行，请执行 sudo systemctl start docker"

if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  fail "未检测到 compose 插件，请安装 docker-compose-plugin（见部署指南第 3 步）"
fi
info "Docker 环境正常（$DC）"

# ------------------------------------------------------------
# 2. 构建产物
# ------------------------------------------------------------
JAR="${PROJECT_DIR}/backend/target/cet6-sprint-backend-1.0.0.jar"
DIST="${PROJECT_DIR}/frontend/dist/index.html"

[ -f "$JAR" ]  || fail "缺少后端 jar：backend/target/cet6-sprint-backend-1.0.0.jar
        请在本地执行构建后重新上传（见部署指南第 5 步）"
[ -f "$DIST" ] || fail "缺少前端产物：frontend/dist/index.html
        请在本地执行 npm run build 后重新上传（见部署指南第 5 步）"
info "前后端构建产物已就位"

# ------------------------------------------------------------
# 3. 数据库种子
# ------------------------------------------------------------
SEED_COUNT=$(find "${SCRIPT_DIR}/seed" -maxdepth 1 -name '*.sql' 2>/dev/null | wc -l)
[ "$SEED_COUNT" -gt 0 ] || fail "缺少数据库导出文件 deploy/seed/*.sql
        请在本地执行 deploy/make-seed.sh 生成后重新上传"
info "数据库种子已就位（${SEED_COUNT} 个文件）"

# ------------------------------------------------------------
# 4. 配置文件
# ------------------------------------------------------------
cd "$SCRIPT_DIR"

if [ ! -f .env ]; then
  cp .env.example .env
  echo
  warn "已为你生成 deploy/.env，请先填写两个必填项："
  echo "        DB_PASSWORD  数据库密码"
  echo "        JWT_SECRET   JWT 密钥（用 openssl rand -base64 48 生成）"
  echo
  echo "      编辑命令：  vi ${SCRIPT_DIR}/.env"
  echo "      改完后重新执行：bash deploy/deploy.sh"
  echo
  exit 0
fi

if grep -q "请改成你自己的强密码" .env; then
  fail ".env 里的 DB_PASSWORD 还是占位符，请改成真实密码"
fi
if grep -q "请粘贴一条随机字符串" .env; then
  fail ".env 里的 JWT_SECRET 还是占位符，请填入随机密钥
        生成方法：openssl rand -base64 48"
fi

# HS256 要求密钥至少 256 bit；过短会在后端启动时抛异常
SECRET_LEN=$(grep -E '^JWT_SECRET=' .env | head -1 | cut -d= -f2- | tr -d '\r\n' | wc -c)
if [ "$SECRET_LEN" -lt 33 ]; then
  fail ".env 里的 JWT_SECRET 只有 $((SECRET_LEN - 1)) 字节，至少需要 32 字节"
fi
info ".env 校验通过"

# ------------------------------------------------------------
# 5. 启动
# ------------------------------------------------------------
echo
echo "开始构建并启动容器……（首次需要拉取镜像，视网络情况约 2-5 分钟）"
echo
$DC up -d --build

# ------------------------------------------------------------
# 6. 等待就绪
# ------------------------------------------------------------
HTTP_PORT=$(grep -E '^HTTP_PORT=' .env | head -1 | cut -d= -f2- | tr -d '\r\n' || true)
HTTP_PORT="${HTTP_PORT:-80}"

printf '等待服务就绪'
READY=0
for _ in $(seq 1 60); do
  if curl -fs "http://127.0.0.1:${HTTP_PORT}/health" >/dev/null 2>&1; then
    READY=1
    break
  fi
  printf '.'
  sleep 3
done
echo

if [ "$READY" -eq 1 ]; then
  info "Web 服务已就绪"
else
  warn "Web 服务在 180 秒内未就绪，请查看日志：$DC logs --tail 50 nginx"
fi

# ------------------------------------------------------------
# 7. 接口验证
# ------------------------------------------------------------
printf '等待后端接口'
API_OK=0
for _ in $(seq 1 40); do
  if curl -fs "http://127.0.0.1:${HTTP_PORT}/api/ping" >/dev/null 2>&1; then
    API_OK=1
    break
  fi
  printf '.'
  sleep 3
done
echo

if [ "$API_OK" -eq 1 ]; then
  info "后端接口响应正常"
else
  warn "后端接口未响应。首次启动需导入 5.7 万条词库，可再等 1-2 分钟；"
  echo "       若持续失败请查看：$DC logs --tail 80 backend"
fi

# ------------------------------------------------------------
# 8. 汇总
# ------------------------------------------------------------
PUBLIC_IP=$(curl -fs --max-time 5 https://api.ipify.org 2>/dev/null || echo "<你的服务器公网IP>")
PORT_SUFFIX=""
[ "$HTTP_PORT" != "80" ] && PORT_SUFFIX=":${HTTP_PORT}"

echo
echo "==============================================="
echo "  部署完成"
echo "==============================================="
echo "  访问地址：http://${PUBLIC_IP}${PORT_SUFFIX}"
echo "  演示账号：cuijiabing / 123456"
if [ "$HTTP_PORT" = "80" ]; then
  echo "  若打不开：请确认阿里云安全组已放行 80 端口"
fi
echo
$DC ps
echo
echo "常用命令："
echo "  查看状态  $DC ps"
echo "  查看日志  $DC logs -f backend"
echo "  重启服务  $DC restart"
echo "  停止服务  $DC down"
echo
