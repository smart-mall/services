# 把 objects/gulimall 下的 269 个文件按原 key 写回 MinIO 的 gulimall 桶。
# 前提：gl-minio 容器已运行，且本机有 minio/mc 镜像（docker pull minio/mc）。
# key 必须原样保留：数据库里 sku_img、brand_img 等字段存的就是 /gulimall/<key>，改了 key 图就 404。
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

docker rm -f mc-restore 2>$null | Out-Null
# 目标路径必须是容器里已经存在的 /tmp：docker cp 到不存在的目录会把 gulimall 的内容摊平，源路径就错了。
docker create --name mc-restore --network GrainMall-network --entrypoint sh minio/mc:latest -c "mc alias set m http://gl-minio:9000 minioadmin minioadmin >/dev/null 2>&1; mc mb --ignore-existing m/gulimall; mc mirror --overwrite /tmp/gulimall m/gulimall; echo '--- 桶内对象数 ---'; mc ls --recursive m/gulimall | wc -l" | Out-Null
docker cp "$here\objects\gulimall" mc-restore:/tmp/
docker start -a mc-restore
docker rm -f mc-restore | Out-Null
