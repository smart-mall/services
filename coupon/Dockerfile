# gl-coupon —— 营销服务（优惠券、会员价、秒杀场次）
# 构建（必须在仓库根目录执行，构建上下文 = 仓库根）：
#   docker build -f coupon/Dockerfile -t gl-coupon:latest .

# ── 构建阶段 ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 1) 公共模块 common：这两步在所有服务的 Dockerfile 里完全一致，
#    Docker 层缓存可跨镜像复用，实际只有第一个镜像会真正下载依赖。
COPY .mvn/settings.xml settings.xml
COPY common/pom.xml common/pom.xml
RUN mvn -B -s settings.xml -f common/pom.xml dependency:go-offline

COPY common/src common/src
RUN mvn -B -s settings.xml -f common/pom.xml clean install -Dmaven.test.skip=true

# 2) 本模块依赖下载（此时 common 已在本地仓库），单独成层以便复用
COPY coupon/pom.xml coupon/pom.xml
RUN mvn -B -s settings.xml -f coupon/pom.xml dependency:go-offline

# 3) 编译打包（jar 已被 spring-boot-maven-plugin 重打包为可执行包）
COPY coupon/src coupon/src
RUN mvn -B -s settings.xml -f coupon/pom.xml clean package -Dmaven.test.skip=true

# ── 运行阶段 ──
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/coupon/target/coupon-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 52100
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
