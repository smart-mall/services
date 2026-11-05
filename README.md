# GrainMall · 后端

谷粒商城后端：Spring Cloud 微服务，一个网关（53000）+ 11 个业务服务 + 后台管理的服务端；业务服务不对外暴露端口，所有请求一律走网关。

## 启动

```bash
# 1. 起基础设施（MySQL / Redis / Nacos / ES / RabbitMQ / MinIO / Sentinel / Zipkin）
docker compose -f gl-com.yml up -d gl-mysql gl-redis gl-nacos gl-es gl-rabbitmq gl-minio gl-sentinel gl-zipkin

# 2. 首次启动导入库表（幂等，可重复执行）
docker exec -i gl-mysql mysql -uroot -proot --default-character-set=utf8mb4 < assets/sql/init.sql

# 3. 编译，然后在 IDE 里按需启动模块
mvn -DskipTests install
```

连接参数集中在 `common/src/main/resources/args.yaml`，默认写的是 compose 里的容器名；不在容器内跑时要改成本机地址（MySQL 53306、Redis 56379、Nacos 58848 …）。

## 构建镜像

```powershell
.\build-images.ps1                  # 按源码指纹增量构建，源码没变的跳过
.\build-images.ps1 -Force           # 全量重建
.\build-images.ps1 -Only gl-order   # 只构建指定镜像
```

每个服务一个 Dockerfile，构建上下文都是仓库根（需要先编译 `common`）。后台管理、用户端两个前端镜像在各自仓库里构建。

## 部署

```bash
docker compose -f gl-com.yml up -d
```

起来之后：网关 53000、后台管理 56731、用户端 56732、MinIO 控制台 59001、模拟支付 53001。数据目录按 `gl-com.yml` 里的 `/volume/GrainMall/...` 挂载，换机器需要改成对应路径。
