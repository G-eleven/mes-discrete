# 参考项目清单（references）

> 调研结论：开源界**没有**"前端 Vue3 + 后端 Java"的完整开源 MES。本项目以自建（对齐飞书设计文档）为主，
> 以下项目在工程模式与业务设计上提供参考。只借鉴设计思路，未拷贝任何代码（避开 AGPL 项目）。

## 直接参考（MIT/Apache，可放心借鉴）

| 项目 | 地址 | 借鉴点 |
|------|------|--------|
| RuoYi-Vue3 | https://github.com/yangzongzhuan/RuoYi-Vue3 （gitee: y_project/RuoYi-Vue3） | Vue3+Vite+Element Plus+Pinia 的工程组织；登录/布局/CRUD 页面范式；本项目的 axios 拦截器、菜单过滤、表格页模板均参照其惯例（未使用其代码生成器与 sys_* 体系） |
| ktg-mes（苦糖果） | https://gitee.com/kutangguo/ktg-mes | Java 系离散 MES 中模块最全（工厂建模/工艺/BOM/工单/报工/仓库/设备/质检）；其"工单驱动 + 工序流转"的业务骨架与表前缀风格（md_/plan_ 等）是本项目基础数据表设计的参照 |
| cp-mes-ruoyi | https://gitee.com/cloudpulse/cp-mes-ruoyi | 小而全的若依式 MES（52 commits 可精读）；"基础数据→工单→报工→不良→报表"的闭环顺序即本项目的实现顺序 |

## 页面范本（前端 Vue3，后端 .NET，仅看 UI 交互）

| 项目 | 地址 | 参考点 |
|------|------|--------|
| iMES 工厂管家 | https://gitee.com/ZM-Rid/imes | Vue3+Element Plus 的 MES 全模块页面长什么样；模块清单（基础数据/库存/生产/质量/看板）可当需求清单 |
| TMom | https://gitee.com/thgao/tmom | Vue3+TS+AntD 的 MES 范本（可视化工艺路线、低代码报表/大屏）；若想换 Ant Design Vue 风格看它 |

## 仅作业务理解（AGPL 或停更，勿抄代码）

| 项目 | 地址 | 看点 |
|------|------|------|
| smart-mes（离散制造MES） | https://gitee.com/hero-of-tang-dynasty/smart-mes（镜像） | Gitee MES 榜首时期的表结构设计（AGPL，只读不抄） |
| 章鱼师兄 MES-Springboot | https://gitee.com/wangziyangyang/MES-Springboot | ISA-95 标准下工单下达/在制品/质检的概念讲解（配套博客） |
| qcadoo/mes | https://github.com/qcadoo/mes | 老牌 Java 开源 MES（非 Vue），纯业务域模型参考 |

## License 红线备忘

- MIT/Apache（RuoYi、ktg-mes、cp-mes-ruoyi）：借鉴、改造、再发布均安全。
- AGPL（smart-mes 等）：学习阅读无碍；衍生代码若发布需同协议开源——本仓库未拷贝其代码。
- iMES/TMom 声明"仅限学习研究"：看页面交互可以，勿直接商用。

## 调研日期

2026-08-17（详细对比表见本次会话调研记录；star 数为当时量级，仅供方向参考）。
