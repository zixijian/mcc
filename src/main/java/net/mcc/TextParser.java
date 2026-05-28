package net.mcc;

/**
 * 零链接 Text 解析器，支持 § 颜色代码
 */
public class TextParser {
    public static Object parse(String msg) {
        try {
            Class<?> textClass = MappingHelper.getClass("Text");
            Object root = null;

            // 策略 1: 尝试 literal("")
            try {
                root = MappingHelper.invokeStaticMethod(textClass, "literal", "");
            } catch (Exception ignored) {}

            // 策略 2: 尝试 empty()
            if (root == null) {
                try {
                    root = MappingHelper.invokeStaticMethod(textClass, "method_43470");
                } catch (Exception ignored) {}
            }

            if (root == null) {
                // 暴力搜索
                for (java.lang.reflect.Method m : textClass.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0 && m.getReturnType().equals(textClass)) {
                        root = m.invoke(null); break;
                    }
                }
            }

            if (root == null) return null;

            String[] parts = msg.split("§");
            Object currentStyle = MappingHelper.invokeMethod(root, "getStyle");

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (i == 0 && !msg.startsWith("§")) {
                    if (part.isEmpty()) continue;
                    Object span = createLiteral(part);
                    if (span != null) MappingHelper.invokeMethod(root, "append", span);
                    continue;
                }

                if (part.isEmpty()) continue;

                char code = Character.toLowerCase(part.charAt(0));
                String content = part.substring(1);

                currentStyle = updateStyle(currentStyle, code);
                if (!content.isEmpty()) {
                    Object span = createLiteral(content);
                    if (span != null) {
                        try {
                            // 必须确保 setStyle 调用成功
                            MappingHelper.invokeMethod(span, "setStyle", currentStyle);
                        } catch (Exception e) {
                            // 如果 method_10862 (setStyle) 找不到，尝试使用 method_27721 等替代方案 (虽然 Style 很少变)
                            try { MappingHelper.invokeMethod(span, "method_10862", currentStyle); } catch (Exception ignored) {}
                        }
                        MappingHelper.invokeMethod(root, "append", span);
                    }
                }
            }
            return root;
        } catch (Exception e) {
            System.out.println("[MCC] TextParser Error: " + e);
            return null;
        }
    }

    private static Object createLiteral(String text) {
        try {
            Class<?> textClass = MappingHelper.getClass("Text");
            return MappingHelper.invokeStaticMethod(textClass, "literal", text);
        } catch (Exception e) {
            try {
                Class<?> textClass = MappingHelper.getClass("Text");
                for (java.lang.reflect.Method m : textClass.getMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                        return m.invoke(null, text);
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Object updateStyle(Object style, char code) throws Exception {
        if (style == null || style instanceof java.util.Collection) {
            // 防御性编程：如果 style 是集合（可能是因为 getStyle 误识别为 getSiblings），尝试从默认样式重新开始
            try {
                style = MappingHelper.invokeStaticMethod(MappingHelper.getClass("Style"), "method_1030"); // Style.EMPTY
            } catch (Exception e) { return style; }
        }

        switch (code) {
            case '0': return withColor(style, 0x000000);
            case '1': return withColor(style, 0x0000AA);
            case '2': return withColor(style, 0x00AA00);
            case '3': return withColor(style, 0x00AAAA);
            case '4': return withColor(style, 0xAA0000);
            case '5': return withColor(style, 0xAA00AA);
            case '6': return withColor(style, 0xFFAA00);
            case '7': return withColor(style, 0xAAAAAA);
            case '8': return withColor(style, 0x555555);
            case '9': return withColor(style, 0x5555FF);
            case 'a': return withColor(style, 0x55FF55);
            case 'b': return withColor(style, 0x55FFFF);
            case 'c': return withColor(style, 0xFF5555);
            case 'd': return withColor(style, 0xFF55FF);
            case 'e': return withColor(style, 0xFFFF55);
            case 'f':
                style = MappingHelper.invokeMethod(style, "withBold", false);
                return withColor(style, 0xFFFFFF);
            case 'l': return MappingHelper.invokeMethod(style, "withBold", true);
            case 'r':
                style = MappingHelper.invokeMethod(style, "withBold", false);
                style = MappingHelper.invokeMethod(style, "withItalic", false);
                style = MappingHelper.invokeMethod(style, "withUnderline", false);
                return withColor(style, 0xFFFFFF);
        }
        return style;
    }

    private static Object withColor(Object style, int rgb) {
        try {
            // 尝试直接 invoke withColor(int)
            return MappingHelper.invokeMethod(style, "withColor", rgb);
        } catch (Exception e) {
            try {
                // 尝试 TextColor.fromRgb(rgb)
                Class<?> textColorClass = MappingHelper.getClass("net/minecraft/class_5251"); // TextColor
                Object textColor = MappingHelper.invokeStaticMethod(textColorClass, "method_27721", rgb); // fromRgb
                return MappingHelper.invokeMethod(style, "withColor", textColor);
            } catch (Exception e2) {
                return style;
            }
        }
    }
}
