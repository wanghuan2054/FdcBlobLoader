# FdcBlobLoader

## 1. 项目简介 (Project Introduction)
本项目应用于半导体显示行业（Array 工艺）。主要用于解析生产设备上传并压缩存储的二进制设备状态参数数据（Blob 数据）。
通过提取设备产生的检测数据（Trace Data），程序将 Blob 字段进行 GZip 解压缩、解析清洗，并按 Step（工序）和时间（班次）对各项设备的参数进行聚合计算（如：最大值、最小值、平均值），最终将结构化的指标数据转存至数据仓库中，供后续 FDC（Fault Detection and Classification，故障检测与分类）系统进行监控和分析。

---

## 2. 表结构说明 (Table Structures)

### 2.1 数据读取表 (Read Source)
程序主要从设备追踪日志表 **`EQP_TRACE_TRX_FDC`** 中读取压缩数据，主要涉及的字段包括：
*   `EQP_MODULE_ID` / `UNIT_ID`: 设备/模组ID
*   `SUBSTRATE_ID`: 玻璃基板ID
*   `EQP_DCP_ID`: 数据采集计划ID
*   `FILE_DATA`: 核心二进制 Blob 数据字段（存放 GZip 压缩的设备 Trace Data 文件流）
*   `RSD_05`: 生产类型 (Production Type)
*   `OPERATION_ID`: 站点/操作ID

### 2.2 数据写入表 (Write Target)
清洗并经过聚合计算后的数据会被批量写入到数据仓库（MDW）的 **`EDS_FDC_TRACE`** 表中，其表结构及字段映射如下：
*   `START_TIMEKEY` (开始时间)
*   `END_TIMEKEY` (结束时间)
*   `SHIFT_TIMEKEY` (班次时间，转换为标准的A/B班标记，如06:00或18:00)
*   `LOT_ID` (批次号)
*   `GLASS_ID` (玻璃基板ID)
*   `SLOT` (插槽号)
*   `RECIPE` (配方/Recipe)
*   `PRODUCT_ID` (产品ID)
*   `STEP_ID` (工序ID)
*   `STEP_NAME` (工序名称)
*   `UNIT_ID` (设备/模组ID)
*   `PTODUCTIONTYPE` (生产类型)
*   `OPER_ID` (站点操作ID)
*   `ITEM` (具体的设备参数名称)
*   `VALUE_MIN` (该参数在当前Step和时间段内的最小值)
*   `VALUE_MAX` (该参数在当前Step和时间段内的最大值)
*   `VALUE_AVG` (该参数在当前Step和时间段内的平均值)

---

## 3. 技术架构与代码架构

### 3.1 技术架构
*   **开发语言**: Java 8
*   **构建工具**: Maven (包含 `maven-assembly-plugin` 打包机制)
*   **数据库连接**: JDBC (Oracle `ojdbc6`)
*   **连接池技术**: 支持且集成了多种连接池，如 **HikariCP** (默认使用)、**Druid**、**Commons-DBCP**，保障海量数据吞吐时的连接稳定性。
*   **日志管理**: Log4j / SLF4J

### 3.2 代码架构
项目采用分层架构，核心包及功能如下：
*   **`blobdata/`**: 业务核心逻辑包。
    *   `MainProgram.java`: 程序主入口，负责定时扫描/循环拉取数据，并调用解析逻辑。
    *   `FDCTraceParserBlob.java`: 核心解析类，实现从文本中构建虚拟内存表（Table），并按 Step 和 Time 分组计算各项指标的聚合值。
    *   `Table.java` & `AggregateFunction.java`: 抽象的数据内存表及聚合指标实体类。
*   **`Utils/`**: 工具类包。
    *   `DBUtil.java`: 数据库操作核心类，实现从数据源调用存储过程拉取 Blob 游标，并将计算好的结果利用 `PreparedStatement.executeBatch()` 批量入库。
    *   `FileUtils.java`: 实现 Blob 数据的 GZip 解压，并逐行读取文本，过滤非法字符、剥离无用 `@` 符号，完成数据的清洗转换。
*   **`datapool/`**: 数据库连接池配置包。封装了 Druid、DBCP 和 HikariCP 的工具类，支持灵活切换。
*   **`config/`**: 配置读取包。用于读取数据库及连接池的 `.properties` 文件，以及处理 Log 输出。

### 3.3 核心处理流程
1.  **数据抓取**: 挂起线程按时间切片（循环获取前 120 分钟的数据），通过调用 Oracle 存储过程 `PKG_FDC_TRACE_TRX.GET_DRYPUMPDATA` 抓取 ResultSet 数据集。
2.  **解压转换**: 将返回的 `FILE_DATA` Blob 字段数据，使用 GZip 算法解压到本地临时文件 `resultTemp.txt`。
3.  **清洗解析**: 解析文本，剔除停用列 (Stop Cols) 及非数字参数，对齐 `LINE_INFO`、`SVID_INFO` 及 `DATA_TYPE_CD`。
4.  **分组聚合**: 对同一张 Glass，判断其所在班次（06:00/18:00）。根据 Step (工步) 和 Time (班次时间) 切片，过滤异常指标（如 NaN），然后进行指标的数学计算（Min、Max、Avg）。
5.  **批量入库**: 将聚合生成的 `LinkedHashMap` 使用 JDBC 批量提交的方式写入 `EDS_FDC_TRACE` 表中，如遇失败可事务回滚以保数据一致性。

---

## 4. 项目的影响力与使用价值

1. **变废为宝，解锁“黑盒”数据价值**：
   Array 制程设备通常会产生频率极高、体积庞大的 Trace 过程参数（例如各种气体流量、温度、压力、干泵数据等）。这些数据因为体积问题常以 Blob (Gzip 压缩) 的格式进行存储，如同“黑盒”。本项目作为解密“黑盒”的钥匙，将非结构化文本彻底转换为了易于查询的结构化指标。

2. **支撑 FDC 智能预警，提升良率**：
   通过本项目产出的各参数极值 (Max/Min) 和均值 (Avg) 宽表数据，上层的 FDC（故障检测与分类系统）或大数据分析平台，能够轻松绘制控制图 (Control Chart)、计算 CPK 等质量指标，实现对机台状态劣化和基板缺陷的早期预警，是提升工厂工艺良率的重要数据底座。

3. **高性能与自动化**：
   系统通过自定义的内存数据结构（Table）与纯流式文件解析（BufferedReader）、批处理提交（Batch Insert）相结合，确保了在工业界海量高频数据面前的稳健与低延迟。同时后台全自动化循环监控拉取，极大节省了原先繁杂的人工干预和数据导出成本，对半导体显示行业的数字化和智能化转型有着卓越的业务价值。
