# 官方源码基线

以下源码从官方 GitHub 仓库下载并保留 MIT 许可；下载日期 2026-10-23。

| 目录 | 官方仓库 | 分支 | 原始提交 |
|---|---|---|---|
| backend | https://github.com/yangzongzhuan/RuoYi-Vue | springboot3 | a51a838b71b446ea27256900efe7ed2faa2a02fd |
| frontend | https://github.com/yangzongzhuan/RuoYi-Vue3 | master | 838965c5a18d2c61b73ec30c6e288057aaa08b63 |

版本：RuoYi 3.9.2、Spring Boot 3.5.16、JDK 17。前端依赖精确版本与 npm package-lock.json 一并保留。

业务代码位于 `backend/ruoyi-education` 和 `frontend/src/views/education`。必要接入改动：

- UserConstants 将账号长度统一为3至30，密码8至32，兼容随机初始密码和学生内部账号。
- 后端父 POM 注册教育模块，admin 装配教育模块和开发用 H2。
- SysLoginService 通过同步登录事件执行教育登录政策，保存服务器端会话凭据状态；未改动密码校验、验证码与失败锁定实现。
- MyBatisConfig 注册扩展 Interceptor；仅 dev profile 改写 MySQL 时钟函数以运行内存 H2。
- 应用配置使用环境变量，日志改为可写目录，新增 dev / education profile。
- 前端扩展登录解析、初始密码路由限制、业务页面、首页与主题；菜单生成和权限指令沿用若依。

仓库保留上游 README 和 LICENSE 便于追溯；实际运行使用本目录 README 与 sql/V001–V003，不要执行原始带 DROP 的上游 SQL。
