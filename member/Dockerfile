# gl-member —— 会员服务（会员、收货地址、积分成长）
# 构建（必须在仓库根目录执行，构建上下文 = 仓库根）：
#   docker build -f member/Dockerfile -t gl-member:latest .

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
COPY member/pom.xml member/pom.xml
COPY member/src member/src
RUN --mount=type=cache,id=maven-repo,target=/root/.m2 \
    mvn -B -s settings.xml -f member/pom.xml clean package -Dmaven.test.skip=true

# ── 运行阶段 ──
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/member/target/member-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 52200
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
