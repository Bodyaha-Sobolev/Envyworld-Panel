# Полное дерево проекта

В дереве перечислены файлы исходников, ресурсов, документации и проверок. Локальные настройки, сборки, сохранения игры и архивы сюда не входят.

```text
Envyworld-Panel/
├── .github/
│   └── workflows/
│       └── build.yml
├── docs/
│   ├── images/
│   │   ├── item-editor.png
│   │   ├── items.png
│   │   ├── logs.png
│   │   ├── macros.png
│   │   ├── menu.png
│   │   ├── online.png
│   │   ├── profile.png
│   │   └── rules.png
│   ├── ARCHITECTURE.md
│   └── PROJECT_TREE.md
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── licenses/
│   ├── nametag-font-NOTICE.txt
│   └── renderer-NOTICE.txt
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── envyworld/
│       │           ├── bind/
│       │           │   ├── Binding.java
│       │           │   ├── BindRunner.java
│       │           │   └── BindTrigger.java
│       │           ├── client/
│       │           │   ├── gui/
│       │           │   │   ├── ChoiceScreen.java
│       │           │   │   ├── ColorPickerPopup.java
│       │           │   │   ├── DupeIpResult.java
│       │           │   │   ├── EnvyHud.java
│       │           │   │   ├── EnvyScreen.java
│       │           │   │   ├── HeldItemNames.java
│       │           │   │   ├── HudEditorScreen.java
│       │           │   │   ├── HudNotices.java
│       │           │   │   ├── HudSurface.java
│       │           │   │   ├── IpCheckPlan.java
│       │           │   │   ├── ItemEditorScreen.java
│       │           │   │   ├── ItemImportScreen.java
│       │           │   │   ├── ItemTextEditorScreen.java
│       │           │   │   ├── ItemTextForm.java
│       │           │   │   ├── KeyCaptureScreen.java
│       │           │   │   ├── KLauncherSkins.java
│       │           │   │   ├── LogDetailsScreen.java
│       │           │   │   ├── MacroDetailsScreen.java
│       │           │   │   ├── MenuScreen.java
│       │           │   │   ├── MenuTheme.java
│       │           │   │   ├── NameplateLayout.java
│       │           │   │   ├── NameplatePunishments.java
│       │           │   │   ├── OwnProfileScreen.java
│       │           │   │   ├── PlayerProfileView.java
│       │           │   │   ├── PresetExchangeScreen.java
│       │           │   │   ├── ProfileAccountLogs.java
│       │           │   │   ├── ProfileChecks.java
│       │           │   │   ├── PunishmentTypes.java
│       │           │   │   ├── ReasonChoiceScreen.java
│       │           │   │   ├── ScrollBars.java
│       │           │   │   ├── SessionChatScreen.java
│       │           │   │   ├── SettingsPanel.java
│       │           │   │   ├── SpectatorOverlay.java
│       │           │   │   ├── StyledTextBox.java
│       │           │   │   ├── TagFont.java
│       │           │   │   ├── TelegramScreen.java
│       │           │   │   ├── UiButton.java
│       │           │   │   ├── UiEditBox.java
│       │           │   │   ├── UiFont.java
│       │           │   │   ├── UiGraphics.java
│       │           │   │   ├── UiIcons.java
│       │           │   │   ├── UiMotion.java
│       │           │   │   ├── UiRenderer.java
│       │           │   │   ├── UiText.java
│       │           │   │   └── UiToggle.java
│       │           │   ├── BurstBudget.java
│       │           │   ├── ClientBinds.java
│       │           │   ├── ClientMacros.java
│       │           │   ├── ClientProtection.java
│       │           │   ├── ClipboardFiles.java
│       │           │   ├── EnvyWorldClient.java
│       │           │   ├── FreezeDetector.java
│       │           │   ├── MacroKeys.java
│       │           │   ├── MacroRepeatState.java
│       │           │   ├── MacroSequence.java
│       │           │   ├── MiniAppLifecycle.java
│       │           │   ├── PlayerBoxProjection.java
│       │           │   ├── PlayerIdentityFilter.java
│       │           │   ├── PlayerModelBounds.java
│       │           │   ├── RemoteGame.java
│       │           │   ├── RemoteMiniApp.java
│       │           │   ├── ScoreboardRequestGate.java
│       │           │   ├── ServerScoreboard.java
│       │           │   ├── SpectatorView.java
│       │           │   ├── TargetInspector.java
│       │           │   ├── TelegramGameChat.java
│       │           │   └── WorldProjection.java
│       │           ├── command/
│       │           │   └── RulesCommand.java
│       │           ├── config/
│       │           │   ├── ClientSettings.java
│       │           │   ├── ConfigIO.java
│       │           │   └── RulesConfig.java
│       │           ├── graphics/
│       │           │   ├── misc/
│       │           │   │   ├── AlphaStack.java
│       │           │   │   ├── BindableResource.java
│       │           │   │   ├── BuiltInThemes.java
│       │           │   │   ├── ClasspathResource.java
│       │           │   │   ├── DecodedImage.java
│       │           │   │   ├── FontRegistry.java
│       │           │   │   ├── FrameRateLimiter.java
│       │           │   │   ├── MsdfAtlas.java
│       │           │   │   ├── MsdfAtlasData.java
│       │           │   │   ├── MsdfBounds.java
│       │           │   │   ├── MsdfFont.java
│       │           │   │   ├── MsdfFontBuilder.java
│       │           │   │   ├── MsdfFontData.java
│       │           │   │   ├── MsdfGlyph.java
│       │           │   │   ├── MsdfGlyphData.java
│       │           │   │   ├── MsdfMetrics.java
│       │           │   │   ├── MsdfMetricsData.java
│       │           │   │   ├── NativeMemoryAccess.java
│       │           │   │   ├── NativeMemoryAllocator.java
│       │           │   │   ├── OpenGlScissorRegion.java
│       │           │   │   ├── OpenGlTexture.java
│       │           │   │   ├── PreparedLayer.java
│       │           │   │   ├── RenderFrameContext.java
│       │           │   │   ├── ResizableTexture.java
│       │           │   │   ├── ResourceData.java
│       │           │   │   ├── ResourceResolver.java
│       │           │   │   ├── ScissorStack.java
│       │           │   │   ├── ShaderIncludeProcessor.java
│       │           │   │   ├── ShaderUniformWriter.java
│       │           │   │   ├── StencilFrameState.java
│       │           │   │   ├── TextureHandle.java
│       │           │   │   ├── TextureResource.java
│       │           │   │   ├── TextureUnitAllocator.java
│       │           │   │   ├── TextureViewAccessor.java
│       │           │   │   ├── ThemePalette.java
│       │           │   │   ├── UnsafeMemoryAccess.java
│       │           │   │   └── UnsafeNativeMemoryAccess.java
│       │           │   ├── render/
│       │           │   │   ├── GaussianBlurRenderer.java
│       │           │   │   ├── GaussianBlurTarget.java
│       │           │   │   ├── GlShaderProgram.java
│       │           │   │   ├── GuiDrawMode.java
│       │           │   │   ├── ImportedGlTexture.java
│       │           │   │   ├── KawaseBlurRenderer.java
│       │           │   │   ├── LayerFramebuffer.java
│       │           │   │   ├── OpenGlTextureUnitAllocator.java
│       │           │   │   ├── RenderFrameClock.java
│       │           │   │   ├── ScissorRegion.java
│       │           │   │   ├── TextGradient.java
│       │           │   │   ├── TextureViewRegistry.java
│       │           │   │   └── VertexAttributeFormat.java
│       │           │   └── util/
│       │           │       ├── GlRenderStateSnapshot.java
│       │           │       ├── GuiDrawApi.java
│       │           │       ├── GuiRenderBackend.java
│       │           │       ├── GuiRenderDriver.java
│       │           │       ├── GuiVertexBuffer.java
│       │           │       ├── MsdfFontRenderer.java
│       │           │       ├── NativeBuffer.java
│       │           │       ├── RenderContextState.java
│       │           │       ├── RenderFrameState.java
│       │           │       ├── StencilState.java
│       │           │       └── UnsafeAccess.java
│       │           ├── items/
│       │           │   ├── CustomItemPreset.java
│       │           │   ├── CustomItems.java
│       │           │   ├── ItemOptions.java
│       │           │   ├── ItemText.java
│       │           │   └── PresetExchange.java
│       │           ├── logs/
│       │           │   ├── ChatFilter.java
│       │           │   ├── ChatPunishmentParser.java
│       │           │   ├── LogQueryCache.java
│       │           │   ├── NewLogTracker.java
│       │           │   ├── NotificationReconciler.java
│       │           │   ├── OnlineScanner.java
│       │           │   ├── OnlineWatch.java
│       │           │   ├── PunishLogClient.java
│       │           │   ├── PunishmentClock.java
│       │           │   ├── PunishNotify.java
│       │           │   ├── ServerModes.java
│       │           │   ├── SessionChat.java
│       │           │   ├── SessionState.java
│       │           │   ├── TelegramAlert.java
│       │           │   ├── TelegramAuth.java
│       │           │   ├── TelegramMultipart.java
│       │           │   ├── TelegramNotify.java
│       │           │   └── TelegramPanel.java
│       │           ├── mixin/
│       │           │   ├── BossHealthOverlayAccessor.java
│       │           │   ├── ClientPacketListenerMixin.java
│       │           │   ├── ClientProtectionMixin.java
│       │           │   ├── EditBoxAccessor.java
│       │           │   ├── GameModeKeysMixin.java
│       │           │   ├── GameModeSwitcherMixin.java
│       │           │   ├── HudCaptureMixin.java
│       │           │   ├── LevelProjectionMixin.java
│       │           │   ├── MenuRenderMixin.java
│       │           │   ├── MinecraftRemoteInvoker.java
│       │           │   ├── ParticleProtectionMixin.java
│       │           │   ├── PlayerModelBoundsMixin.java
│       │           │   ├── RemoteFramerateMixin.java
│       │           │   ├── RemoteKeyboardInputMixin.java
│       │           │   ├── RemotePointerMixin.java
│       │           │   ├── SkinManagerInvoker.java
│       │           │   ├── SoundProtectionMixin.java
│       │           │   ├── SpectatorGuiMixin.java
│       │           │   ├── SpectatorHandMixin.java
│       │           │   ├── SpectatorKeysMixin.java
│       │           │   └── SpectatorNamesMixin.java
│       │           ├── punish/
│       │           │   ├── AuditLog.java
│       │           │   ├── Punishment.java
│       │           │   ├── PunishmentService.java
│       │           │   ├── PunishmentStore.java
│       │           │   └── PunishmentType.java
│       │           ├── ranks/
│       │           │   ├── Rank.java
│       │           │   ├── RankCatalog.java
│       │           │   ├── RankCommand.java
│       │           │   └── RankDetector.java
│       │           ├── rules/
│       │           │   ├── RuleAction.java
│       │           │   ├── RuleBook.java
│       │           │   └── RulePreset.java
│       │           ├── util/
│       │           │   ├── Durations.java
│       │           │   ├── Msg.java
│       │           │   └── Perms.java
│       │           ├── EnvyEvents.java
│       │           └── EnvyWorld.java
│       └── resources/
│           ├── assets/
│           │   ├── envyworld/
│           │   │   ├── items/
│           │   │   │   ├── stick_amethyst.json
│           │   │   │   ├── stick_blood.json
│           │   │   │   ├── stick_emerald.json
│           │   │   │   ├── stick_ender.json
│           │   │   │   ├── stick_fire.json
│           │   │   │   ├── stick_gold.json
│           │   │   │   ├── stick_ice.json
│           │   │   │   ├── stick_netherite.json
│           │   │   │   └── stick_rainbow.json
│           │   │   ├── lang/
│           │   │   │   ├── en_us.json
│           │   │   │   └── ru_ru.json
│           │   │   ├── models/
│           │   │   │   └── item/
│           │   │   │       ├── stick_amethyst.json
│           │   │   │       ├── stick_blood.json
│           │   │   │       ├── stick_emerald.json
│           │   │   │       ├── stick_ender.json
│           │   │   │       ├── stick_fire.json
│           │   │   │       ├── stick_gold.json
│           │   │   │       ├── stick_ice.json
│           │   │   │       ├── stick_netherite.json
│           │   │   │       └── stick_rainbow.json
│           │   │   ├── nametags/
│           │   │   │   ├── font.json
│           │   │   │   ├── font.png
│           │   │   │   └── SOURCE-LICENSE.txt
│           │   │   ├── remote/
│           │   │   │   └── index.html
│           │   │   ├── renderer/
│           │   │   │   ├── fonts/
│           │   │   │   │   └── msdf/
│           │   │   │   │       ├── atlas/
│           │   │   │   │       │   ├── cosmetics_icons.png
│           │   │   │   │       │   ├── icons.png
│           │   │   │   │       │   ├── inter_bold.png
│           │   │   │   │       │   ├── inter_display_bold.png
│           │   │   │   │       │   ├── inter_extrabold.png
│           │   │   │   │       │   ├── inter_semibold.png
│           │   │   │   │       │   ├── Maison_Neue_Extended_Bold.png
│           │   │   │   │       │   └── sf_pro_display_medium.png
│           │   │   │   │       ├── cosmetics_icons.json
│           │   │   │   │       ├── icons.json
│           │   │   │   │       ├── inter_bold.json
│           │   │   │   │       ├── inter_display_bold.json
│           │   │   │   │       ├── inter_extrabold.json
│           │   │   │   │       ├── inter_semibold.json
│           │   │   │   │       ├── Maison_Neue_Extended_Bold.json
│           │   │   │   │       └── sf_pro_display_medium.json
│           │   │   │   └── shaders/
│           │   │   │       ├── common/
│           │   │   │       │   ├── defines.glsl
│           │   │   │       │   ├── helpers.glsl
│           │   │   │       │   ├── iface_dc.glsl
│           │   │   │       │   ├── iface_gl.glsl
│           │   │   │       │   └── modes.glsl
│           │   │   │       └── gl/
│           │   │   │           ├── core_gl.frag
│           │   │   │           ├── core_gl.vert
│           │   │   │           ├── fullscreen_quad.vert
│           │   │   │           ├── gauss_blur.frag
│           │   │   │           ├── kawase_down.frag
│           │   │   │           └── kawase_up.frag
│           │   │   ├── sounds/
│           │   │   │   └── punish_notify.ogg
│           │   │   ├── textures/
│           │   │   │   └── item/
│           │   │   │       ├── stick_amethyst.png
│           │   │   │       ├── stick_blood.png
│           │   │   │       ├── stick_emerald.png
│           │   │   │       ├── stick_ender.png
│           │   │   │       ├── stick_fire.png
│           │   │   │       ├── stick_gold.png
│           │   │   │       ├── stick_ice.png
│           │   │   │       ├── stick_netherite.png
│           │   │   │       └── stick_rainbow.png
│           │   │   ├── ui/
│           │   │   │   ├── svg/
│           │   │   │   │   ├── bold.svg
│           │   │   │   │   ├── check.svg
│           │   │   │   │   ├── chevron-down.svg
│           │   │   │   │   ├── chevron-right.svg
│           │   │   │   │   ├── chevron-up.svg
│           │   │   │   │   ├── circle-x.svg
│           │   │   │   │   ├── download.svg
│           │   │   │   │   ├── eraser.svg
│           │   │   │   │   ├── eye-off.svg
│           │   │   │   │   ├── eye.svg
│           │   │   │   │   ├── italic.svg
│           │   │   │   │   ├── keyboard.svg
│           │   │   │   │   ├── message-circle.svg
│           │   │   │   │   ├── minus.svg
│           │   │   │   │   ├── plus.svg
│           │   │   │   │   ├── refresh-cw.svg
│           │   │   │   │   ├── send.svg
│           │   │   │   │   ├── sparkles.svg
│           │   │   │   │   ├── strikethrough.svg
│           │   │   │   │   ├── terminal.svg
│           │   │   │   │   ├── trash.svg
│           │   │   │   │   ├── underline.svg
│           │   │   │   │   └── upload.svg
│           │   │   │   ├── icons.json
│           │   │   │   ├── icons.png
│           │   │   │   ├── LICENSE-LUCIDE.txt
│           │   │   │   └── mapping.json
│           │   │   ├── icon.png
│           │   │   └── sounds.json
│           │   └── minecraft/
│           │       └── items/
│           │           ├── bamboo.json
│           │           ├── blaze_rod.json
│           │           ├── bone.json
│           │           ├── breeze_rod.json
│           │           ├── carrot_on_a_stick.json
│           │           ├── debug_stick.json
│           │           ├── fishing_rod.json
│           │           ├── mace.json
│           │           ├── stick.json
│           │           ├── trident.json
│           │           └── warped_fungus_on_a_stick.json
│           ├── envyworld/
│           │   ├── items.json
│           │   ├── ranks.json
│           │   └── rules.json
│           ├── envyworld.mixins.json
│           └── fabric.mod.json
├── tools/
│   ├── com/
│   │   └── envyworld/
│   │       ├── client/
│   │       │   └── gui/
│   │       │       └── SkinLookupSmokeTest.java
│   │       ├── items/
│   │       │   └── CatalogItemSmoke.java
│   │       └── logs/
│   │           ├── BurstBudgetSmoke.java
│   │           ├── FreezeGuardSmoke.java
│   │           ├── LifecycleLayoutSmoke.java
│   │           ├── LogCacheSmokeTest.java
│   │           ├── ModerationOverlaySmoke.java
│   │           ├── NotificationReconcileSmoke.java
│   │           ├── PanelAndSequenceSmoke.java
│   │           ├── ProfileChecksSmoke.java
│   │           ├── ProjectionSmoke.java
│   │           ├── RenderContextSmoke.java
│   │           ├── ScoreboardRankSmoke.java
│   │           ├── TelegramAuthSmoke.java
│   │           └── UiBehaviorSmokeTest.java
│   ├── audit_release.py
│   ├── CleanUiProbe.java
│   ├── clipboard-smoke.gradle
│   ├── ClipboardFilesSmoke.java
│   ├── items-smoke.gradle
│   ├── render-smoke.gradle
│   ├── renderer-assets.sha256.json
│   ├── RendererSmokeTest.java
│   └── run-client-clean-ui.gradle
├── .gitattributes
├── .gitignore
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── README.md
├── settings.gradle
└── THIRD_PARTY_NOTICES.md
```

Всего файлов: 351.
