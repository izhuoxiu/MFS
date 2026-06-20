---
title: "Haptic Event Schema (HES) — 技术规范白皮书"
subtitle: "一种通用的触觉交互描述语言"
author: "捉秀项目 (Pulse Project)"
date: "版本 v0.1 · 2026年6月"
geometry: "margin=2.5cm"
toc: true
toc-depth: 3
numbersections: true
---

# 封面

<p style="text-align:center; font-size:48px; font-weight:bold;">Haptic Event Schema</p>
<p style="text-align:center; font-size:24px;">Technical Specification</p>
<p style="text-align:center; font-size:18px;">Version 0.1</p>
<br>
<p style="text-align:center;">捉秀标准委员会 · 2026</p>

---

# 1. 项目背景

## 1.1 人类感官的缺失：为什么需要标准触觉语言

触觉是人类最早发育、最直接的身体感知通道，但在数字世界中，它长期被忽视。我们通过屏幕“看”到内容，通过扬声器“听”到声音，却极少通过震动“感受”到信息。

随着手机线性马达、穿戴设备、VR/AR 控制器等硬件普及，触觉反馈的硬件基础已经成熟。然而，**软件层面缺乏一套统一的描述语言**——每个平台（iOS、Android、Windows）各有自己的 API，开发者不得不为每个硬件重复编写波形代码，内容创作者也无法像撰写字幕一样“撰写触觉”。

## 1.2 当前行业痛点

| 痛点 | 说明 |
| :--- | :--- |
| **硬件碎片化** | 不同厂商的马达特性差异巨大，同样的震动参数在不同手机上效果天差地别 |
| **语义缺失** | 现有 API 只提供“频率、振幅、时长”等底层参数，没有“心跳”、“上升”等高阶概念 |
| **内容不可复用** | 一段精心设计的触觉脚本无法从一个 App 迁移到另一个，更无法跨平台 |
| **创作门槛高** | 触觉设计需要专业工具和硬件调试，普通内容创作者无法参与 |

## 1.3 本规范的目标与适用范围

**Haptic Event Schema (HES)** 是一套开放、免费、硬件无关的触觉描述标准。它定义：

- 一套基础触觉词汇（从“轻触”到“心跳”）
- 一种通用脚本格式（HSF，类似 MIDI 或 SRT）
- 一套内容到触觉的映射指南

**适用范围**：移动设备、游戏手柄、穿戴设备、VR/AR 控制器、汽车触觉提示等一切具备震动反馈能力的终端。

---

# 2. 设计原则

## 2.1 硬件无关性 (Hardware Agnostic)

HES 只描述“应该感受到什么”，而不规定“具体怎么震动”。具体波形渲染由各平台的实现层（Renderer）根据本地马达特性优化。

## 2.2 语义优先于波形 (Semantic over Waveform)

开发者优先使用 `Rhythm.Heartbeat` 而非 `频率=60Hz, 振幅=0.8, 时长=200ms`。语义层保证了跨设备体验的一致性。

## 2.3 组合性与扩展性 (Composable & Extensible)

基础词条可以组合成复合触觉（如“上升+心跳”），同时允许自定义扩展词条（保留私有编码区）。

## 2.4 向后兼容 (Backward Compatibility)

新版本词典必须保持旧编码的有效性。不支持高级词条的设备应具备降级策略（如替换为 `Tap.Light` 或静默）。

---

# 3. 基础触觉分类体系 (Taxonomy)

我们将人类可感知的触觉分为 **5 大维度**，共 **18 个子类**。

## 3.1 力量感 (Force) —— 震动的“重量”与“锐度”

| 子类 | 英文 | 描述 |
| :--- | :--- | :--- |
| 轻触 | Tap | 蜻蜓点水，极短促 |
| 敲击 | Knock | 有明确起止的撞击感 |
| 重击 | Heavy | 沉闷、有力、下坠感 |
| 爆发 | Burst | 瞬间释放的巨大能量 |
| 震荡 | Oscillate | 持续高频的颤抖 |
| 碾压 | Crush | 逐渐加重直至极限 |

## 3.2 节奏感 (Rhythm) —— 震动的“时间排列”

| 子类 | 英文 | 描述 |
| :--- | :--- | :--- |
| 单次 | Single | 孤立的、一次性的 |
| 重复 | Repeat | 均匀的来回往复 |
| 脉冲 | Pulse | 规律且带有间隙的推进 |
| 心跳 | Heartbeat | 强弱交替的“咚哒”模式 |
| 波浪 | Wave | 强度呈正弦曲线起伏 |
| 滚奏 | Roll | 极快速密集的连续敲击 |

## 3.3 空间感 (Space) —— 震动的“方位与轨迹”（预留）

| 子类 | 英文 | 描述 |
| :--- | :--- | :--- |
| 接近 | Approach | 由弱变强（物体靠近） |
| 远离 | Retreat | 由强变弱（物体离去） |
| 穿过 | Through | 从左到右或从前到后的位移感 |
| 环绕 | Orbit | 围绕身体旋转 |
| 扩散 | Expand | 从中心向四周散开 |
| 聚集 | Contract | 从四周向中心收拢 |

> *注：空间感需要多马达或穿戴设备支持，本版本仅作架构预留。*

## 3.4 运动感 (Motion) —— 震动的“物理惯性”

| 子类 | 英文 | 描述 |
| :--- | :--- | :--- |
| 上升 | Rise | 频率或强度递增 |
| 下降 | Fall | 频率或强度递减 |
| 加速 | Accel | 间隔越来越短 |
| 减速 | Decel | 间隔越来越长 |
| 悬浮 | Hover | 极度轻微且持续的飘忽感 |
| 急停 | Brake | 高速震动瞬间归零 |

## 3.5 情绪感 (Emotion) —— 震动的“心理暗示”

| 子类 | 英文 | 描述 |
| :--- | :--- | :--- |
| 紧张 | Tension | 高频细碎、不规律 |
| 压迫 | Oppress | 沉重、喘不过气 |
| 释放 | Release | 紧张后的突然松弛 |
| 兴奋 | Excite | 高频率、高强度的连续刺激 |
| 平静 | Calm | 极低频、极柔和的包裹感 |
| 诡异 | Weird | 不和谐的非对称震动 |

---

# 4. 事件编码规范

## 4.1 编码规则

每个标准事件由 **前缀字母 + 三位数字** 组成，前缀代表所属维度：

| 前缀 | 维度 | 示例 |
| :--- | :--- | :--- |
| F | 力量感 (Force) | F-001 |
| R | 节奏感 (Rhythm) | R-103 |
| S | 空间感 (Space) | S-201 |
| M | 运动感 (Motion) | M-301 |
| E | 情绪感 (Emotion) | E-401 |

数字段从 001 开始，每维度保留 000–099 为私有扩展区，100–999 为公开标准词条。

## 4.2 事件属性

每个词条应至少包含以下属性：

| 属性 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | string | 唯一编码，如 `F-001` |
| `name` | string | 标准英文名，如 `Tap.Light` |
| `cn_name` | string | 中文名，如 `轻触` |
| `category` | string | 所属维度，如 `Force` |
| `intensity_default` | float (0–1) | 默认强度 |
| `duration_ms` | int | 建议持续时间（毫秒） |
| `waveform_hint` | string | 波形建议（方波/正弦波/锯齿波/自定义包络） |
| `description` | string | 物理感受描述 |

---

# 5. 标准事件词典（核心 30 词条）

| 编号 | 标准名 | 中文名 | 维度 | 默认强度 | 时长(ms) | 波形建议 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F-001 | Tap.Light | 轻触 | Force | 0.2 | 15 | 方波 |
| F-002 | Tap.Medium | 敲击 | Force | 0.5 | 30 | 方波 |
| F-003 | Tap.Heavy | 重击 | Force | 0.8 | 80 | 方波+衰减尾 |
| F-004 | Shock.Burst | 爆发 | Force | 1.0 | 150 | 陡起缓落包络 |
| F-005 | Vibrate.Shake | 震荡 | Force | 0.6 | 200 | 150Hz正弦波 |
| F-006 | Press.Crush | 碾压 | Force | 0.3→1.0 | 400 | 线性渐强 |
| R-101 | Rhythm.Single | 单次 | Rhythm | 0.4 | 30 | 方波 |
| R-102 | Rhythm.Double | 双连击 | Rhythm | 0.5 | 30×2 (间隔80ms) | 方波 |
| R-103 | Rhythm.Pulse | 脉冲 | Rhythm | 0.6 | 持续(0.5s开/0.3s关) | 方波 |
| R-104 | Rhythm.Heartbeat | 心跳 | Rhythm | 强0.8/弱0.4 | 80/40 | 咚哒模式 |
| R-105 | Rhythm.Wave | 波浪 | Rhythm | 0.2→0.8→0.2 | 周期1000ms | 正弦包络 |
| R-106 | Rhythm.Roll | 滚奏 | Rhythm | 0.4 | 每次30ms，持续200ms | 密集方波 |
| S-201 | Space.Approach | 接近 | Space | 0.1→0.9 | 1500 | 强度递增 |
| S-202 | Space.Retreat | 远离 | Space | 0.9→0.1 | 1500 | 强度递减 |
| S-203 | Space.Through | 穿过 | Space | 0.5 | 300 | 相位移动（预留） |
| S-204 | Space.Orbit | 环绕 | Space | 0.5 | 持续 | 循环相位（预留） |
| S-205 | Space.Expand | 扩散 | Space | 0.6 | 500 | 中心→外围感（预留） |
| S-206 | Space.Contract | 聚集 | Space | 0.6 | 500 | 外围→中心感（预留） |
| M-301 | Motion.Rise | 上升 | Motion | 0.2→0.8 | 1000 | 频率30→120Hz |
| M-302 | Motion.Fall | 下降 | Motion | 0.8→0.2 | 1000 | 频率120→30Hz |
| M-303 | Motion.Accel | 加速 | Motion | 0.6 | 间隔递减 | 间隔300→100ms |
| M-304 | Motion.Decel | 减速 | Motion | 0.6 | 间隔递增 | 间隔100→300ms |
| M-305 | Motion.Hover | 悬浮 | Motion | 0.1 | 持续 | 80Hz细碎震动 |
| M-306 | Motion.Brake | 急停 | Motion | 0.8→0 | 瞬间 | 强震骤停 |
| E-401 | Emotion.Tension | 紧张 | Emotion | 0.5 | 不规律 | 高频不规则敲击 |
| E-402 | Emotion.Oppress | 压迫 | Emotion | 0.7 | 持续 | 40Hz低频重压 |
| E-403 | Emotion.Release | 释放 | Emotion | 长震→0→轻触 | 可变 | 长衰减+尾触 |
| E-404 | Emotion.Excite | 兴奋 | Emotion | 1.0 | 乱序 | 高强度复合脉冲 |
| E-405 | Emotion.Calm | 平静 | Emotion | 0.15 | 持续 | 20Hz超低频柔和 |
| E-406 | Emotion.Weird | 诡异 | Emotion | 0.5 | 不对称 | 左右/强弱随机 |

---

# 6. 触觉脚本格式 (HSF) 规范

## 6.1 文件结构

HSF 使用 JSON 作为载体，结构如下：

```json
{
  "schema": "HapticScriptFormat",
  "version": "0.1",
  "meta": {
    "title": "可选的脚本标题",
    "duration_ms": 32500,
    "bpm": 113,
    "author": "作者名"
  },
  "tracks": [
    {
      "id": "main_motor",
      "device_target": "phone_motor",
      "events": [
        {
          "id": "evt_001",
          "time_ms": 15000,
          "duration_ms": 40,
          "event_ref": "F-001",
          "intensity": 0.6,
          "note": "底鼓",
          "envelope": {
            "attack_ms": 5,
            "decay_ms": 30,
            "release_ms": 5
          }
        }
      ]
    }
  ]
}
```

##  6.2字段定义
## 

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| schema | string | 是 | 固定为 "HapticScriptFormat" |
| version | string | 是 | 版本号，如 "0.1" |
| meta | object | 否 | 元数据，包含标题、时长、BPM等 |
| tracks | array | 是 | 轨道列表，每个轨道对应一个物理设备（如手机马达、手柄等） |
| track.id | string | 是 | 轨道唯一标识 |
| track.device_target | string | 否 | 设备类型提示，供渲染器优化 |
| track.events | array | 是 | 事件列表，按时间升序排列 |
| event.id | string | 否 | 事件唯一标识（便于调试） |
| event.time_ms | integer | 是 | 绝对起始时间（毫秒） |
| event.duration_ms | integer | 否 | 持续时长，若不填则使用词典默认值 |
| event.event_ref | string | 是 | 词典编码，如 F-001 |
| event.intensity | float (0–1) | 否 | 强度增益，默认为词典默认值 |
| event.note | string | 否 | 描述性备注 |
| event.envelope | object | 否 | 自定义包络，覆盖默认波形 |
| envelope.attack_ms | int | 否 | 起音时间 |
| envelope.decay_ms | int | 否 | 衰减时间 |
| envelope.release_ms | int | 否 | 释放时间 |


## 6.3排序与合并法则

· 所有事件必须按 time_ms 升序排列。

· 若两个事件重叠，渲染器应决定如何混合（建议取强度最大值或叠加）。

· 事件频率建议不超过 8次/秒，保护马达寿命

 ---

# 7.内容映射指南


本节为开发者提供从媒体内容自动生成 HES 事件的参考规则。

### 7.1 音频特征映射表
| 音频特征 | 检测方式 | 映射为 HES 事件 | 强度映射 |
| :--- | :--- | :--- | :--- |
| 底鼓瞬态 | 50–100Hz 能量峰值 | R-103 (Pulse) | 归一化能量 × 0.6 |
| 军鼓/拍手 | 1k–5kHz 瞬态 | F-002 (Tap.Medium) | 归一化能量 × 0.5 |
| 持续低频 (808) | 20–60Hz 连续能量 | E-402 (Oppress) | 能量值 × 0.7 |
| 白噪音上升 (Build-up) | 频谱质心上升 | M-301 (Rise) | 0.2→0.8 |
| 副歌高潮 | 能量瞬间跳变 | F-004 (Burst) + E-404 (Excite) | 1.0 |
| 节奏减速 | 节拍间隔增大 | M-304 (Decel) | 0.6 |

### 7.2 视觉特征映射表
| 视觉特征 | 触发条件 | 映射事件 |
| :--- | :--- | :--- |
| 镜头快速拉近 | 光流速度 > 阈值 | S-201 (Approach) |
| 画面爆炸/闪白 | 亮度瞬间峰值 | S-205 (Expand) + F-004 (Burst) |
| 主观坠落 | 画面快速下移 | M-302 (Fall) + E-401 (Tension) |

### 7.3 交互事件映射表
| UI 操作 | 映射事件 |
| :--- | :--- |
| 按钮点击 | F-001 (Tap.Light) |
| 滑动选择 | R-105 (Wave) 或 F-001 轻触 |
| 长按 | F-006 (Crush) |
| 通知到达 | R-104 (Heartbeat) 或 E-404 (Excite) |

7.4 冲突解决

当多个源同时产生事件时，遵循：

· 优先级：交互 > 视频 > 音频 > 背景
· 合并策略：取强度最高者，或叠加（需注意限流）

---
# 8发者实现规范

8.1 解析器实现要求

· 必须支持 JSON 格式的 HSF 解析。

· 必须识别 version 字段，并对未来版本做向下兼容。

· 对于未知 event_ref，应触发降级策略。

### 8.2 渲染器最小实现（Android / iOS）
| 平台 | 基础映射 | 降级策略 |
| :--- | :--- | :--- |
| Android | 将 event_ref 转换为 VibrationEffect | 不支持的事件 → VibrationEffect.createOneShot(50, 128) |
| iOS | 将 event_ref 转换为 CHHapticPattern | 不支持的事件 → UIImpactFeedbackGenerator.impactOccurred() |


具体映射表请参考附录 A 和 B。

8.3 性能要求

· 解析 HSF 时间 ≤ 5ms（对于 10 分钟长度的脚本）
· CPU 实时渲染占用 < 3%（持续播放时）
· 内存开销 < 20MB

8.4 降级策略

· 硬件不支持高级震动：将复杂事件替换为 F-001 (Tap.Light)。
· CPU 过载：降低事件采样率（如合并 50ms 内的事件）。
· 马达过热：暂停触觉输出，待温度恢复后继续。

---

9. 应用场景

9.1 短视频与音乐流媒体（捉秀 App）

通过 HES 脚本，让用户在看视频或听歌时，手机自动播放与之同步的触觉反馈，实现“视听触”三重沉浸。

9.2 无障碍辅助

为视障用户提供触觉导航：左转、右转、前方障碍等不同 HES 事件，帮助感知周围环境。

9.3 游戏事件反馈

将游戏内的脚步声、枪击、爆炸、碰撞等事件映射为 HES 脚本，增强沉浸感。

9.4 驾驶安全预警

车道偏离、碰撞警告等通过高强度 HES 事件传递，比声音更直接、反应更快。

9.5 通知语义化

不同 App 或不同通知类型（微信、邮件、闹钟）使用不同的 HES 事件，用户无需看屏幕即可感知重要程度。

---

# 10. 未来路线图
| 版本 | 内容 |
| :--- | :--- |
| v0.1 ✅ | 基础分类体系 + 30 个核心词条 + HSF 草案（当前版本） |
| v0.2 | 空间感 (Space) 完整实现 + 多马达/穿戴设备映射指南 |
| v0.3 | AI 辅助自动标注工具（音频→HES 脚本） |
| v1.0 | 稳定版：完整词典（100+ 词条）+ 正式映射表 + 验证工具集 |
| v2.0 | 触觉内容分发协议 + 云端动态优化引擎 

|


## 附录 A：Android VibrationEffect 映射参考（简表）
| HES 事件 | Android API 建议 |
| :--- | :--- |
| F-001 (Tap.Light) | VibrationEffect.createOneShot(15, 80) |
| F-004 (Shock.Burst) | VibrationEffect.createWaveform([0, 100, 200], [0, 255, 0], -1) |
| R-104 (Heartbeat) | VibrationEffect.createWaveform([0, 80, 100, 40], [0, 255, 0, 128], -1) |
| 其他 | 映射为 createOneShot(duration, amplitude)，amplitude 根据 intensity 换算 |


## 附录 B：iOS CoreHaptics 映射参考（简表）
| HES 事件 | iOS 实现思路 |
| :--- | :--- |
| F-001 (Tap.Light) | CHHapticEvent(eventType: .hapticContinuous, parameters: [.hapticIntensity(0.2), .hapticSharpness(0.8)], relativeTime: 0, duration: 0.015) |
| F-004 (Shock.Burst) | 自定义包络，快速起振后衰减 |
| R-104 (Heartbeat) | 组合两个事件，间隔 0.8s |
| 其他 | 使用 CHHapticEvent 组合，intensity/sharpness 映射 |


## 附录 C：测试用例与标准测试曲目集
| 曲目 | 预期事件 |
| :--- | :--- |
| 摇滚乐（如《We Will Rock You》） | 强节拍 → R-103，副歌 → E-404 |
| 电子乐（如《Animals》） | 持续低频 → E-402，Drop → F-004 |
| 古典钢琴（如《月光奏鸣曲》） | 轻柔段落 → E-405，渐强 → M-301 |


## 附录 D：术语表 (Glossary)
| 术语 | 解释 |
| :--- | :--- |
| HES | Haptic Event Schema，本规范名称 |
| HSF | Haptic Script Format，触觉脚本格式 |
| 事件 (Event) | 一个独立的触觉描述单元，对应一个词典词条 |
| 轨道 (Track) | 同一设备或同一触觉通道的事件序列 |
| 包络 (Envelope) | 震动强度随时间变化的轮廓（起音-衰减-持续-释放） |
| 渲染器 (Renderer) | 将 HES 事件转换为具体硬件震动指令的模块 |
| 降级 (Fallback) | 在不支持特定事件时替换为更通用的替代事件 |



## 版本历史
| 版本 | 日期 | 变更说明 |
| :--- | :--- | :--- |
| v0.1 | 2026-06-20 | 初始草案，含 5 大维度、30 个词条、HSF 格式及映射指南 |


---

版权与引用

本规范由 捉秀项目 (Pulse Project) 发起并维护，采用 CC BY 4.0 许可证公开发布。

引用方式：

捉秀 Haptic Event Schema (HES) v0.1. (2026). 取自 https://github.com/Tiggero/haptic-event-schema

---


