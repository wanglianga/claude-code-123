-- ============================================================
-- 诉前调解排期与协议履行平台 MySQL 初始化脚本
-- 适用 MySQL 8.x（utf8mb4）
-- 表结构由 Spring Data JPA（hibernate.ddl-auto=update）自动创建，
-- 本脚本仅负责创建数据库与账号授权。
-- ============================================================

CREATE DATABASE IF NOT EXISTS mediation
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- 如使用独立账号（可选）：
-- CREATE USER IF NOT EXISTS 'mediation'@'%' IDENTIFIED BY 'mediation123';
-- GRANT ALL PRIVILEGES ON mediation.* TO 'mediation'@'%';
-- FLUSH PRIVILEGES;

USE mediation;

-- 启动后端时使用 mysql profile 即会自动建表并写入演示数据：
-- java -jar mediation-platform.jar \
--   --spring.profiles.active=mysql \
--   --spring.datasource.url='jdbc:mysql://localhost:3306/mediation?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false' \
--   --spring.datasource.username=root --spring.datasource.password=你的密码
