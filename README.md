### 分支用途

- 根据官网标准语法扩充流程来进行开发，学习执行器优化方式，研究语法树解析
- 国内calcite小众开发者

### 目标

- 基本多种类型SQL（DDL、DML）语句
- 支持多种存储引擎

### 当前进度

#### DDL

|                      | CREATE TABLE | DROP TABLE | ALTER TABLE | TRUNCATE TABLE | RENAME TABLE |
|----------------------| ------------ | ---------- | ----------- | -------------- | ------------ |
| Postgresql           | ✅            | ❌          | ❌           | ❌              | ❌            |
| ElasticSearch联合HBase | ❌            | ❌          | ❌           | ❌              | ❌            |
| Kafka                | ❌            | ❌          | ❌           | ❌              | ❌            |

#### DML

|                        | SELECT | INSERT | UPDATE | DELETE | REPLACE |
| ---------------------- | ------ | ------ | ------ | ------ | ------- |
| Postgresql             | ✅      | ✅      | ✅      | ✅      | ❌       |
| ElasticSearch组合HBase | ❌      | ❌      | ❌      | ❌      | ❌       |
| Kafka                  | ❌      | ❌      | ❌      | ❌      | ❌       |
