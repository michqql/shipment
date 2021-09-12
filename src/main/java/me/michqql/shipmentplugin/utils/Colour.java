package me.michqql.shipmentplugin.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colour {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-zA-Z0-9]{6}", Pattern.CASE_INSENSITIVE);

    public static String format(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        while(matcher.find()) {
            String hex = input.substring(matcher.start(), matcher.end());
            input = input.replace(hex, ChatColor.of(hex) + "");
            matcher = HEX_PATTERN.matcher(input);
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> format(List<String> input) {
        List<String> formatted = new ArrayList<>();
        for(String s : input) {
            formatted.add(format(s));
        }
        return formatted;
    }
}
