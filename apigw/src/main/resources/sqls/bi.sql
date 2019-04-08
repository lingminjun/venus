# api配置
# jdbc:mysql://localhost:3306/bi?useUnicode=true&characterEncoding=utf8mb4&autoReconnect=true
CREATE DATABASE bi;


# 统计uv或者pv临时表，建议保留一个礼拜（最终使用数据请另行保存）
# 某个应用维度
# CREATE TABLE IF NOT EXISTS `bi_pv_history` (
#   `id`  bigint NOT NULL AUTO_INCREMENT ,
#   `htm`  bigint NOT NULL COMMENT 'hour time 调用时间，记录到小时，小时以后全为零' ,
#   `app`  int NOT NULL COMMENT '应用，具体名字参照小二后台定义' ,
#   `did`  bigint NOT NULL COMMENT '调用设备记录' ,
#   `md5`  varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'referer 的 md5，含参数，方便快速查找',
#   `tml`  int NOT NULL COMMENT 'terminal: pc端1, h5端2, iOS客户端3, android客户端4, 微信小程序5, 支付宝小程序6, 等等后续定义' ,
#   `ref`  varchar(2048) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '调用页面，含参数',
#   `uid`  bigint NULL DEFAULT NULL COMMENT '调用用户' ,
#   `acct`  bigint NULL DEFAULT NULL COMMENT '调用账号' ,
#   PRIMARY KEY (`id`),
#   UNIQUE INDEX `UNI_IDX` (`htm`,`app`,`did`,`md5`) USING BTREE,
#   INDEX `REFERER_IDX` (`md5`) USING BTREE,
#   INDEX `DID_IDX` (`did`) USING BTREE,
#   INDEX `UID_IDX` (`uid`) USING BTREE
# )
#   ENGINE=InnoDB
#   DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
#   AUTO_INCREMENT=1;

# 统计method调用情况，用于风控，建议保留三天 (一般风控预警一天内调用过于频繁接口)
# 此数据纯粹为过程数据，不建议保留
CREATE TABLE IF NOT EXISTS `bi_invoked_history` (
  `id`  bigint NOT NULL AUTO_INCREMENT ,
  `aid`  int NOT NULL COMMENT '应用端id' ,
  `app`  int NOT NULL COMMENT '应用，具体名字参照小二后台定义' ,
  `did`  bigint NOT NULL COMMENT '调用设备记录' ,
  `tid`  varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'trace id',
  `cip`  varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '客户端ip 255.255.255.255',
  `mthd`  varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'method 调用方法domain.module.method',
  `uid`  bigint NULL DEFAULT NULL COMMENT '调用用户' ,
  `acct`  bigint NULL DEFAULT NULL COMMENT '调用账号' ,
  `cvc`  int NULL DEFAULT 0 COMMENT '版本code' ,
  `cvn`  varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '客户端版本1.0.0',
  `cost`  bigint NULL DEFAULT 0 COMMENT '花销' ,
  `scs`  int NULL DEFAULT 0 COMMENT '是否成功' ,
  `at`  bigint NOT NULL COMMENT '调用时间' ,
  `tml`  int NOT NULL COMMENT 'terminal: pc端1, h5端2, iOS客户端3, android客户端4, 微信小程序5, 支付宝小程序6, 等等后续定义' ,
  `md5`  varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'referer 的 md5，含参数，方便快速查找',
  `ref`  varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'referer, 不带参数',
  `qry`  varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '参数部分，query and fragment',
  `vid`  varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'visit id, 追踪链路id, 系统内部规则',
  `src`  varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '追踪链路_src',
  `spm`  varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '追踪链路_spm',
  PRIMARY KEY (`id`),
  INDEX `TID_IDX` (`tid`) USING BTREE,
  INDEX `CIP_IDX` (`cip`) USING BTREE,
  INDEX `METHOD_IDX` (`mthd`) USING BTREE,
  INDEX `AT_IDX` (`at`) USING BTREE,
  INDEX `DID_IDX` (`did`) USING BTREE,
  INDEX `UID_IDX` (`uid`) USING BTREE,
  INDEX `APP_IDX` (`app`) USING BTREE,
  INDEX `REF_IDX` (`md5`) USING BTREE
)
  ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
  AUTO_INCREMENT=1;



















