package net.syrupstudios.syrupessentials.util;

public class SNBTFormatter {

    public static String formatString(String snbt) {
        String result = "";
        int indent = 0;
        boolean inQuote = false;
        boolean inTypedArray = false;
        char last = 0;
        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);
            if (c == '"' && last != '\\') inQuote = !inQuote;
            if (inQuote) {
                result += c;
            } else if (inTypedArray) {
                if (c == ',') result += ", ";
                else {
                    result += c;
                    if (c == ']') inTypedArray = false;
                }
            } else {
                switch (c) {
                    case '{' -> {
                        result += "{\n";
                        indent++;
                        for (int j = 0; j < indent; j++) result += "  ";
                    }
                    case '}' -> {
                        result += "\n";
                        indent--;
                        for (int j = 0; j < indent; j++) result += "  ";
                        result += "}";
                    }
                    case '[' -> {
                        if (i + 2 < snbt.length() && snbt.charAt(i + 2) == ';') {
                            result += "[";
                            inTypedArray = true;
                        } else {
                            result += "[\n";
                            indent++;
                            for (int j = 0; j < indent; j++) result += "  ";
                        }
                    }
                    case ']' -> {
                        result += "\n";
                        indent--;
                        for (int j = 0; j < indent; j++) result += "  ";
                        result += "]";
                    }
                    case ',' -> {
                        result += ",\n";
                        for (int j = 0; j < indent; j++) result += "  ";
                    }
                    case ':' -> result += ": ";
                    default -> result += c;
                }
            }
            last = c;
        }
        return result;
    }
}
