# 样本数据

一套可以直接导入的谷粒商城样本数据：**MySQL 全库 + MinIO 商品图片 + Elasticsearch 商品索引**。
别人把项目跑起来之后，按下面的顺序执行三个脚本，就能得到和原作者环境一样的商品、订单、图片和搜索结果，不用自己一条条录数据。

## 目录

```
sample-data/
├── mysql/
│   ├── gulimall.sql        8 个库的完整 dump（44MB）
│   └── restore.ps1         导入脚本
├── minio/
│   ├── objects/            269 个图片文件，路径就是对象在 MinIO 里的 key
│   ├── manifest.json       本地文件 ↔ bucket/key 的映射，含 size/etag/contentType
│   ├── objects.jsonl       mc ls 的原始清单（导出时留档）
│   └── restore.ps1         按 key 原样写回 gulimall 桶
└── es/
    ├── product.index.json  建索引用的 settings + mapping
    ├── product.ndjson      141 条商品的 bulk 文件
    ├── product.mapping.json / product.settings.json   原始接口返回，留档
    └── restore.ps1         重建索引并批量导入
```

## 里面是什么

| 部分 | 内容 |
| --- | --- |
| MySQL | `gulimall_admin`（后台菜单/用户）、`gulimall_oms` 订单、`gulimall_pms` 商品、`gulimall_sms` 优惠、`gulimall_ums` 会员、`gulimall_wms` 仓储、`third_party` 第三方、`nacos` 配置中心 |
| MinIO | 桶 `gulimall`，269 个对象共 191.6MB，key 形如 `2026/10/18/<uuid>.jpg` |
| Elasticsearch | 索引 `product`，141 条商品文档（含 `attrs` 嵌套子文档，Lucene 记为 435） |

`nacos` 库也一起导了 —— 服务启动时要靠它拿配置和注册，缺了服务根本起不来。

## 导入顺序

前提：`gl-mysql`、`gl-minio`、`gl-es` 三个容器已经起来（`docker compose -f gl-com.yml up -d gl-mysql gl-minio gl-es`）。

```powershell
cd assets\sample-data

.\mysql\restore.ps1     # 1. 全库数据
.\minio\restore.ps1     # 2. 图片对象
.\es\restore.ps1        # 3. 搜索索引
```

三步互不依赖，但顺序有讲究：商品图和搜索索引里的数据都来自 MySQL，先有库再有图、再有索引，对不上号时好排查。

### 关于 Elasticsearch

`product` 的 mapping 里 `skuTitle` 用了 **IK 分词器**（`ik_max_word` / `ik_smart`）。目标 ES 没装 IK 插件时，建索引这一步会直接报 `analyzer [ik_max_word] not found`。

装法（ES 8.11 + IK 8.11.0）：

```bash
docker exec -it gl-es bin/elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/8.11.0
docker restart gl-es
```

不想装插件、只要数据的话：把 `product.index.json` 里 `skuTitle` 的 `analyzer` 和 `search_analyzer` 两行删掉再导入，索引能建起来，但中文搜索会退化成单字切分。

## 校验

| 部分 | 命令 | 期望 |
| --- | --- | --- |
| MySQL | `docker exec gl-mysql mysql -uroot -proot -N -e "select count(*) from gulimall_pms.pms_sku_info"` | 按你的样本量，非 0 |
| MinIO | `mc ls --recursive m/gulimall \| wc -l` | `269` |
| ES | `curl http://localhost:59200/product/_count` | `{"count":141,...}` |

## 两条不能改的东西

1. **MinIO 的 key 不能改**。数据库里 `sku_img`、`brand_img`、`spu_description` 的图片地址是 `http://localhost:59000/gulimall/<key>`，改了 key 页面上的图全部 404。
2. **MySQL 的库名和表结构不能改**。各服务启动时按库名连，`nacos` 库里还存着全部配置；结构变了 Flyway/手动建表都对不上。
