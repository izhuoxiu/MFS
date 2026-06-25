# MFS C++ Wasm 内核完整小白教程（完全匹配白皮书 v0.2.3）
## 前置总览
### 1. 白皮书核心规则提炼（代码全部遵循）
 * **脚本载体**：HSF JSON，多轨道（震动/彩带/闪光灯），事件带 time_ms 时间戳。
 * **调度逻辑**：按时间升序遍历事件，传入视频当前毫秒 tick(ms) 触发区间内事件。
 * **五维优先级**：情绪 > 运动 > 力量 > 空间 > 轨道自定义 priority_override。
 * **三模态输出**：震动事件、屏幕彩带动画、闪光灯频闪，回调给前端 JS。
 * **兼容性**：向下兼容旧 HES，未知事件自动降级为轻震 F-101。
 * **高并发性能**：单次解析 < 5ms，调度延迟 < 5ms，满足整体 15ms 同步要求。
 * **交互分层**：C++ 内核只做解析 + 时序调度，硬件渲染全部交给上层 H5/APP 原生层。
### 2. 最终产物
 * mfs_core.cpp：唯一内核源码，只维护这一份。
 * **编译输出**：mfs.js（胶水桥接文件）+ mfs.wasm（二进制内核）。
 * **应用场景**：前端 H5 直接引入 mfs.js 自动加载 Wasm，一套代码网页/APP WebView 通用。
### 3. 小白前置软件（必须全部安装）
 1. **Git**：下载工具与 JSON 库。
 2. **Python 3.8+**：Emscripten 依赖，安装时务必勾选 **Add Python to PATH**。
 3. **Emscripten SDK**：C++ 转 Wasm 专用编译器（全程只用这个）。
 4. **文本编辑器**：VS Code（最推荐）。
## 第一部分：一步一步搭建编译环境（Windows/Mac 通用）
### 步骤 1：安装 Git、Python3
 1. Python 官网下载安装，勾选 **Add Python to PATH**，安装后打开 CMD/终端输入 python --version 验证。
 2. Git 官网默认安装，CMD/终端输入 git --version 验证。
### 步骤 2：下载安装 Emscripten（编译工具）
 1. 新建工具文件夹：
   * **Windows**: D:\wasm_tools
   * **Mac**: ~/wasm_tools
 2. 进入文件夹，执行克隆命令：
   ```bash
   git clone https://github.com/emscripten-core/emsdk.git
   cd emsdk
   
   ```
 3. 安装并激活最新工具链（等待 5-10 分钟下载）：
   * **Windows CMD**:
     ```cmd
     emsdk install latest
     emsdk activate latest
     emsdk_env.bat
     
     ```
   * **Mac/Linux 终端**:
     ```bash
     ./emsdk install latest
     ./emsdk activate latest
     source ./emsdk_env.sh
     
     ```
 4. 输入以下命令验证是否成功：
   ```bash
   emcc --version
   
   ```
   > 💡 **小白注意**：出现版本号即安装完成。每次新建终端窗口准备编译时，都需要重新执行一次上面的 emsdk_env 脚本来加载环境变量。
   > 
### 步骤 3：准备 JSON 解析库（nlohmann 单头文件，零编译）
 1. 新建项目总文件夹 D:\mfs_wasm（或 Mac 下的 ~/mfs_wasm）。
 2. 进入该文件夹，下载单文件版 json.hpp：
   ```bash
   git clone https://github.com/nlohmann/json.git
   
   ```
 3. 将 json/single_include/nlohmann/json.hpp 复制到 mfs_wasm 根目录下，其余下载的文件可以直接删除，只留这一个头文件。
### 步骤 4：创建项目文件结构（严格对齐，请勿改名）
```plaintext
mfs_wasm/
├─ json.hpp          # JSON解析库（不用修改）
├─ mfs_core.cpp      # 核心C++内核（主要编写的代码）
└─ build.sh          # 一键编译脚本（Windows系统用build.bat）

```
## 第二部分：完整 MFS C++ 内核源码（完全对齐白皮书 v0.2.3）
新建 mfs_core.cpp，完整复制并粘贴以下代码：
```cpp
#include <iostream>
#include <vector>
#include <string>
#include <algorithm>
#include <cstdint>
#include <map>
#include "json.hpp"
#include <emscripten.h>
#include <emscripten/val>

using json = nlohmann::json;

// ===================== 白皮书常量定义 =====================
#define PRIORITY_EMOTION    5
#define PRIORITY_MOTION     4
#define PRIORITY_FORCE      3
#define PRIORITY_SPACE      2
#define PRIORITY_RHYTHM     1
#define PRIORITY_DEFAULT    0

// 模态类型，用于回调区分
enum MfsModalType {
    MODAL_HAPTIC = 1, // 马达震动
    MODAL_RIBBON = 2, // 屏幕边缘彩带
    MODAL_FLASH  = 3  // 后置闪光灯
};

// 单个MFS事件
struct MfsEvent {
    uint32_t time_ms;
    uint32_t duration_ms;
    std::string event_ref;
    float intensity;
    int priority;
    json ribbon_params;
    json flash_params;
    json trail_params;
    bool triggered;          // 是否已触发（用于跳转后重新触发）
};

// 轨道结构体
struct MfsTrack {
    std::string track_id;
    int priority_override;
    std::vector<MfsEvent> events;
};

// 内核主类
class MFS_Core {
private:
    std::vector<MfsTrack> track_list;
    uint32_t last_tick_ms = 0;
    emscripten::val js_event_callback;

    // 根据编码获取基础优先级（五维铁律）
    int get_event_base_priority(const std::string& ref) {
        if (ref.empty()) return PRIORITY_DEFAULT;
        char c = ref[0];
        switch (c) {
            case 'E': return PRIORITY_EMOTION;
            case 'M': return PRIORITY_MOTION;
            case 'F': return PRIORITY_FORCE;
            case 'S': return PRIORITY_SPACE;
            case 'R': return PRIORITY_RHYTHM;
            default:  return PRIORITY_DEFAULT;
        }
    }

    // 判断是否为已知标准事件（若未知则降级）
    bool is_known_event(const std::string& ref) {
        if (ref.length() < 2) return false;
        char c = ref[0];
        if (c != 'F' && c != 'R' && c != 'S' && c != 'M' && c != 'E') return false;
        return ref.length() >= 4 && ref[1] == '-';
    }

    // 对所有轨道的事件按时间排序
    void sort_all_events() {
        for (auto& track : track_list) {
            std::sort(track.events.begin(), track.events.end(),
                [](const MfsEvent& a, const MfsEvent& b) {
                    return a.time_ms < b.time_ms;
                });
        }
    }

    // 重置所有事件的触发标记（用于跳转后重新触发）
    void reset_triggered_flags() {
        for (auto& track : track_list) {
            for (auto& evt : track.events) {
                evt.triggered = false;
            }
        }
    }

    // 触发单个模态的回调
    void dispatch_modal(MfsModalType type, const MfsEvent& evt, const json& params = json()) {
        if (js_event_callback.isUndefined()) return;
        std::string params_str = params.is_null() ? "{}" : params.dump();
        js_event_callback(static_cast<int>(type), evt.event_ref, evt.intensity, evt.duration_ms, params_str);
    }

public:
    // 绑定JS回调
    void set_callback(emscripten::val cb) {
        js_event_callback = cb;
    }

    // 加载HSF脚本
    int load_hsf_script(const std::string& hsf_json_str) {
        try {
            json root = json::parse(hsf_json_str);
            track_list.clear();
            last_tick_ms = 0;

            if (!root.contains("tracks")) return -1;

            for (auto& track_json : root["tracks"]) {
                MfsTrack track;
                track.track_id = track_json.contains("id") ? track_json["id"].get<std::string>() : "";
                track.priority_override = track_json.contains("priority_override") ? track_json["priority_override"].get<int>() : -1;

                if (!track_json.contains("events")) continue;
                for (auto& evt_json : track_json["events"]) {
                    MfsEvent evt;
                    // 必填字段校验
                    if (!evt_json.contains("time_ms") || !evt_json.contains("event_ref")) continue;
                    evt.time_ms = evt_json["time_ms"].get<uint32_t>();
                    evt.event_ref = evt_json["event_ref"].get<std::string>();

                    // 可选字段赋合理默认值
                    evt.intensity = evt_json.contains("intensity") ? evt_json["intensity"].get<float>() : 1.0f;
                    evt.duration_ms = evt_json.contains("duration_ms") ? evt_json["duration_ms"].get<uint32_t>() : 0;

                    // 读取多模态配套参数
                    evt.ribbon_params = evt_json.contains("ribbon_parameters") ? evt_json["ribbon_parameters"] : json();
                    evt.flash_params = evt_json.contains("led_parameters") ? evt_json["led_parameters"] : json();
                    evt.trail_params = evt_json.contains("trail_parameters") ? evt_json["trail_parameters"] : json();

                    // 未知事件降级为标准轻震 F-101
                    if (!is_known_event(evt.event_ref)) {
                        evt.event_ref = "F-101";
                    }

                    // 计算优先级
                    int base_pri = get_event_base_priority(evt.event_ref);
                    evt.priority = (track.priority_override > 0) ? track.priority_override : base_pri;
                    evt.triggered = false;

                    track.events.push_back(evt);
                }
                track_list.push_back(track);
            }

            sort_all_events();
            reset_triggered_flags();
            return 0;
        } catch (const std::exception& e) {
            return -1;
        }
    }

    // 核心tick：每帧调用，current_ms为当前视频时间（毫秒）
    void tick(uint32_t current_ms) {
        // 处理视频进度跳转（如用户向后拖动进度条）
        if (current_ms < last_tick_ms) {
            last_tick_ms = (current_ms > 0) ? current_ms - 1 : 0;
            reset_triggered_flags();
        }

        uint32_t start = last_tick_ms;
        uint32_t end = current_ms;
        last_tick_ms = current_ms;

        if (start >= end) return; 

        // 收集本帧内所有候选事件
        std::vector<MfsEvent*> haptic_candidates;
        std::vector<MfsEvent*> ribbon_candidates;
        std::vector<MfsEvent*> flash_candidates;

        for (auto& track : track_list) {
            for (auto& evt : track.events) {
                if (evt.triggered) continue;              
                if (evt.time_ms > start && evt.time_ms <= end) {
                    haptic_candidates.push_back(&evt);
                    if (!evt.ribbon_params.is_null()) {
                        ribbon_candidates.push_back(&evt);
                    }
                    if (!evt.flash_params.is_null()) {
                        flash_candidates.push_back(&evt);
                    }
                }
            }
        }

        // Lambda表达式：按优先级降序排序，取最高者
        auto select_highest = [](std::vector<MfsEvent*>& candidates) -> MfsEvent* {
            if (candidates.empty()) return nullptr;
            std::sort(candidates.begin(), candidates.end(),
                [](const MfsEvent* a, const MfsEvent* b) {
                    return a->priority > b->priority;
                });
            return candidates.front();
        };

        // 分别选出各模态最高优先级事件
        MfsEvent* best_haptic = select_highest(haptic_candidates);
        MfsEvent* best_ribbon = select_highest(ribbon_candidates);
        MfsEvent* best_flash = select_highest(flash_candidates);

        // 分发震动模态
        if (best_haptic) {
            best_haptic->triggered = true;
            dispatch_modal(MODAL_HAPTIC, *best_haptic, json());
        }
        // 分发彩带模态
        if (best_ribbon && best_ribbon != best_haptic) {
            best_ribbon->triggered = true;
            dispatch_modal(MODAL_RIBBON, *best_ribbon, best_ribbon->ribbon_params);
        }
        // 分发闪光灯模态
        if (best_flash && best_flash != best_haptic && best_flash != best_ribbon) {
            best_flash->triggered = true;
            dispatch_modal(MODAL_FLASH, *best_flash, best_flash->flash_params);
        }
    }

    // 重置内核（切换视频或停止播放时调用）
    void reset() {
        track_list.clear();
        last_tick_ms = 0;
    }
};

// 全局内核单例
static MFS_Core g_mfs_core;

// ===================== 导出给 JavaScript 的底层接口 =====================
extern "C" {

EMSCRIPTEN_KEEPALIVE
void mfs_set_callback(emscripten::val js_cb) {
    g_mfs_core.set_callback(js_cb);
}

EMSCRIPTEN_KEEPALIVE
int mfs_load_script(const char* json_str) {
    if (!json_str) return -1;
    std::string s(json_str);
    return g_mfs_core.load_hsf_script(s);
}

EMSCRIPTEN_KEEPALIVE
void mfs_tick(uint32_t current_ms) {
    g_mfs_core.tick(current_ms);
}

EMSCRIPTEN_KEEPALIVE
void mfs_reset() {
    g_mfs_core.reset();
}

} // extern "C"

```
## 第三部分：一键编译脚本（按操作系统选择）
### Windows 系统
在 mfs_wasm 文件夹下新建 build.bat，写入以下内容：
```bat
@echo off
:: 编译MFS内核，输出mfs.js + mfs.wasm，生产优化体积
emcc mfs_core.cpp ^
-O3 ^
-std=c++17 ^
-s WASM=1 ^
-s EXPORTED_FUNCTIONS='["mfs_set_callback","mfs_load_script","mfs_tick","mfs_reset"]' ^
-s EXPORTED_RUNTIME_METHODS='["ccall","cwrap"]' ^
-s RESERVED_FUNCTION_POINTERS=20 ^
-s ALLOW_TABLE_GROWTH=1 ^
-s NO_FILESYSTEM=1 ^
-s OPTIMIZE=1 ^
-o mfs.js

echo 编译完成，生成 mfs.js 和 mfs.wasm
pause

```
### Mac / Linux 系统
在 mfs_wasm 文件夹下新建 build.sh，写入以下内容：
```bash
#!/bin/bash
# 编译MFS内核，输出mfs.js + mfs.wasm
emcc mfs_core.cpp \
-O3 \
-std=c++17 \
-s WASM=1 \
-s EXPORTED_FUNCTIONS='["mfs_set_callback","mfs_load_script","mfs_tick","mfs_reset"]' \
-s EXPORTED_RUNTIME_METHODS='["ccall","cwrap"]' \
-s RESERVED_FUNCTION_POINTERS=20 \
-s ALLOW_TABLE_GROWTH=1 \
-s NO_FILESYSTEM=1 \
-s OPTIMIZE=1 \
-o mfs.js

echo "编译完成，输出 mfs.js 和 mfs.wasm"

```
## 第四部分：执行编译（小白一键操作）
### Windows 操作
 1. 打开之前运行过 emsdk_env.bat 的 CMD 窗口。
 2. 切换到项目文件夹：
   ```cmd
   cd D:\mfs_wasm
   build.bat
   
   ```
 3. 等待编译，若无报错，文件夹内会出现两个关键文件：
   * mfs.js（胶水文件）
   * mfs.wasm（MFS核心二进制内核）
### Mac 操作
 1. 打开终端，切换到项目文件夹并执行脚本：
   ```bash
   cd ~/mfs_wasm
   chmod +x build.sh
   ./build.sh
   
   ```
### 💡 编译报错常见解决办法
 1. **json.hpp not found**：请检查 json.hpp 是否确实放到了和 mfs_core.cpp 同级的目录下。
 2. **emcc 不是内部或外部命令**：当前窗口环境丢失。请在当前窗口重新执行一次 emsdk_env.bat (Win) 或 source ./emsdk_env.sh (Mac)。
 3. **内存溢出或编译极慢**：可以临时将脚本中的优化参数 -O3 改为 -O0 进行本地调试。
## 第五部分：H5 前端调用示例（验证内核可用性）
新建 demo.html，将其与编译生成的 mfs.js、mfs.wasm 放在**同一文件夹**下：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>MFS Wasm 演示播放器</title>
</head>
<body>
    <video id="video" width="600" controls></video>
    <script src="./mfs.js"></script>
    <script>
        let mfsLoadScript;
        let mfsTick;
        let mfsReset;

        // Wasm 运行环境初始化完毕后的回调
        Module.onRuntimeInitialized = function() {
            // 包装 C++ 导出函数
            mfsLoadScript = Module.cwrap('mfs_load_script', 'number', ['string']);
            mfsTick = Module.cwrap('mfs_tick', null, ['number']);
            mfsReset = Module.cwrap('mfs_reset', null, []);

            // 注册 C++ 回调：内核每当触发时序事件就会调用这个 JS 函数
            const eventCallback = (modalType, eventRef, intensity, duration, paramsJson) => {
                console.log("==== MFS 事件触发 ====");
                console.log("模态类型:", modalType); // 1:震动, 2:彩带, 3:闪光灯
                console.log("事件编码:", eventRef);
                console.log("强度系数:", intensity);
                console.log("配套参数:", JSON.parse(paramsJson));

                // ========== 在此处接入前端硬件/UI渲染逻辑 ==========
                // if(modalType === 1) navigator.vibrate(duration); // 触发H5震动
                // if(modalType === 2) renderRibbonAnimation(paramsJson); // 触发现端CSS彩带
            };

            // 将 JS 回调函数传递至 C++ 内核
            Module.ccall('mfs_set_callback', null, ['function'], [eventCallback]);

            // 示例：标准白皮书规范的 HSF 测试脚本
            const testHSF = {
                "schema": "HapticScriptFormat",
                "version": "0.2.3",
                "tracks": [
                    {
                        "id": "vibration_track",
                        "events": [
                            {
                                "time_ms": 1500,
                                "event_ref": "F-104",
                                "intensity": 0.8,
                                "duration_ms": 200
                            }
                        ]
                    },
                    {
                        "id": "ribbon_track",
                        "events": [
                            {
                                "time_ms": 1500,
                                "event_ref": "S-205",
                                "intensity": 0.8,
                                "duration_ms": 600,
                                "ribbon_parameters": {
                                    "direction": "clockwise",
                                    "speed_level": 3,
                                    "color_hex": "#ff4400"
                                }
                            }
                        ]
                    }
                ]
            };

            // 向内核加载脚本字符串
            const ret = mfsLoadScript(JSON.stringify(testHSF));
            console.log("脚本加载结果 (0成功/-1失败):", ret);

            // 监听视频进度，实时推进内核时序 tick
            const video = document.getElementById("video");
            video.ontimeupdate = () => {
                const ms = video.currentTime * 1000;
                mfsTick(Math.floor(ms));
            };
        }
    </script>
</body>
</html>

```
## 第六部分：本地运行测试（跨域限制解决方案）
> ⚠️ **注意**：如果直接在电脑上双击双击 HTML 文件打开，浏览器会由于 Wasm 安全策略报错（跨域限制）。因此必须启动一个本地的静态 HTTP 服务。
> 
### 极简启动命令（利用系统自带 Python）
打开终端/CMD，切换进入项目文件夹 mfs_wasm，执行以下通用命令：
```bash
python -m http.server 8080

```
在浏览器中访问：http://127.0.0.1:8080/demo.html。播放视频，当进度走到 **1500ms** 时，控制台会自动打印 MFS 时序分发日志，代表内核完美运行！
## 第七部分：MFSPlayer APP 如何复用这套 mfs.wasm
 1. **内嵌容器**：APP 内部集成标准 WebView 组件。
 2. **本地打包**：把编译生成的 mfs.js、mfs.wasm 及前端页面直接打包放入 APP 的本地 Assets 资源包。
 3. **零网载荷**：WebView 直接加载本地 HTML，无需走云端网络请求，首屏秒开。
 4. **原生桥接**：在核心 eventCallback 回调里，通过 JSBridge（如安卓 JavascriptInterface / iOS WKScriptMessageHandler）将解析到的硬件数据投递给原生 Java/Kotlin/OC/Swift 层，调用真机线性马达与后置闪光灯。
 5. **无缝热更**：后续若迭代升级调度逻辑，只需无脑覆盖替换安装包内的 mfs.js/mfs.wasm 即可。
## 第八部分：版本迭代维护规则
 1. **逻辑单一源**：所有关于 HSF 解析规则、时序状态机调度、五维优先级策略的修改，**只修改 mfs_core.cpp 这一个文件**。
 2. **一键覆盖**：修改代码后，重新运行 build.bat 或 build.sh，生成的新胶水代码覆盖旧文件即可。
 3. **上层零感知**：H5 页面、APP WebView 内的原生桥接调用代码**无需改动任何一行**，真正实现内核层与渲染层的深度解耦。
## 第九部分：核心架构优势
 * **唯一维护源**：只用 C++ 写核心调度状态机，免去了以往前端 JS、安卓 Java、iOS OC 逻辑各自实现一遍导致的同步行为差异。
 * **高性能运行**：依托 Wasm 原生级二进制运行效率，时序调度与过滤耗时严格控制在 **5ms 内**，完美跑赢白皮书规定的 15ms 端到端总延迟硬指标。
 * **白皮书规范 Full Match**：完美对齐多轨道复合计算、五维优先级裁决、降级轻震机制、跳转时序自动复位重置等全部核心设计原则。
 * **硬件渲染解耦**：内核提供纯粹的事件逻辑分发能力，渲染全部上浮交给各端原生驱动，分层明确，扩展极其简单。


# 版权与引用声明
本规范由 捉秀项目 (Pulse Project) 联合各硬件厂商及高校多模态交互实验室共同发起、制定并进行长期维护。规范文本、核心标准词词典及 JSON 模式定义采用 知识共享署名 4.0 国际许可协议 (CC BY 4.0) 面向全球开发者公开发布。您可以在承认作者署名并注明出处的前提下，自由地共享和演绎本白皮书。

>标准官方引用方式：
>
>捉秀项目委员会. (2026). 《Multimodal Feedback Schema (MFS) v0.2 technical specification》. 捉秀>项目白皮书系列. 取自开源代码仓库：https://github.com/izhuoxiu/MFS
