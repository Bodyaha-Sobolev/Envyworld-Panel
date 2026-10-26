package com.envyworld.graphics.util;

import java.util.Deque;
import com.envyworld.graphics.misc.AlphaStack;
import com.envyworld.graphics.misc.ScissorStack;

record RenderFrameState(
   AlphaStack alphaStack,
   ScissorStack scissorStack,
   Deque<RenderContextState> contextStates,
   Deque<StencilState> stencilStates,
   int framebufferWidth,
   int framebufferHeight,
   int stencilMode,
   boolean layerFrame,
   float layerOriginX,
   float layerOriginY
) {
}
