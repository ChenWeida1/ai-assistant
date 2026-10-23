CREATE DATABASE IF NOT EXISTS ai_assistant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_assistant;

DROP TABLE IF EXISTS chat_record;
DROP TABLE IF EXISTS kb_chunk;
DROP TABLE IF EXISTS kb_document;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS shop_order;

CREATE TABLE shop_order (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no      VARCHAR(32)    NOT NULL COMMENT '订单号',
    user_name     VARCHAR(50)    NOT NULL COMMENT '下单客户',
    phone         VARCHAR(20)    NOT NULL COMMENT '手机号',
    status        VARCHAR(20)    NOT NULL COMMENT 'PENDING_PAYMENT/PAID/SHIPPED/COMPLETED/CANCELLED',
    receiver      VARCHAR(50)    NOT NULL COMMENT '收货人',
    address       VARCHAR(200)   NOT NULL COMMENT '收货地址',
    total_amount  DECIMAL(12, 2) NOT NULL COMMENT '订单金额',
    remark        VARCHAR(200)            DEFAULT NULL COMMENT '备注',
    created_at    DATETIME       NOT NULL COMMENT '下单时间',
    pay_time      DATETIME                DEFAULT NULL COMMENT '支付时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_name (user_name),
    KEY idx_phone (phone),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

CREATE TABLE order_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT         NOT NULL COMMENT '所属订单',
    product_name  VARCHAR(100)   NOT NULL COMMENT '商品名称',
    quantity      INT            NOT NULL COMMENT '数量',
    price         DECIMAL(12, 2) NOT NULL COMMENT '单价',
    amount        DECIMAL(12, 2) NOT NULL COMMENT '小计',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品';

CREATE TABLE kb_document (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(255) NOT NULL COMMENT '文档标题',
    chunk_count  INT          NOT NULL DEFAULT 0 COMMENT '分片数量',
    status       VARCHAR(20)  NOT NULL COMMENT 'READY/FAILED',
    created_at   DATETIME     NOT NULL COMMENT '入库时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档';

CREATE TABLE kb_chunk (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    document_id  BIGINT      NOT NULL COMMENT '所属文档',
    chunk_index  INT         NOT NULL COMMENT '分片序号，从 0 开始',
    content      MEDIUMTEXT  NOT NULL COMMENT '分片原文',
    embedding_id VARCHAR(64) NOT NULL COMMENT '向量库条目 ID',
    embedding    MEDIUMTEXT  NOT NULL COMMENT '向量 JSON，重启后重建索引',
    created_at   DATETIME    NOT NULL COMMENT '写入时间',
    PRIMARY KEY (id),
    KEY idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分片与向量';

CREATE TABLE chat_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id   VARCHAR(64)  NOT NULL COMMENT '会话 ID',
    role         VARCHAR(20)  NOT NULL COMMENT 'user/assistant',
    content      MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    tools_json   VARCHAR(500)          DEFAULT NULL COMMENT '本次调用的工具',
    sources_json MEDIUMTEXT            DEFAULT NULL COMMENT 'RAG 引用片段',
    created_at   DATETIME     NOT NULL COMMENT '写入时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话记录';

INSERT INTO shop_order (order_no, user_name, phone, status, receiver, address, total_amount, remark, created_at, pay_time) VALUES
('ORD20260925001', '张三', '13800001111', 'PAID', '张三', '北京市朝阳区建国路 88 号', 299.00, '请工作日配送', '2026-09-25 09:12:00', '2026-09-25 09:15:00'),
('ORD20260925002', '李四', '13900002222', 'SHIPPED', '李四', '上海市浦东新区世纪大道 100 号', 158.50, NULL, '2026-09-24 14:20:00', '2026-09-24 14:22:00'),
('ORD20260918003', '张三', '13800001111', 'COMPLETED', '张三', '北京市海淀区中关村大街 1 号', 89.00, NULL, '2026-09-18 11:03:00', '2026-09-18 11:05:00'),
('ORD20260920004', '王五', '13700003333', 'PENDING_PAYMENT', '王五', '广州市天河区体育西路 66 号', 520.00, '尽快发货', '2026-09-20 16:40:00', NULL),
('ORD20260922005', '李四', '13900002222', 'CANCELLED', '李四', '上海市浦东新区世纪大道 100 号', 45.00, '买错规格', '2026-09-22 08:18:00', NULL);

INSERT INTO order_item (order_id, product_name, quantity, price, amount) VALUES
(1, '机械键盘', 1, 299.00, 299.00),
(2, '无线鼠标', 1, 129.00, 129.00),
(2, '鼠标垫', 1, 29.50, 29.50),
(3, 'USB 扩展坞', 1, 89.00, 89.00),
(4, '显示器支架', 1, 520.00, 520.00),
(5, '手机支架', 1, 45.00, 45.00);
