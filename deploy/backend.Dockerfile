# ============================================================
# 后端镜像
# ------------------------------------------------------------
# 刻意不在这里跑 Maven 构建：
#   云服务器多为 2 核 2G，编译 Spring Boot 要下几百 MB 依赖 + 吃 1G 以上内存，
#   比在本地构建慢一个数量级。因此 jar 由本地构建好一起上传，镜像只负责「装进去并启动」。
#   本地构建命令见 deploy/部署指南.md。
# ============================================================
FROM eclipse-temurin:17-jre

ENV TZ=Asia/Shanghai \
    JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC"

WORKDIR /app

COPY backend/target/cet6-sprint-backend-1.0.0.jar /app/app.jar

EXPOSE 8080

# 用 exec 形式启动，让 java 成为 PID 1，容器停止时能收到信号正常退出
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
