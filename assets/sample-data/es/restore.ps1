# 重建 product 索引并导入 141 条商品文档。
# 前提：
#   1. gl-es 容器已运行（默认宿主机端口 59200）；
#   2. ES 已装 IK 分词插件 —— mapping 里 skuTitle 用 ik_max_word / ik_smart，
#      插件不在时下面的建索引会直接报 analyzer [ik_max_word] not found。
param(
    [string]$Es = 'http://localhost:59200'
)
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host '1/3 删除旧索引（不存在会返回 404，忽略）'
curl.exe -s -X DELETE "$Es/product" | Out-Null

Write-Host '2/3 按 product.index.json 建索引'
$created = curl.exe -s -X PUT "$Es/product" -H "Content-Type: application/json" --data-binary "@$here\product.index.json"
Write-Host $created

Write-Host '3/3 批量导入 product.ndjson'
$bulk = curl.exe -s -X POST "$Es/product/_bulk?refresh=true" -H "Content-Type: application/x-ndjson" --data-binary "@$here\product.ndjson"
Write-Host $bulk

Write-Host '--- 校验 ---'
curl.exe -s "$Es/product/_count"
Write-Host ''
