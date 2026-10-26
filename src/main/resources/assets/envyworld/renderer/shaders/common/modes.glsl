vec2 chamsPreviewUvAt(vec2 localUv, vec2 targetLocalUv) {
    vec2 spanX = dFdx(V_TEX) * MESH_SIZE.x;
    vec2 spanY = dFdy(V_TEX) * MESH_SIZE.y;
    return clamp(V_TEX + spanX * (targetLocalUv.x - localUv.x) + spanY * (targetLocalUv.y - localUv.y), vec2(0.001), vec2(0.999));
}

vec4 chamsPreviewScene(vec2 uv, vec2 localUv) {
    vec4 sampled = GET_TEX(clamp(uv, vec2(0.001), vec2(0.999)));
    vec3 lower = vec3(0.055, 0.075, 0.115);
    vec3 upper = vec3(0.72, 0.80, 0.92);
    vec3 environment = mix(lower, upper, clamp(1.0 - localUv.y, 0.0, 1.0));
    float horizon = exp(-pow((localUv.y - 0.48) * 7.0, 2.0));
    environment += vec3(0.22, 0.25, 0.30) * horizon;
    return vec4(mix(environment, sampled.rgb, sampled.a), 1.0);
}

vec3 chamsPreviewMaskNormal(vec2 uv, vec2 localUv) {
    vec2 stepSize = max(fwidth(V_TEX) * 1.5, vec2(1.0) / vec2(GET_TEX_SIZE(int(TEX_INDEX))));
    vec4 texL = GET_TEX(uv - vec2(stepSize.x, 0.0));
    vec4 texR = GET_TEX(uv + vec2(stepSize.x, 0.0));
    vec4 texD = GET_TEX(uv - vec2(0.0, stepSize.y));
    vec4 texU = GET_TEX(uv + vec2(0.0, stepSize.y));
    vec3 luminance = vec3(0.299, 0.587, 0.114);
    float hL = dot(texL.rgb, luminance) * 0.6 + texL.a * 0.4;
    float hR = dot(texR.rgb, luminance) * 0.6 + texR.a * 0.4;
    float hD = dot(texD.rgb, luminance) * 0.6 + texD.a * 0.4;
    float hU = dot(texU.rgb, luminance) * 0.6 + texU.a * 0.4;
    vec3 surfaceNormal = vec3(-(hR - hL) * 4.0, -(hU - hD) * 4.0, 1.0);
    vec3 bodyNormal = normalize(vec3((vec2(0.5) - localUv) * 1.5, 1.0));
    return normalize(mix(surfaceNormal, bodyNormal, 0.3));
}

vec2 chamsPreviewEnvironmentUv(vec3 reflectDir) {
    vec3 absDir = abs(reflectDir);
    float maxAxis = max(max(absDir.x, absDir.y), absDir.z);
    vec2 environmentUv;
    if (absDir.x == maxAxis) {
        environmentUv = reflectDir.x > 0.0
            ? vec2(-reflectDir.z, -reflectDir.y) / absDir.x
            : vec2(reflectDir.z, -reflectDir.y) / absDir.x;
    } else if (absDir.y == maxAxis) {
        environmentUv = reflectDir.y > 0.0
            ? vec2(reflectDir.x, reflectDir.z) / absDir.y
            : vec2(reflectDir.x, -reflectDir.z) / absDir.y;
    } else {
        environmentUv = reflectDir.z > 0.0
            ? vec2(reflectDir.x, -reflectDir.y) / absDir.z
            : vec2(-reflectDir.x, -reflectDir.y) / absDir.z;
    }
    environmentUv = environmentUv * 0.5 + 0.5;
    environmentUv.y = 1.0 - environmentUv.y;
    return clamp(environmentUv, vec2(0.0), vec2(1.0));
}

vec4 chamsPreviewGlass(vec2 uv, vec2 localUv, vec4 source, vec4 color) {
    vec3 normal = chamsPreviewMaskNormal(uv, localUv);
    vec3 viewDir = normalize(vec3(localUv * 2.0 - 1.0, -1.0));
    vec3 reflectDir = normalize(viewDir - 2.0 * dot(viewDir, normal) * normal);
    vec2 environmentLocal = chamsPreviewEnvironmentUv(reflectDir);
    vec4 environment = chamsPreviewScene(chamsPreviewUvAt(localUv, environmentLocal), environmentLocal);
    vec2 reflectedLocal = clamp(vec2(localUv.x, 1.0 - localUv.y) + reflectDir.xy * 0.03, vec2(0.0), vec2(1.0));
    vec4 blurredEnvironment = chamsPreviewScene(chamsPreviewUvAt(localUv, reflectedLocal), reflectedLocal) * 4.0;
    float totalWeight = 4.0;
    vec2 blurRadius = vec2(10.0) / max(MESH_SIZE, vec2(1.0));
    for (int direction = 0; direction < 4; direction++) {
        float angle = float(direction) * 1.57079632679;
        vec2 blurredLocal = clamp(reflectedLocal + vec2(cos(angle), sin(angle)) * blurRadius * 0.5, vec2(0.0), vec2(1.0));
        blurredEnvironment += chamsPreviewScene(chamsPreviewUvAt(localUv, blurredLocal), blurredLocal) * 1.5;
        totalWeight += 1.5;
    }
    blurredEnvironment /= totalWeight;
    float blurMix = smoothstep(0.3, 0.7, 10.0 / 12.0);
    vec4 reflectionColor = mix(environment, blurredEnvironment, blurMix);
    float fresnel = pow(1.0 - max(dot(viewDir, normal), 0.0), 2.0);
    float reflectionStrength = 0.75 + fresnel * 0.25;
    vec3 finalColor = mix(source.rgb * color.rgb, reflectionColor.rgb * color.rgb, reflectionStrength);
    float finalAlpha = color.a * source.a * (0.85 + fresnel * 0.15);
    return vec4(clamp(finalColor, vec3(0.0), vec3(1.0)), finalAlpha);
}

float chamsPreviewLiquidWaveHeight(vec2 localUv) {
    float wave1 = sin(localUv.x * 8.0) * cos(localUv.y * 6.0);
    float wave2 = sin(localUv.x * 12.0) * sin(localUv.y * 10.0);
    return (wave1 + wave2) * 2.0 * 0.06;
}

vec3 chamsPreviewLiquidNormal(vec2 uv, vec2 localUv) {
    const float epsilon = 0.005;
    float height = chamsPreviewLiquidWaveHeight(localUv);
    float heightX = chamsPreviewLiquidWaveHeight(localUv + vec2(epsilon, 0.0));
    float heightY = chamsPreviewLiquidWaveHeight(localUv + vec2(0.0, epsilon));
    vec3 waveNormal = normalize(vec3((height - heightX) / epsilon, (height - heightY) / epsilon, 1.0));

    vec4 source = GET_TEX(uv);
    float heightMap = dot(source.rgb, vec3(0.299, 0.587, 0.114));
    vec2 toCenter = vec2(0.5) - localUv;
    float radialFactor = length(toCenter);
    vec3 radialNormal = normalize(vec3(toCenter * 2.0, 1.0 - radialFactor));
    return normalize(mix(waveNormal, radialNormal, 0.6 + heightMap * 0.2));
}

vec3 chamsPreviewLiquidGlass(vec2 uv, vec2 localUv, vec4 source, vec3 tint) {
    vec3 normal = chamsPreviewLiquidNormal(uv, localUv);
    vec3 viewDir = normalize(vec3(localUv - 0.5, -1.0));

    vec3 refractDir = refract(viewDir, normal, 1.0 / 1.33);
    vec2 refractedLocal = clamp(localUv + refractDir.xy * 4.0 * 0.04, vec2(0.0), vec2(1.0));
    vec4 refractedColor = chamsPreviewScene(chamsPreviewUvAt(localUv, refractedLocal), refractedLocal);

    vec3 reflectDir = reflect(viewDir, normal);
    vec2 reflectedLocal = vec2(localUv.x, 1.0 - localUv.y) + reflectDir.xy * 0.08;
    float waveHeight = chamsPreviewLiquidWaveHeight(localUv);
    reflectedLocal += vec2(waveHeight * 0.015);
    reflectedLocal = clamp(reflectedLocal, vec2(0.0), vec2(1.0));

    vec4 reflectedColor = chamsPreviewScene(chamsPreviewUvAt(localUv, reflectedLocal), reflectedLocal) * 3.0;
    float reflectionWeight = 3.0;
    const float blurRadius = 4.0 * 0.008;
    for (int sampleIndex = 1; sampleIndex <= 2; sampleIndex++) {
        float angle = float(sampleIndex) * 3.14159265359;
        vec2 sampleLocal = clamp(reflectedLocal + vec2(cos(angle), sin(angle)) * blurRadius, vec2(0.0), vec2(1.0));
        reflectedColor += chamsPreviewScene(chamsPreviewUvAt(localUv, sampleLocal), sampleLocal);
        reflectionWeight += 1.0;
    }
    reflectedColor /= reflectionWeight;

    float cosTheta = max(dot(-viewDir, normal), 0.0);
    float fresnel = pow(1.0 - cosTheta, 3.0);
    vec3 glassColor = mix(refractedColor.rgb, reflectedColor.rgb, fresnel * 0.6);

    vec2 toCenter = vec2(0.5) - localUv;
    float depth = length(toCenter);
    float caustics = abs(sin(length(localUv * 8.0) * 2.0)) * 0.3;
    glassColor += caustics * 2.0 * 0.05;
    float edgeFactor = 1.0 - smoothstep(0.3, 0.5, depth);
    glassColor += edgeFactor * fresnel * 0.06;

    vec3 baseColor = source.rgb * tint;
    vec3 finalColor = mix(baseColor * 0.4, glassColor, 0.85);
    finalColor *= tint;
    finalColor *= 1.0 - depth * 0.2;
    return clamp(finalColor, vec3(0.0), vec3(1.0));
}

vec4 shade(vec2 fragUL) {
    if (DRAW_MODE == CHAMS_PREVIEW_MODE) {
        vec4 source = GET_TEX(V_TEX);
        if (source.a <= 0.001) discard;

        vec2 localUv = clamp((fragUL - MESH_POS) / max(MESH_SIZE, vec2(1.0)), vec2(0.0), vec2(1.0));
        int materialMode = int(THICK + 0.5);
        vec3 tint = COL4.rgb;
        float configuredAlpha = COL4.a;
        vec3 result;
        float resultAlpha;

        if (materialMode == 0) {
            result = tint;
            resultAlpha = configuredAlpha;
        } else if (materialMode == 1) {
            vec2 sampleStep = max(fwidth(V_TEX), vec2(1.0) / vec2(GET_TEX_SIZE(int(TEX_INDEX))));
            float neighborAlpha = min(
                min(GET_TEX(V_TEX + vec2(sampleStep.x, 0.0)).a, GET_TEX(V_TEX - vec2(sampleStep.x, 0.0)).a),
                min(GET_TEX(V_TEX + vec2(0.0, sampleStep.y)).a, GET_TEX(V_TEX - vec2(0.0, sampleStep.y)).a)
            );
            float edge = 1.0 - smoothstep(0.0, 0.75, neighborAlpha);
            result = mix(source.rgb, tint, configuredAlpha);
            result = mix(result, min(tint * 1.2, vec3(1.0)), edge);
            resultAlpha = 1.0;
        } else if (materialMode == 3) {
            float band = 0.5 + 0.5 * sin((localUv.y * 3.0 + localUv.x) * 6.2831853);
            float edgeLight = pow(abs(localUv.x - 0.5) * 2.0, 2.0);
            vec3 metal = mix(vec3(0.12), vec3(1.0), clamp(band * 0.7 + edgeLight * 0.6, 0.0, 1.0));
            result = mix(metal, metal * tint, 0.7);
            resultAlpha = max(configuredAlpha, 0.85);
        } else if (materialMode == 4) {
            result = chamsPreviewLiquidGlass(V_TEX, localUv, source, tint);
            vec3 liquidNormal = chamsPreviewLiquidNormal(V_TEX, localUv);
            vec3 liquidViewDir = normalize(vec3(localUv - 0.5, -1.0));
            float liquidFresnel = pow(1.0 - max(dot(-liquidViewDir, liquidNormal), 0.0), 3.0);
            resultAlpha = configuredAlpha * (0.85 + liquidFresnel * 0.15);
        } else {
            return chamsPreviewGlass(V_TEX, localUv, source, COL4);
        }

        return vec4(result, source.a * resultAlpha);
    }

    if (DRAW_MODE == COLOR_MODE) {
        vec2 meshPos = MESH_POS;
        vec2 meshSize = MESH_SIZE;
        vec2 center = fragUL - (meshPos + 0.5 * meshSize);
        float d = sdRoundedBox(center, 0.5 * meshSize, vec4(0.0));
        float cov;
        if (SOFT <= 0.0 && meshSize.x >= 1.0 && meshSize.y >= 1.0) {
            cov = d <= 0.0 ? 1.0 : 0.0;
        } else {
            float edge = SOFT + 1.0;
            cov = 1.0 - smoothstep(-1.0, edge, d);
        }
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(COL4.rgb, COL4.a * cov);
    }
    if (DRAW_MODE == TEXTURE_MODE) {
        vec4 t = GET_TEX(V_TEX);
        float cov = t.a;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        if (t.a == 0.0) discard;
        return t * COL4;
    }
    if (DRAW_MODE == LAYER_TEXTURE_MODE) {
        vec4 t = GET_TEX(V_TEX);
        float cov = t.a;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        if (t.a <= 0.001) discard;
        vec3 rgb = clamp(t.rgb / max(t.a, 1.0 / 255.0), 0.0, 1.0);
        return vec4(rgb * COL4.rgb, t.a * COL4.a);
    }
    if (DRAW_MODE == ROUNDED_LAYER_TEXTURE_MODE) {
        vec4 t = GET_TEX(V_TEX);
        vec2 c = fragUL - MESH_POS - MESH_SIZE * 0.5;
        vec4 radii = clampCornerRadii(RADIUS4, MESH_SIZE);
        float d = sdRoundedBox(c, MESH_SIZE * 0.5, radii);
        float roundMask = 1.0 - smoothstep(-1.0, 1.0, d);
        float cov = t.a * roundMask;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        if (cov <= 0.001) discard;
        vec3 rgb = clamp(t.rgb / max(t.a, 1.0 / 255.0), 0.0, 1.0);
        return vec4(rgb * COL4.rgb, cov * COL4.a);
    }
    if (DRAW_MODE == ROUNDED_RECTANGLE_MODE) {
        vec2 meshPos = MESH_POS;
        vec2 meshSize = MESH_SIZE;
        vec2 c = fragUL - meshPos - meshSize * 0.5;
        float inset = min(THICK, min(meshSize.x, meshSize.y) * 0.5);
        vec4 outlineRadii = clampCornerRadii(max(RADIUS4 - vec4(inset * 0.5), vec4(0.0)), meshSize);
        float outerD = sdRoundedBox(c, meshSize * 0.5, outlineRadii);
        float aaHalf = (THICK > 0.0) ? 1.0 : max(0.1, 1.0 + 0.5 * SOFT);
        float outerMask = 1.0 - smoothstep(-aaHalf, aaHalf, outerD);
        float cov = outerMask;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        if (THICK <= 0.0) {
            return vec4(COL4.rgb, COL4.a * outerMask);
        }

        vec2 innerSize = max(meshSize - vec2(inset * 2.0), vec2(0.0));
        float innerMask = 0.0;
        if (innerSize.x > 0.0 && innerSize.y > 0.0) {
            vec4 innerRadii = insetCornerRadii(outlineRadii, meshSize, inset);
            float innerD = sdRoundedBox(c, innerSize * 0.5, innerRadii);
            innerMask = 1.0 - smoothstep(-1.0, 1.0, innerD);
        }

        float outlineMask = clamp(outerMask - innerMask, 0.0, 1.0);
        float fillMask = innerMask;

        vec4 outlineLayer = vec4(OUTLINE4.rgb, OUTLINE4.a * outlineMask);
        vec4 fillLayer = vec4(COL4.rgb, COL4.a * fillMask);

        float outA = outlineLayer.a + fillLayer.a * (1.0 - outlineLayer.a);
        if (outA <= 0.001) {
            discard;
        }

        vec3 outRgb = (
        outlineLayer.rgb * outlineLayer.a +
        fillLayer.rgb * fillLayer.a * (1.0 - outlineLayer.a)
        ) / outA;
        return vec4(outRgb, outA);
    }
    if (DRAW_MODE == ROUNDED_TEXTURE_MODE) {
        vec4 t = GET_TEX(V_TEX) * COL4;
        vec2 c = fragUL - MESH_POS - MESH_SIZE * 0.5;
        vec4 radii = clampCornerRadii(RADIUS4, MESH_SIZE);
        float d = sdRoundedBox(c, MESH_SIZE * 0.5, radii);
        float roundMask = 1.0 - smoothstep(-1.0, 1.0, d);
        float cov = t.a * roundMask;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        if (cov <= 0.001) discard;
        return vec4(t.rgb, cov);
    }
    if (DRAW_MODE == MSDF_TEXT_MODE) {
        vec2 duvUv = fwidth(V_TEX);
        vec2 duv = duvUv * MSDF_ATLAS;
        float px = MSDF_RANGE * inversesqrt(dot(duv, duv));
        float e = MSDF_EDGE;

        float stemBias = max(0.0, (2.0 - px)) * 0.02;
        float effThick = THICK + stemBias;

        float alpha;
        float oa = 0.0;
        float sigForFade;

        if (px > 8.0) {
            vec3 m = GET_TEX(V_TEX).rgb;
            float sig = median(m.r, m.g, m.b) - 0.5 + effThick;
            alpha = smoothstep(-e, e, sig * px);
            if (SOFT > 0.0) oa = smoothstep(-e, e, (sig + SOFT) * px) - alpha;
            sigForFade = sig;
        } else {
            vec2 sx = duvUv * vec2(0.125, 0.375);
            vec2 sy = duvUv * vec2(-0.375, 0.125);
            vec3 m0 = GET_TEX(V_TEX + sx).rgb;
            vec3 m1 = GET_TEX(V_TEX - sx).rgb;
            vec3 m2 = GET_TEX(V_TEX + sy).rgb;
            vec3 m3 = GET_TEX(V_TEX - sy).rgb;
            float s0 = median(m0.r, m0.g, m0.b) - 0.5 + effThick;
            float s1 = median(m1.r, m1.g, m1.b) - 0.5 + effThick;
            float s2 = median(m2.r, m2.g, m2.b) - 0.5 + effThick;
            float s3 = median(m3.r, m3.g, m3.b) - 0.5 + effThick;
            float a0 = smoothstep(-e, e, s0 * px);
            float a1 = smoothstep(-e, e, s1 * px);
            float a2 = smoothstep(-e, e, s2 * px);
            float a3 = smoothstep(-e, e, s3 * px);
            alpha = (a0 + a1 + a2 + a3) * 0.25;
            if (SOFT > 0.0) {
                float o0 = smoothstep(-e, e, (s0 + SOFT) * px);
                float o1 = smoothstep(-e, e, (s1 + SOFT) * px);
                float o2 = smoothstep(-e, e, (s2 + SOFT) * px);
                float o3 = smoothstep(-e, e, (s3 + SOFT) * px);
                oa = (o0 + o1 + o2 + o3) * 0.25 - alpha;
            }
            sigForFade = (s0 + s1 + s2 + s3) * 0.25;
        }

        float safetyFade = smoothstep(-0.45, -0.15, sigForFade);
        alpha *= safetyFade;
        oa *= safetyFade;
        if (alpha + oa <= 0.004) discard;
        vec3 rgb = mix(OUTLINE4.rgb, COL4.rgb, alpha);
        float finalA = alpha * COL4.a + oa * COL4.a;
        float cov = finalA;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(rgb, finalA);
    }
    if (DRAW_MODE == BLURRED_ROUNDED_RECTANGLE_MODE) {
        int idx = int(TEX_INDEX);
        ivec2 ts = GET_TEX_SIZE(idx);
        vec2 texel = 1.0 / vec2(ts);
        vec2 uv = (gl_FragCoord.xy + MSDF_ATLAS) * texel;
        vec2 off1 = texel * vec2(0.75, 0.75);
        vec2 off2 = texel * vec2(-0.75, 0.75);
        vec3 cCenter = GET_TEX_AT(TEX_INDEX, uv).rgb;
        vec3 c0 = GET_TEX_AT(TEX_INDEX, uv - off1).rgb;
        vec3 c1 = GET_TEX_AT(TEX_INDEX, uv + off1).rgb;
        vec3 c2 = GET_TEX_AT(TEX_INDEX, uv - off2).rgb;
        vec3 c3 = GET_TEX_AT(TEX_INDEX, uv + off2).rgb;
        vec3 blurCol = cCenter * 0.36 + (c0 + c1 + c2 + c3) * 0.16;
        vec2 center = fragUL - MESH_POS - MESH_SIZE * 0.5;
        vec4 radii = clampCornerRadii(RADIUS4, MESH_SIZE);
        float d = sdRoundedBox(center, MESH_SIZE * 0.5, radii);
        float blurAaHalf = max(0.1, 1.0 + 0.5 * SOFT);
        float alpha = 1.0 - smoothstep(-blurAaHalf, blurAaHalf, d);
        float cov = alpha;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(blurCol, alpha);
    }
    if (DRAW_MODE == BACKDROP_STACK_MODE) {
        int idx = int(TEX_INDEX);
        ivec2 ts = GET_TEX_SIZE(idx);
        vec2 texel = 1.0 / vec2(ts);
        vec2 uv = (gl_FragCoord.xy + MSDF_ATLAS) * texel;
        vec2 off1 = texel * vec2(0.75, 0.75);
        vec2 off2 = texel * vec2(-0.75, 0.75);
        vec3 cCenter = GET_TEX_AT(TEX_INDEX, uv).rgb;
        vec3 bc0 = GET_TEX_AT(TEX_INDEX, uv - off1).rgb;
        vec3 bc1 = GET_TEX_AT(TEX_INDEX, uv + off1).rgb;
        vec3 bc2 = GET_TEX_AT(TEX_INDEX, uv - off2).rgb;
        vec3 bc3 = GET_TEX_AT(TEX_INDEX, uv + off2).rgb;
        vec3 blurCol = cCenter * 0.36 + (bc0 + bc1 + bc2 + bc3) * 0.16;
        vec2 bsCenter = fragUL - MESH_POS - MESH_SIZE * 0.5;
        vec4 bsRadii = clampCornerRadii(RADIUS4, MESH_SIZE);
        float bsD = sdRoundedBox(bsCenter, MESH_SIZE * 0.5, bsRadii);
        float bsAaHalf = max(0.1, 1.0 + 0.5 * SOFT);
        float bsCov = 1.0 - smoothstep(-bsAaHalf, bsAaHalf, bsD);
        if (STENCIL_MODE == 1) { if (bsCov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        vec3 afterOverlay = mix(blurCol, OUTLINE4.rgb, OUTLINE4.a);
        vec3 composed = mix(afterOverlay, COL4.rgb, COL4.a);
        return vec4(composed, bsCov);
    }
    if (DRAW_MODE == BLURRED_RECTANGLE_MODE) {
        int idx = int(TEX_INDEX);
        ivec2 ts = GET_TEX_SIZE(idx);
        vec2 texel = 1.0 / vec2(ts);
        vec2 uv = (gl_FragCoord.xy + MSDF_ATLAS) * texel;
        vec2 off1 = texel * vec2(0.75, 0.75);
        vec2 off2 = texel * vec2(-0.75, 0.75);
        vec3 cCenter = GET_TEX_AT(TEX_INDEX, uv).rgb;
        vec3 c0 = GET_TEX_AT(TEX_INDEX, uv - off1).rgb;
        vec3 c1 = GET_TEX_AT(TEX_INDEX, uv + off1).rgb;
        vec3 c2 = GET_TEX_AT(TEX_INDEX, uv - off2).rgb;
        vec3 c3 = GET_TEX_AT(TEX_INDEX, uv + off2).rgb;
        vec3 blurCol = cCenter * 0.36 + (c0 + c1 + c2 + c3) * 0.16;
        float cov = 1.0;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(blurCol, 1.0);
    }
    if (DRAW_MODE == SHADOW_MODE) {
        vec2 center = fragUL - MESH_POS - MESH_SIZE * 0.5;
        float d = sdRoundedBox(center, MESH_SIZE * 0.5, vec4(0.0));
        float soft = max(SOFT, 0.0001);
        float normalizedDistance = d / soft;
        float alpha = exp(-(normalizedDistance * normalizedDistance) * 2.0);
        float cov = alpha;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(COL4.rgb, COL4.a * alpha);
    }
    if (DRAW_MODE == ROUNDED_SHADOW_MODE) {
        vec2 center = fragUL - MESH_POS - MESH_SIZE * 0.5;
        vec4 radii = clampCornerRadii(RADIUS4, MESH_SIZE);
        float d = sdRoundedBox(center, MESH_SIZE * 0.5, radii);
        float soft = max(SOFT, 0.0001);
        float normalizedDistance = d / soft;
        float alpha = exp(-(normalizedDistance * normalizedDistance) * 2.0);
        float cov = alpha;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(COL4.rgb, COL4.a * alpha);
    }
    if (DRAW_MODE == GRADIENT_ROUNDED_RECTANGLE_MODE) {
        vec2 meshPos = MESH_POS;
        vec2 meshSize = MESH_SIZE;
        vec2 st = (fragUL - meshPos) / meshSize;
        float NOISE_AMPLITUDE = 0.5 / 255.0;
        vec4 grad = COL4;
        grad.rgb += mix(NOISE_AMPLITUDE, -NOISE_AMPLITUDE, noise(st));
        float effThick = max(THICK, 1.0);
        vec2 center = fragUL - meshPos - meshSize * 0.5;
        vec2 halfSize = meshSize * 0.5 - effThick;
        vec4 radii = insetCornerRadii(clampCornerRadii(max(RADIUS4 - vec4(effThick * 0.5), vec4(0.0)), meshSize), meshSize, effThick);
        float d = sdRoundedBox(center, halfSize, radii);
        float halfEffThick = effThick * 0.5;
        float centerDist = abs(d - halfEffThick);
        float alphaMask = clamp(halfEffThick + 0.5 - centerDist, 0.0, 1.0);
        float cov = alphaMask;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(grad.rgb, grad.a * alphaMask);
    }
    if (DRAW_MODE == CIRCLE_MODE) {
        vec2 center = MESH_POS + 0.5 * MESH_SIZE;
        vec2 p = fragUL - center;
        float halfTh = THICK * 0.5;
        float s = max(SOFT, 0.0);
        float R = max(0.5 * min(MESH_SIZE.x, MESH_SIZE.y) - halfTh - s, 0.0);
        float sdf = length(p) - R;
        float outer = 1.0 - smoothstep(halfTh - s, halfTh + s, sdf);
        float inner =  smoothstep(-halfTh - s, -halfTh + s, sdf);
        float ring = outer * inner;
        float ang = atan(p.y, p.x);
        if (ang < 0.0) ang += 6.28318530718;
        bool inArc = (RADIUS4.x < RADIUS4.y) ? (ang >= RADIUS4.x && ang <= RADIUS4.y) : (ang >= RADIUS4.x || ang <= RADIUS4.y);
        if (!inArc) ring = 0.0;
        float cov = ring;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return vec4(COL4.rgb, COL4.a * ring);
    }
    if (DRAW_MODE == TRIANGLE_MODE) {
        float cov = 1.0;
        if (STENCIL_MODE == 1) { if (cov <= 0.001) discard; return vec4(0, 0, 0, 1); }
        return COL4;
    }
    if (DRAW_MODE == SHIMMER_HIGHLIGHT_MODE) {
        vec2 meshPos = MESH_POS;
        vec2 meshSize = MESH_SIZE;

        vec2 cc = fragUL - meshPos - meshSize * 0.5;
        vec4 sRadii = clampCornerRadii(RADIUS4, meshSize);
        float sd = sdRoundedBox(cc, meshSize * 0.5, sRadii);
        float sAa = max(0.75, 1.0 + 0.5 * SOFT);
        float sMask = 1.0 - smoothstep(-sAa, sAa, sd);
        if (STENCIL_MODE == 1) { if (sMask <= 0.001) discard; return vec4(0.0, 0.0, 0.0, 1.0); }
        if (sMask <= 0.0015) discard;

        float prog = clamp(THICK, 0.0, 1.0);
        float env = smoothstep(0.0, 0.10, prog) * (1.0 - smoothstep(0.72, 1.0, prog));

        vec2 luv = (fragUL - meshPos) / meshSize;

        vec2 sdir = normalize(vec2(1.0, 0.6));
        float proj = dot(luv - 0.5, sdir);
        float span = 0.62;
        float bandW = 0.10;
        float sweepPos = mix(-span - 0.2, span + 0.2, smoothstep(0.0, 1.0, prog));
        float bd = (proj - sweepPos) / bandW;
        float band = exp(-bd * bd);
        float glow = exp(-bd * bd * 0.16);

        float border = (1.0 - smoothstep(0.0, 2.0, abs(sd + 1.6))) * (0.28 + 0.16 * sin(TIME * 2.2));

        vec3 col = clamp(COL4.rgb * 1.6, 0.0, 1.0);
        float intensity = (band * 0.6 + glow * 0.35 + border) * env * COL4.a;
        float sAlpha = clamp(intensity, 0.0, 1.0) * sMask;
        if (sAlpha <= 0.002) discard;
        return vec4(col, sAlpha);
    }
    if (DRAW_MODE == RING_SECTOR_MODE) {
        vec2 center = MESH_POS + 0.5 * MESH_SIZE;
        vec2 p = fragUL - center;
        float startA = RADIUS4.x;
        float endA = RADIUS4.y;
        float innerR = RADIUS4.z;
        float outerR = max(RADIUS4.w, 0.001);
        float pixelScale = max(0.5 * MESH_SIZE.x / outerR, 0.0001);
        float dist = length(p) / pixelScale;
        float aa = 0.5 / pixelScale;
        float th = max(THICK, 0.0) / pixelScale;

        float mid = 0.5 * (startA + endA);
        float halfSpan = 0.5 * (endA - startA);
        float ang = atan(p.y, p.x);
        float da = ang - mid;
        da = da - 6.28318530718 * floor((da + 3.14159265359) / 6.28318530718);
        float adiff = abs(da);

        float rMid = 0.5 * (innerR + outerR);
        float rHalf = 0.5 * (outerR - innerR);
        float dRadial = abs(dist - rMid) - rHalf;
        float dAngular = (adiff - halfSpan) * dist;
        float sectorSdf = max(dRadial, dAngular);

        float fillMask = 1.0 - smoothstep(-aa, aa, sectorSdf);
        float outlineMask = clamp(smoothstep(-th - aa, -th + aa, sectorSdf) - smoothstep(-aa, aa, sectorSdf), 0.0, 1.0);

        vec4 fillLayer = vec4(COL4.rgb, COL4.a * fillMask);
        vec4 outlineLayer = vec4(OUTLINE4.rgb, OUTLINE4.a * outlineMask);
        float outA = outlineLayer.a + fillLayer.a * (1.0 - outlineLayer.a);
        if (STENCIL_MODE == 1) { if (outA <= 0.001) discard; return vec4(0, 0, 0, 1); }
        if (outA <= 0.001) discard;
        vec3 outRgb = (outlineLayer.rgb * outlineLayer.a + fillLayer.rgb * fillLayer.a * (1.0 - outlineLayer.a)) / outA;
        return vec4(outRgb, outA);
    }
    return vec4(0.0);
}
