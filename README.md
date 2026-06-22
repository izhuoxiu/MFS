# Haptic Event Schema (HES)

**一种通用的触觉交互描述语言**

[![License: CC BY 4.0](https://img.shields.io/badge/License-CC%20BY%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by/4.0/)
[![Version](https://img.shields.io/badge/version-v0.1-blue)](https://github.com/your-username/haptic-event-schema/releases)
[![Status](https://img.shields.io/badge/status-draft-orange)](https://github.com/your-username/haptic-event-schema)

---
https://www.doubao.com/building/render/a0bfeeab4e801?a=
---

## 为什么需要 HES？
Haptic Event Schema (HES)：下一代跨平台触觉交互描述协议
HES 是一套轻量级、机器可读的开源标准，旨在解决多模态交互中的“触觉语义断层”问题。它将音视频节奏、文本情感、空间导航等数字内容，转化为标准化的触觉事件指令。通过解耦“内容生成”与“设备执行”，HES 为开发者提供了一套统一的跨平台（iOS/Android/Web）触觉渲染 API，让沉浸式反馈的开发效率提升数倍。

今天的触觉反馈（Haptics）开发，

- 为 iPhone 写一套 CoreHaptics 代码
- 为华为写一套 VibrationEffect 代码
- 为小米写另一套……
- 换一款马达，全部重调

**问题是：我们到底在描述“怎么震”，还是在描述“感受什么”？**

HES 选择后者。

它定义了一套 **硬件无关的触觉词汇** 和 **通用脚本格式**，让开发者只需要说：

> “这里需要一个‘心跳感’”

由底层自动翻译成 iPhone、华为、小米各自能理解的震动指令。

---

## 核心内容

### 1. 触觉世界观（5 大维度）

| 维度 | 说明 | 示例 |
| :--- | :--- | :--- |
| **力量感 (Force)** | 震动的重量与锐度 | 轻触、重击、爆发 |
| **节奏感 (Rhythm)** | 震动的时间排列 | 单次、脉冲、心跳 |
| **空间感 (Space)** | 震动的方位与轨迹 | 接近、环绕、扩散 |
| **运动感 (Motion)** | 震动的物理惯性 | 上升、下降、加速 |
| **情绪感 (Emotion)** | 震动的心理暗示 | 紧张、释放、平静 |

### 2. 标准触觉词典（30 个基础词条）

| 编号 | 标准名 | 中文名 | 类型 |
| :--- | :--- | :--- | :--- |
| F-001 | Tap.Light | 轻触 | Force |
| F-002 | Tap.Medium | 敲击 | Force |
| F-003 | Tap.Heavy | 重击 | Force |
| F-004 | Shock.Burst | 爆发 | Force |
| R-103 | Rhythm.Pulse | 脉冲 | Rhythm |
| R-104 | Rhythm.Heartbeat | 心跳 | Rhythm |
| M-301 | Motion.Rise | 上升 | Motion |
| M-302 | Motion.Fall | 下降 | Motion |
| E-401 | Emotion.Tension | 紧张 | Emotion |
| E-404 | Emotion.Excite | 兴奋 | Emotion |
| ... | ... | ... | ... |

> 完整词典请查阅 [完整白皮书文档](HES/haptic-event-schema-v0.1.md)

### 3. 触觉脚本格式 (HSF)

HSF（Haptic Script Format）是 HES 的时间线表达格式，类似 **SRT 字幕** 或 **MIDI 音乐**：

```json
{
  "schema": "HapticScriptFormat",
  "version": "0.1",
  "meta": {
    "title": "副歌片段示例",
    "duration_ms": 32500
  },
  "tracks": [
    {
      "events": [
        {
          "time_ms": 15000,
          "event_ref": "R-103",
          "intensity": 0.6,
          "note": "底鼓"
        },
        {
          "time_ms": 30200,
          "event_ref": "F-004",
          "intensity": 1.0,
          "note": "高潮爆发"
        }
      ]
    }
  ]
}
```
