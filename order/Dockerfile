# gl-order —— 订单服务（下单、支付、关单、MQ 最终一致性）
# 构建（必须在仓库根目录执行，构建上下文 = 仓库根）：
#   docker build -f order/Dockerfile -t gl-order:latest .

# ── 构建阶段 ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 依赖仓库挂在 BuildKit 缓存上（id=maven-repo，所有镜像共用）。
# 它不在层里，所以 common 改动导致层失效时依赖不会重新下载。
COPY .mvn/settings.xml settings.xml

# 1) 公共模块 common
COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN --mount=type=cache,id=maven-repo,target=/root/.m2 \
    mvn -B -s settings.xml -f common/pom.xml clean install -Dmaven.test.skip=true

# 2) 本模块
COPY order/pom.xml order/pom.xml
COPY order/src order/src
RUN --mount=type=cache,id=maven-repo,target=/root/.m2 \
    mvn -B -s settings.xml -f order/pom.xml clean package -Dmaven.test.skip=true

# ── 运行阶段 ──
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/order/target/order-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 52300
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
