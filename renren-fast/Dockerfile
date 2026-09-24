# gl-renren-fast —— 后台管理服务（renren-fast 脚手架：Spring Boot 2.6.6 / Java 8）
# 与其它服务不同：不依赖 common，且技术栈是 Java 8，因此单独用 JDK 8 基础镜像。
# 构建（必须在仓库根目录执行，构建上下文 = 仓库根）：
#   docker build -f renren-fast/Dockerfile -t gl-renren-fast:latest .

# ── 构建阶段 ──
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /build

# 依赖仓库挂在 BuildKit 缓存上（id=maven-repo，所有镜像共用）。
# 注意：ojdbc6 / sqljdbc4 不在 central 上，依赖 .mvn/settings.xml 里的 aliyun-spring 仓库
COPY .mvn/settings.xml settings.xml
COPY renren-fast/pom.xml renren-fast/pom.xml
COPY renren-fast/src renren-fast/src
RUN --mount=type=cache,id=maven-repo,target=/root/.m2 \
    mvn -B -s settings.xml -f renren-fast/pom.xml clean package -Dmaven.test.skip=true

# ── 运行阶段 ──
FROM eclipse-temurin:8-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/renren-fast/target/renren-fast.jar /app/app.jar
EXPOSE 58880
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
