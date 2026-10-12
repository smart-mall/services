# gl-mock-pay —— 模拟第三方支付平台（收银台静态页 + nginx 转发回调）
#
# 构建（必须在仓库根目录执行，构建上下文 = 仓库根）：
#   docker build -f mock-pay/Dockerfile -t gl-mock-pay:latest .
#
# 没有构建阶段：整个镜像就是一份静态页加一段 nginx 配置，不产出 jar，也不依赖 JVM。
# 它模拟的是"外部平台"，所以刻意不注册 Nacos、不连数据库、不参与服务网格。

FROM nginx:alpine

# default.conf 是基础镜像自带的默认站点配置，直接覆盖掉
COPY mock-pay/html /usr/share/nginx/html
COPY mock-pay/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
