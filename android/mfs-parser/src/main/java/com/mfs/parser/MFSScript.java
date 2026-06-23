package com.mfs.parser;

import java.util.List;

/**
 * MFSScript 对应整个 HSF 文件（最外层的 JSON 对象）
 * 包含版本、元数据、以及所有轨道和事件
 */
public class MFSScript {

    private String schema;          // 固定 "HapticScriptFormat"
    private String version;         // 如 "0.2.2"
    private List<MFSEvent> allEvents; // 拍平后的所有事件列表（便于播放器直接使用）

    public MFSScript(String schema, String version, List<MFSEvent> allEvents) {
        this.schema = schema;
        this.version = version;
        this.allEvents = allEvents;
    }

    // ---- Getter ----
    public String getSchema() { return schema; }
    public String getVersion() { return version; }
    public List<MFSEvent> getAllEvents() { return allEvents; }

    // 辅助方法：获取总时长（取最后一个事件的结束时间）
    public int getTotalDuration() {
        if (allEvents == null || allEvents.isEmpty()) return 0;
        MFSEvent last = allEvents.get(allEvents.size() - 1);
        // 如果最后一个事件没有持续时间，默认加 50ms
        int dur = last.getDurationMs() > 0 ? last.getDurationMs() : 50;
        return last.getTimeMs() + dur;
    }
}
