package com.example.douyinaweme;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class AwemeDumper {

    private static final String TAG = "DouyinAweme";

    public static void dump(Object aweme, String path) {
        Log.i(TAG, "Writing dump to " + path + " ...");
        try {
            Class<?> clazz = aweme.getClass();
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"class\": \"").append(escapeJson(clazz.getName())).append("\",\n");
            sb.append("  \"fields\": [\n");

            int totalFields = 0;
            Class<?> c = clazz;
            while (c != null && c != Object.class) {
                String className = c.getSimpleName();
                for (Field f : c.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        String val = readValue(f, aweme);
                        String javaType = f.getType().getSimpleName();
                        if (totalFields > 0) sb.append(",\n");
                        sb.append("    {\"class\": \"").append(escapeJson(className)).append("\", ");
                        sb.append("\"field\": \"").append(escapeJson(f.getName())).append("\", ");
                        sb.append("\"type\": \"").append(javaType).append("\", ");
                        sb.append("\"value\": ").append(jsonValWithType(val, javaType));
                        sb.append("}");
                        totalFields++;
                    } catch (Throwable ignored) {}
                }
                c = c.getSuperclass();
            }

            sb.append("\n  ],\n");
            sb.append("  \"totalFields\": ").append(totalFields).append("\n");
            sb.append("}\n");

            File file = new File(path);
            file.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(file);
            fw.write(sb.toString());
            fw.close();

            Log.i(TAG, "Dump complete: " + totalFields + " fields -> " + path);
        } catch (IOException e) {
            Log.e(TAG, "Dump write failed: " + e.getMessage());
        } catch (Throwable t) {
            Log.e(TAG, "Dump crashed: " + t.getClass().getName());
        }
    }

    private static String readValue(Field f, Object obj) {
        try {
            Object v = f.get(obj);
            if (v == null) return "null";
            if (v instanceof String) return "\"" + v + "\"";
            if (v instanceof Number || v instanceof Boolean) return v.toString();
            if (v instanceof List) return "List(size=" + ((List<?>) v).size() + ")";
            if (v instanceof Map) return "Map(size=" + ((Map<?, ?>) v).size() + ")";
            return v.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(v));
        } catch (Exception e) {
            return "<error>";
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String jsonValWithType(String val, String javaType) {
        if (val == null || val.equals("null")) return "null";
        if ("int".equals(javaType) || "long".equals(javaType) || "float".equals(javaType)
                || "double".equals(javaType) || "short".equals(javaType) || "byte".equals(javaType)) {
            return val;
        }
        if ("boolean".equals(javaType)) return val;
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            return "\"" + escapeJson(val.substring(1, val.length() - 1)) + "\"";
        }
        return "\"" + escapeJson(val) + "\"";
    }
}
