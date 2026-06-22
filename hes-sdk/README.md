## 🎛️ HES 极简调用引擎 | 覆盖全部30个标准词条(F-101 ~ E-406)
无需复杂解析、无需手动编写HSF脚本，项目引入单文件即可全局调用全部触觉语义事件。

### ✨ 核心优势
- 完整收录 HES v0.1 规范全部 30 条官方触觉词条（Force/Rhythm/Space/Motion/Emotion五大维度）
- 无第三方重型依赖，单文件引入即可集成
- 跨端统一API：Android / iOS / Web / Unity 调用语法完全对齐
- 内置硬件自动降级规则，低端设备自动适配简化触感

---

### 📦 快速接入方式
仅需一步：将 `HESEngine` 引擎源码文件复制到你的项目工程目录，即可全局调用。

### 🔹 1. Android / Java / C++ 标准调用示例
```
// 语法：HESEngine.play(词条编码, 震动强度[0.0 ~ 1.0])
调用HESEngine.play("F-104", 1.0f);

```

### 🔹 2. iOS  标准调用示例

// 语法：HESEngine.shared.play
调用HESEngine.shared.play(code: "F-104;
