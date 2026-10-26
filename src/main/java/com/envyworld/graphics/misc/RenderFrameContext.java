package com.envyworld.graphics.misc;

import java.util.Deque;
import com.envyworld.graphics.util.GlRenderStateSnapshot;

public record RenderFrameContext(GlRenderStateSnapshot stateSnapshot, boolean shaderBound, int stencilMode, Deque<StencilFrameState> stencilStates) {
}
