package kelvinspatola.mode.smartcode.ui;

import java.awt.Color;

public final class ColorTag {
    static public final ColorTag COLOR_1 = new ColorTag("//<bm_color1>//",
            SmartCodeTheme.getColor("bookmarks.linehighlight.color.1"));
    
    static public final ColorTag COLOR_2 = new ColorTag("//<bm_color2>//",
            SmartCodeTheme.getColor("bookmarks.linehighlight.color.2"));
    
    static public final ColorTag COLOR_3 = new ColorTag("//<bm_color3>//",
            SmartCodeTheme.getColor("bookmarks.linehighlight.color.3"));
    
    static public final ColorTag COLOR_4 = new ColorTag("//<bm_color4>//",
            SmartCodeTheme.getColor("bookmarks.linehighlight.color.4"));
    
    static public final ColorTag COLOR_5 = new ColorTag("//<bm_color5>//",
            SmartCodeTheme.getColor("bookmarks.linehighlight.color.5"));

    private final String tag;
    private Color color;

    private ColorTag(String tag, Color colour) {
        this.tag = tag;
        this.color = colour;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getTag() {
        return tag;
    }
    
    static public ColorTag[] values() {
        return new ColorTag[] {
            COLOR_1, COLOR_2, COLOR_3, COLOR_4, COLOR_5
        };
    }
    
    static public ColorTag valueOf(String tag) {
        for (ColorTag ct : values()) {
            if (ct.tag.equals(tag)) {
                return ct;
            }
        }
        return null;
    }
}
