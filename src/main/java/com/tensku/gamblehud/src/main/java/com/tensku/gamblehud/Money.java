package com.tensku.gamblehud;

import java.util.Locale;

public final class Money {
    private Money() {}

    /** Parses "5M", "1.5b", "250,000", "$3k". Returns -1 if invalid. */
    public static double parse(String s) {
        if (s == null) return -1;
        s = s.trim().replace("$", "").replace(",", "");
        if (s.isEmpty()) return -1;
        double mult = 1;
        char c = Character.toLowerCase(s.charAt(s.length() - 1));
        switch (c) {
            case 'k' -> mult = 1e3;
            case 'm' -> mult = 1e6;
            case 'b' -> mult = 1e9;
            case 't' -> mult = 1e12;
            default -> { }
        }
        if (mult != 1) s = s.substring(0, s.length() - 1);
        try {
            return Double.parseDouble(s) * mult;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String format(double v) {
        String sign = v < 0 ? "-" : "";
        double a = Math.abs(v);
        String out;
        if (a >= 1e12) out = trim(a / 1e12) + "T";
        else if (a >= 1e9) out = trim(a / 1e9) + "B";
        else if (a >= 1e6) out = trim(a / 1e6) + "M";
        else if (a >= 1e3) out = trim(a / 1e3) + "k";
        else out = trim(a);
        return sign + "$" + out;
    }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.2f", d);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
