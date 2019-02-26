package com.webdefault.corks.editor.tool;

import com.webdefault.corks.editor.Editor;
import com.webdefault.corks.editor.ToolsSelector;

/**
 * Created by orlandoleite on 3/6/18.
 */

public interface Tool<View>
{
    void init( Editor editor, ToolsSelector tools );
    boolean canClose();
}
