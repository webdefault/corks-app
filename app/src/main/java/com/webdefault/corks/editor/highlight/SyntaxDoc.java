package com.webdefault.corks.editor.highlight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by orlandoleite on 4/3/18.
 */

public class SyntaxDoc
{
    public String fileName;
    public String name;
    public String[] extensions;
    public String firstLine = null;
    
    public List<PatternDoc> patterns;
}
