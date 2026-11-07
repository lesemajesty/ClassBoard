# 数据库文件

本目录保存智慧电子班牌的 MySQL 8 数据库脚本，方便在新机器上还原演示环境。

| 文件 | 内容 | 用途 |
| --- | --- | --- |
| `classboard.sql` | 完整结构 + 当前演示数据 | 直接还原可运行的演示库 |
| `classboard-schema.sql` | 仅表结构 | 只需要空库，演示数据由应用初始化器生成 |

## 导入完整数据库

> `classboard.sql` 包含 `DROP TABLE IF EXISTS`，会覆盖当前 `classboard` 库中的同名表，导入前请先备份。

### 命令行

```powershell
mysql -uroot -p < database/classboard.sql
```

或者在 MySQL Workbench 中打开 `classboard.sql`，直接执行整个脚本。

### 仅导入表结构

```powershell
mysql -uroot -p < database/classboard-schema.sql
```

导入空结构后启动应用，`DataInitializer` / `LifeDataInitializer` 会在表为空时自动写入演示班级、学生、公告、课表、天气、新闻、点歌等数据。

## 说明

- 数据库名：`classboard`
- 字符集：`utf8mb4`
- 导出环境：MySQL 8.0.46 / Windows
- 完整脚本包含演示数据，其中 `sys_user` 表中是默认管理员 `admin / admin123` 的 BCrypt 密码哈希；部署到公网前请修改管理员密码。
- 应用默认连接配置见 `src/main/resources/application.yml`，可通过环境变量 `MYSQL_USER` / `MYSQL_PASSWORD` 覆盖。
