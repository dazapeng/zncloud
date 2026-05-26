# ============================================================
# ZN Cloud Java 通用多阶段构建 Dockerfile
# 构建上下文：项目根目录 (zncloud/)
# 使用方式（docker-compose）：
#   docker compose build user-service
# 或单服务构建（需指定 JAR_FILE）：
#   docker build -f docker/Dockerfile.java \
#     --build-arg JAR_FILE=zncloud-gateway/target/*.jar \
#     -t zncloud-gateway .
# ============================================================

# ---- 第一阶段：编译 ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制父 POM
COPY pom.xml ./

# 复制所有模块 POM（用于依赖解析缓存）
COPY zncloud-common/pom.xml ./zncloud-common/
COPY zncloud-gateway/pom.xml ./zncloud-gateway/
COPY zncloud-user-service/pom.xml ./zncloud-user-service/
COPY zncloud-device-service/pom.xml ./zncloud-device-service/
COPY zncloud-session-service/pom.xml ./zncloud-session-service/
COPY zncloud-schedule-service/pom.xml ./zncloud-schedule-service/
COPY zncloud-billing-service/pom.xml ./zncloud-billing-service/

# 下载依赖（利用 Docker 层缓存）
RUN mvn dependency:go-offline -B || true

# 复制所有模块源码
COPY zncloud-common/src ./zncloud-common/src
COPY zncloud-gateway/src ./zncloud-gateway/src
COPY zncloud-user-service/src ./zncloud-user-service/src
COPY zncloud-device-service/src ./zncloud-device-service/src
COPY zncloud-session-service/src ./zncloud-session-service/src
COPY zncloud-schedule-service/src ./zncloud-schedule-service/src
COPY zncloud-billing-service/src ./zncloud-billing-service/src

# 编译打包（跳过测试）
RUN mvn clean package -DskipTests -B

# ---- 第二阶段：运行 ----
FROM eclipse-temurin:17-jre

WORKDIR /app

# 指定具体服务的 JAR 路径（相对于 /build/）
# 由 docker-compose 传入，例如 zncloud-gateway/target/*.jar
ARG JAR_FILE=target/*.jar
ARG SERVICE_NAME=app

# 复制构建产物
COPY --from=builder /build/${JAR_FILE} app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 健康检查
EXPOSE 8080

# JVM 优化
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/heapdump.hprof"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
