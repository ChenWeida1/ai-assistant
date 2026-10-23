# 购物助手

Spring Boot 3 + MyBatis-Plus + LangChain4j 的 AI 助手。模型通过智谱 OpenAI 兼容接口调用 `glm-4.6v`。

- **Tool Calling**：订单问题调用 Java 方法查 MySQL；天气问题调用 Open-Meteo。
- **RAG**：问题先做向量检索，退换货、运费、会员政策只依据知识库片段回答。
- 前端为 Vue 3。

功能、表结构和验收标准见 [需求文档.md](需求文档.md)。表为 `shop_order`、`order_item`、`kb_document`、`kb_chunk`、`chat_record`。

## 准备

1. 用 Docker 启动 MySQL 8（需本机已安装 Docker Desktop）。首次启动会自动执行 `sql/schema.sql`（库名 `ai_assistant`，示例订单已写入）：

```powershell
docker compose up -d
```

查看状态：`docker compose ps`。停止：`docker compose down`。清空数据卷后重建：`docker compose down -v` 再 `docker compose up -d`。

若不使用 Docker，也可本机 MySQL 8 手动执行 `sql/schema.sql`。

2. 数据库账号默认与 `application.yml` 一致：`root` / `root`，宿主机端口 `3308`（避开本机 3306 及其他容器常用的 3307）；后端端口 `8082`。
3. 设置环境变量 `AI_API_KEY`。未设置时配置项为空，对话和知识库入库会提示未配置。嵌入模型为智谱 `embedding-3`。

```powershell
$env:AI_API_KEY="你的智谱 Key"
mvn spring-boot:run
```

另开终端启动前端：

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173 。

## 可以这样问

- 查询订单 `ORD20260925001`
- 张三有哪些订单
- 哪些订单已发货
- 北京今天天气怎么样
- 未满 99 元怎么收运费
