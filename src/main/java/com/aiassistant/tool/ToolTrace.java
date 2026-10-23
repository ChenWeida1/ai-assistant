package com.aiassistant.tool;

import java.util.ArrayList;
import java.util.List;

public final class ToolTrace {

    private static final ThreadLocal<List<String>> TRACE = ThreadLocal.withInitial(ArrayList::new);

    private ToolTrace() {
    }

    public static void record(String name) {
        TRACE.get().add(name);
    }

    public static List<String> poll() {
        List<String> names = List.copyOf(TRACE.get());
        TRACE.remove();
        return names;
    }

    public static void clear() {
        TRACE.remove();
    }
}
