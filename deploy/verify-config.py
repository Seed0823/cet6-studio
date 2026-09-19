#!/usr/bin/env python3
# ============================================================
# 部署配置自检
# ------------------------------------------------------------
# 部署前在本地跑一次，把「路径拼错、端口不一致、变量漏配」这类
# 只有到服务器上才会暴露的问题提前查出来。
#
# 用法（在项目根目录或任意位置）：
#   python deploy/verify-config.py
#
# 退出码 0 表示通过，1 表示存在必须修复的问题。
# 依赖：无。装了 pyyaml 会额外做一次完整的 YAML 语法解析。
# ============================================================
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEPLOY = os.path.join(ROOT, "deploy")

fails, warns, oks = [], [], []


def rel(p):
    return os.path.relpath(p, ROOT).replace("\\", "/")


def exists(p):
    return os.path.exists(os.path.join(ROOT, p))


# ------------------------------------------------------------
# 1. 关键文件是否齐备
# ------------------------------------------------------------
REQUIRED = [
    ".gitignore",
    ".dockerignore",
    "deploy/docker-compose.yml",
    "deploy/.env.example",
    "deploy/backend.Dockerfile",
    "deploy/nginx/default.conf",
    "deploy/deploy.sh",
    "deploy/make-seed.sh",
    "deploy/make-package.sh",
    "backend/target/cet6-sprint-backend-1.0.0.jar",
    "frontend/dist/index.html",
]
for p in REQUIRED:
    (oks if exists(p) else fails).append(("文件存在 " if exists(p) else "文件缺失 ") + p)

if not os.path.exists(os.path.join(DEPLOY, "docker-compose.yml")):
    print("缺少 docker-compose.yml，无法继续校验")
    sys.exit(1)

compose = open(os.path.join(DEPLOY, "docker-compose.yml"), encoding="utf-8").read()


# ------------------------------------------------------------
# 2. compose 中的挂载路径是否真实存在（相对 deploy/ 解析）
# ------------------------------------------------------------
for host, cont in re.findall(r"-\s+(\.\.?/[^\s:]+):([^\s:]+)", compose):
    real = os.path.normpath(os.path.join(DEPLOY, host))
    if os.path.exists(real):
        oks.append("挂载源存在 %-32s -> %s" % (host, cont))
    else:
        fails.append("挂载源不存在 %s（解析为 %s）" % (host, real))


# ------------------------------------------------------------
# 3. Dockerfile 里 COPY 的源文件
# ------------------------------------------------------------
df_path = os.path.join(DEPLOY, "backend.Dockerfile")
df = open(df_path, encoding="utf-8").read() if os.path.exists(df_path) else ""
for src in re.findall(r"^COPY\s+(\S+)", df, re.M):
    if os.path.exists(os.path.join(ROOT, src)):
        oks.append("Dockerfile COPY 源存在 " + src)
    else:
        fails.append("Dockerfile COPY 源不存在 " + src)


# ------------------------------------------------------------
# 4. .env.example 是否覆盖 compose 引用的全部变量
# ------------------------------------------------------------
env_path = os.path.join(DEPLOY, ".env.example")
env = open(env_path, encoding="utf-8").read() if os.path.exists(env_path) else ""
env_keys = set(re.findall(r"^([A-Z_]+)=", env, re.M))
used = set(re.findall(r"\$\{([A-Z_]+)(?::[^}]*)?\}", compose))
missing = used - env_keys
if missing:
    fails.append(".env.example 缺少 compose 引用的变量：" + ", ".join(sorted(missing)))
else:
    oks.append("配置变量完整：" + ", ".join(sorted(used)))


# ------------------------------------------------------------
# 5. jar 路径在两个脚本里必须一致
# ------------------------------------------------------------
sh_path = os.path.join(DEPLOY, "deploy.sh")
sh = open(sh_path, encoding="utf-8").read() if os.path.exists(sh_path) else ""
m_sh = re.search(r'JAR="\$\{PROJECT_DIR\}/([^"]+)"', sh)
m_df = re.search(r"COPY\s+(\S+\.jar)", df)
if m_sh and m_df:
    if m_sh.group(1) == m_df.group(1):
        oks.append("jar 路径一致：" + m_df.group(1))
    else:
        fails.append("jar 路径不一致：deploy.sh=%s  Dockerfile=%s" % (m_sh.group(1), m_df.group(1)))
else:
    fails.append("无法从脚本中提取 jar 路径做比对")


# ------------------------------------------------------------
# 6. 端口暴露面：只有 nginx 可以对宿主机开放
# ------------------------------------------------------------
for svc in ("mysql", "backend"):
    block = re.search(r"^  %s:(.*?)(?=^  \w+:|\Z)" % svc, compose, re.M | re.S)
    if block and re.search(r"^\s+ports:", block.group(1), re.M):
        fails.append("%s 服务对宿主机暴露了端口，应只在内部网络可达" % svc)
    else:
        oks.append("%s 未对宿主机暴露端口" % svc)


# ------------------------------------------------------------
# 7. Nginx 反代目标
# ------------------------------------------------------------
ng_path = os.path.join(DEPLOY, "nginx", "default.conf")
ng = open(ng_path, encoding="utf-8").read() if os.path.exists(ng_path) else ""
m = re.search(r"proxy_pass\s+http://([\w\-]+):(\d+)", ng)
if m:
    host, port = m.group(1), m.group(2)
    services = re.findall(r"^  (\w+):", compose, re.M)
    if host not in services:
        fails.append("Nginx 反代目标 %s 不是 compose 服务名，容器内无法解析（服务名：%s）"
                     % (host, ", ".join(services)))
    elif port != "8080":
        fails.append("Nginx 反代端口 %s 与后端默认端口 8080 不一致" % port)
    else:
        oks.append("Nginx 反代到 %s:%s，目标可被容器网络解析" % (host, port))
else:
    fails.append("Nginx 配置里找不到 proxy_pass")

if re.search(r"gzip\s+on\s*;", ng):
    oks.append("Nginx 已开启 gzip")
else:
    warns.append("Nginx 未开 gzip，1M 小带宽下首屏会明显变慢")


# ------------------------------------------------------------
# 8. 容器启动方式与安全项
# ------------------------------------------------------------
if 'ENTRYPOINT ["java"' in df:
    oks.append("后端以 exec 形式启动，容器可正常接收停止信号")
else:
    warns.append("后端 ENTRYPOINT 非 exec 数组形式，停止时可能收不到信号")

if "privileged: true" in compose:
    fails.append("compose 使用了 privileged 高权限模式，属不必要的风险")
else:
    oks.append("未使用 privileged 高权限模式")


# ------------------------------------------------------------
# 9. 数据库种子
# ------------------------------------------------------------
seed_dir = os.path.join(DEPLOY, "seed")
seeds = [f for f in os.listdir(seed_dir) if f.endswith(".sql")] if os.path.isdir(seed_dir) else []
if not seeds:
    fails.append("deploy/seed 下没有 sql 文件，容器首次启动会得到一个空库")
else:
    for name in seeds:
        sp = os.path.join(seed_dir, name)
        size_mb = os.path.getsize(sp) / 1024 / 1024
        head = open(sp, encoding="utf-8", errors="replace").read(4000)
        oks.append("种子 %s（%.1f MB）" % (name, size_mb))
        if "CREATE DATABASE" in head and "USE `" in head:
            oks.append("种子自带 CREATE DATABASE + USE，导入自洽")
        else:
            warns.append("种子缺少 CREATE DATABASE/USE，需依赖 MYSQL_DATABASE 定位")


# ------------------------------------------------------------
# 10. 可选的 YAML 语法解析
# ------------------------------------------------------------
try:
    import yaml  # noqa
    try:
        parsed = yaml.safe_load(compose)
        svcs = list(parsed.get("services", {}).keys())
        oks.append("docker-compose.yml YAML 语法解析通过（服务：%s）" % ", ".join(svcs))
    except Exception as exc:
        fails.append("docker-compose.yml YAML 语法有误：%s" % exc)
except ImportError:
    warns.append("未安装 pyyaml，已跳过 YAML 语法解析（pip install pyyaml 可开启）")


# ------------------------------------------------------------
# 输出
# ------------------------------------------------------------
line = "=" * 64
print(line)
print("通过 %d 项" % len(oks))
for m in oks:
    print("  [ ok ]  " + m)
if warns:
    print("-" * 64)
    print("提醒 %d 项（不阻断部署）" % len(warns))
    for m in warns:
        print("  [warn]  " + m)
if fails:
    print("-" * 64)
    print("失败 %d 项（需修复后才能部署）" % len(fails))
    for m in fails:
        print("  [FAIL]  " + m)
print(line)
print("结论：" + ("通过" if not fails else "存在问题"))
sys.exit(1 if fails else 0)
