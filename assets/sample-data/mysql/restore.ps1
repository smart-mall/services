# 把本目录的 gulimall.sql 导入 gl-mysql。
# 前提：gl-mysql 容器已运行（docker compose -f gl-com.yml up -d gl-mysql）。
# 用 docker cp 传文件而不是管道：Windows 的 PowerShell 管道会把中文按本地代码页转码，导入后商品名会变乱码。
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

docker cp "$here\gulimall.sql" gl-mysql:/tmp/gulimall.sql
docker exec gl-mysql sh -c "mysql -uroot -proot < /tmp/gulimall.sql"
docker exec gl-mysql sh -c "rm -f /tmp/gulimall.sql"

Write-Host '--- 导入结果 ---'
docker exec gl-mysql mysql -uroot -proot -e "select table_schema, count(*) as tables from information_schema.tables where table_schema not in ('mysql','sys','performance_schema','information_schema') group by table_schema;"
