package com.bettersettings.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling color codes and text formatting.
 * Supports both legacy (&) codes and hex colors (&#RRGGBB).
 */
public class ColorUtils {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    
    /**
     * Translates color codes from & format to § format.
     * Also handles hex colors (&#RRGGBB).
     * 
     * @param text Text with & color codes
     * @return Text with § color codes
     */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // First, handle hex colors
        text = translateHexColors(text);
        
        // Then translate & to §
        return text.replace('&', '§');
    }
    
    /**
     * Translates hex color codes (&#RRGGBB) to Minecraft format.
     * 
     * @param text Text with hex colors
     * @return Text with Minecraft hex format
     */
    private static String translateHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            
            for (char c : hexCode.toCharArray()) {
                replacement.append('§').append(c);
            }
            
            matcher.appendReplacement(buffer, replacement.toString());
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    /**
     * Converts text with & color codes to an Adventure Component.
     * 
     * @param text Text with & color codes
     * @return Adventure Component
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Handle hex colors first
        text = translateHexColors(text);
        
        // Use Adventure's serializer
        return SERIALIZER.deserialize(text);
    }
    
    /**
     * Converts a list of strings with & color codes to Adventure Components.
     * 
     * @param lines List of text lines with & color codes
     * @return List of Adventure Components
     */
    public static List<Component> toComponentList(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            components.add(toComponent(line));
        }
        return components;
    }
    
    /**
     * Strips all color codes from text.
     * 
     * @param text Text with color codes
     * @return Plain text without colors
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Remove hex colors
        text = HEX_PATTERN.matcher(text).replaceAll("");
        
        // Remove legacy colors
        return text.replaceAll("[&§][0-9a-fk-or]", "");
    }
}
