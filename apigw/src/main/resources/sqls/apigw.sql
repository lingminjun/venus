# api配置
# jdbc:mysql://localhost:3306/apigwdb?useUnicode=true&characterEncoding=utf8mb4&autoReconnect=true
CREATE DATABASE apigw;


# 接口申明定义以及配置列表
CREATE TABLE IF NOT EXISTS `apigw_api` (
  `id`  bigint NOT NULL AUTO_INCREMENT ,
  `domain`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '服务名',
  `module`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '模块名' ,
  `method`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '方法名',

  `owner`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `version`  varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '版本',
  `detail`  varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',

  `security`  int DEFAULT 0 COMMENT '接口权限 取值@See ESBSecurityLevel' ,

  `json`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '配置结构',

  `status`  tinyint DEFAULT 0 COMMENT '状态:0普通接口;1开放平台接口;-1禁用;' ,
  `mock`  tinyint DEFAULT 0 COMMENT '是否mock返回，apigw_api_mock' ,

  `md5`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '校验md5',
  `upstamp`  bigint DEFAULT 0 COMMENT '发布时间戳' ,
  `prestamp`  bigint DEFAULT 0 COMMENT '前一个发布时间戳' ,
  `thestamp`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '发布时间戳,YYYYMMDD-HHMMSS.SSS' ,

  `created`  bigint DEFAULT 0 COMMENT '创建时间' ,
  `modified`  bigint DEFAULT 0 COMMENT '修改时间' ,
  `rollback`  tinyint DEFAULT 0 COMMENT '表示回滚删除;' ,

  PRIMARY KEY (`id`),
  UNIQUE INDEX `UNI_IDX_ID` (`domain`,`module`,`method`) USING BTREE,
  INDEX `IDX_TIMESTAMP` (`upstamp`) USING BTREE
)
  ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
  AUTO_INCREMENT=1;


# 接口申明定义以及配置列表
CREATE TABLE IF NOT EXISTS `apigw_api_history` (
  `id`  bigint NOT NULL AUTO_INCREMENT ,
  `upstamp`  bigint DEFAULT 0 COMMENT '发布时间戳' ,
  `domain`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '服务名',
  `module`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '模块名' ,
  `method`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '方法名',

  `owner`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `version`  varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '版本',
  `detail`  varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',

  `security`  int DEFAULT 0 COMMENT '接口权限 取值@See ESBSecurityLevel' ,

  `json`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '配置结构',

  `status`  tinyint DEFAULT 0 COMMENT '状态:0普通接口;1开放平台接口;-1禁用;' ,
  `mock`  tinyint DEFAULT 0 COMMENT '是否mock返回，apigw_api_mock' ,

  `md5`  varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '校验md5',
  `prestamp`  bigint DEFAULT 0 COMMENT '前一个发布时间戳' ,
  `thestamp`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '发布时间戳,YYYYMMDD-HHMMSS.SSS' ,

  `created`  bigint DEFAULT 0 COMMENT '创建时间' ,
  `modified`  bigint DEFAULT 0 COMMENT '修改时间' ,
  `rollback`  tinyint DEFAULT 0 COMMENT '表示回滚删除;' ,

  PRIMARY KEY (`id`),
  UNIQUE INDEX `UNI_IDX_ID` (`upstamp`,`domain`,`module`,`method`) USING BTREE,
  INDEX `IDX_ID` (`domain`,`module`,`method`) USING BTREE
)
  ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
  AUTO_INCREMENT=1;


# 接口mock值返回（此功能尽量不要在api库中）
CREATE TABLE IF NOT EXISTS `apigw_api_mock` (
  `id`  bigint NOT NULL AUTO_INCREMENT ,
  `api_id`  bigint NOT NULL  COMMENT '接口id',

  `mock`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '配置结构',

  `create`  bigint DEFAULT 0 COMMENT '创建时间' ,
  `modified`  bigint DEFAULT 0 COMMENT '修改时间' ,

  PRIMARY KEY (`id`),
  UNIQUE INDEX `UNI_IDX_ID` (`api_id`) USING BTREE
)
  ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
  AUTO_INCREMENT=1;


# 账号表; 用户能绑定多个账号,所有的账号,都具备登录的能力
CREATE TABLE IF NOT EXISTS `apigw_third_party_secret` (
  `id`  bigint NOT NULL AUTO_INCREMENT ,
  `platform`  varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '第三方平台',
  `info`  varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '其他说明信息',

  `prikey`  varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '颁发的私钥',
  `pubkey`  varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '颁发的公钥',
  `algo`  varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '颁发秘钥算法' ,

  `create`  bigint DEFAULT 0 COMMENT '创建时间' ,
  `modified`  bigint DEFAULT 0 COMMENT '修改时间' ,
  `delete`  tinyint DEFAULT 0 COMMENT '0: enabled, 1: deleted' ,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UNI_IDX_ID` (`platform`) USING BTREE
)
  ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
  AUTO_INCREMENT=1;



















