"""Check release contents before replacing the installed mod."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import json

root = Path(__file__).resolve().parents[1]
jar = root / 'build/libs/envyworld-1.0.0.jar'
with ZipFile(jar) as archive:
    names = archive.namelist()
    assert 'com/envyworld/client/gui/ColorPickerScreen.class' not in names, 'Fullscreen color screen must not return'
    assert 'com/envyworld/client/CompatProbe.class' not in names, 'Native compatibility probe in release'
    for required in ('com/envyworld/client/gui/ColorPickerPopup.class', 'com/envyworld/client/gui/NameplatePunishments.class', 'com/envyworld/client/gui/IpCheckPlan.class', 'com/envyworld/client/gui/UiToggle.class', 'com/envyworld/client/gui/UiMotion.class', 'com/envyworld/client/ClientProtection.class', 'com/envyworld/client/FreezeDetector.class', 'com/envyworld/client/BurstBudget.class', 'com/envyworld/mixin/SoundProtectionMixin.class', 'com/envyworld/mixin/ParticleProtectionMixin.class', 'com/envyworld/mixin/HudCaptureMixin.class'):
        assert required in names, required
    assert len(names) > 100, 'Incomplete JAR'
    assert not any(name.endswith('telegram-private.json') for name in names)
    for required in ('com/envyworld/client/RemoteMiniApp.class', 'com/envyworld/client/TelegramGameChat.class', 'com/envyworld/client/WorldProjection.class', 'com/envyworld/client/PlayerBoxProjection.class', 'com/envyworld/client/PlayerModelBounds.class', 'com/envyworld/client/PlayerIdentityFilter.class', 'com/envyworld/client/gui/HeldItemNames.class', 'com/envyworld/logs/TelegramAuth.class',
                     'com/envyworld/client/TargetInspector.class', 'com/envyworld/client/gui/SpectatorOverlay.class',
                     'com/envyworld/client/gui/ItemTextForm.class',
                     'assets/envyworld/nametags/font.json', 'assets/envyworld/nametags/font.png', 'assets/envyworld/remote/index.html', 'assets/envyworld/ui/icons.json',
                     'assets/envyworld/ui/icons.png', 'assets/envyworld/ui/LICENSE-LUCIDE.txt'):
        assert required in names, required
    for mixin in ('RemoteFramerateMixin','PlayerModelBoundsMixin','LevelProjectionMixin','GameModeKeysMixin','GameModeSwitcherMixin','SpectatorGuiMixin','SpectatorHandMixin','SpectatorKeysMixin','RemotePointerMixin','SpectatorNamesMixin'):
        assert mixin.encode() in archive.read('envyworld.mixins.json'), mixin
    assert not any(n.endswith('.exe') for n in names), 'Tunnel helper must be downloaded and verified on explicit start, not bundled'
    forbidden = ('FEATURE_PROBE', 'UI_EXPANSION_PROBE', 'UI_V2_PROBE', 'UI_V3_PROBE', 'UI_V4_PROBE', 'UI_V5_PROBE', 'HUD_UNICODE_PROBE', 'envyworld.renderProbe', 'AltOne, AltTwo', 'UI_TEST', 'probe-repeat', 'LookTarget', 'RadiusTarget', 'HitTarget', 'HookTarget', 'probe-exact-bow', 'NOT_A_REAL_TOKEN_ONLY_VISUAL_TEST')
    for name in names:
        if name.endswith('.class'):
            contents = archive.read(name)
            assert not any(marker.encode() in contents for marker in forbidden), f'Development probe in {name}'
    assert not any(name.startswith(('com/envyworld/render/', 'assets/envyworld/fonts/', 'assets/envyworld/font/', 'assets/envyworld/shaders/')) for name in names if not name.endswith('/')), 'Legacy renderer still packaged'
    for class_name in ('HudSurface', 'MenuTheme', 'PlayerProfileView', 'KLauncherSkins', 'ProfileChecks', 'PunishmentTypes',
                       'ItemEditorScreen', 'EnvyHud', 'HudEditorScreen', 'SettingsPanel', 'ScrollBars', 'UiIcons',
                       'ItemTextEditorScreen', 'StyledTextBox', 'ItemImportScreen', 'KeyCaptureScreen', 'ChoiceScreen', 'ReasonChoiceScreen', 'LogDetailsScreen'):
        assert f'com/envyworld/client/gui/{class_name}.class' in names
    assert 'com/envyworld/logs/PunishmentClock.class' in names
    assert 'com/envyworld/logs/NewLogTracker.class' in names
    assert 'com/envyworld/logs/OnlineWatch.class' in names
    assert 'com/envyworld/client/MacroRepeatState.class' in names
    assert 'com/envyworld/client/MacroKeys.class' in names
    assert 'com/envyworld/items/ItemOptions.class' in names
    for required in ('com/envyworld/items/PresetExchange.class','com/envyworld/client/gui/PresetExchangeScreen.class','com/envyworld/client/ClipboardFiles.class','com/envyworld/logs/TelegramNotify.class','com/envyworld/client/gui/TelegramScreen.class','com/envyworld/logs/NotificationReconciler.class','com/envyworld/logs/ChatPunishmentParser.class'):
        assert required in names, required
    assert 'com/envyworld/client/ProjectileMacros.class' not in names
    assert 'com/envyworld/items/BundledCollection.class' not in names
    assert 'com/envyworld/logs/PunishChatWatcher.class' not in names
    for required in ('com/envyworld/logs/SessionChat.class', 'com/envyworld/client/gui/SessionChatScreen.class', 'com/envyworld/client/gui/MacroDetailsScreen.class'):
        assert required in names, required
    assert not any(n.startswith('assets/envyworld/icons/') for n in names), 'Old raster icon replacements still packaged'
    for required in ('com/envyworld/logs/TelegramPanel.class', 'com/envyworld/logs/SessionState.class', 'com/envyworld/client/MacroSequence.class', 'com/envyworld/client/gui/OwnProfileScreen.class'):
        assert required in names, required
    sound = archive.read('assets/envyworld/sounds/punish_notify.ogg')
    assert hashlib.sha256(sound).hexdigest().upper() == 'A41F8DDC4735E4F6801FBA6665FEC18986628C6187FC6993D179E20346E3A296', 'Notification sound checksum mismatch'
    assert b'SkinManagerInvoker' in archive.read('envyworld.mixins.json')
    for required in ('com/envyworld/client/RemoteGame.class', 'com/envyworld/logs/TelegramAlert.class', 'com/envyworld/logs/TelegramMultipart.class'):
        assert required in names, required
    for mixin in ('BossHealthOverlayAccessor', 'MinecraftRemoteInvoker', 'RemoteKeyboardInputMixin'):
        assert mixin.encode() in archive.read('envyworld.mixins.json'), mixin
    for name in names:
        if name.endswith('.class'):
            assert not any(marker in archive.read(name) for marker in (b'UI_V8_PROBE',b'UI_V9_PROBE',b'V11_RENDER_PASS',b'V12_ANGLES_PASS',b'V13_ANGLES_PASS',b'V13_VISUAL_PASS',b'V15Probe',b'V15_NEUTRAL_PASS',b'V15_PROBE_PASS',b'V12AngleProbe',b'LOCAL_TEST_ONLY',b'ui-v9-item',b'ProbeTarget')), f'Native test probe in {name}'
    expected = json.loads((root / 'tools/renderer-assets.sha256.json').read_text(encoding='utf-8'))
    for name, digest in expected.items():
        assert name in names and hashlib.sha256(archive.read(name)).hexdigest() == digest, f'Renderer asset changed: {name}'
    count = len(expected)
    assert count == 27
    assert 'licenses/renderer-NOTICE.txt' in names
print(f'PASS: release audit; {count} renderer assets verified, development probes excluded.')
print(f'JAR: {jar.stat().st_size} bytes; SHA256 {hashlib.sha256(jar.read_bytes()).hexdigest()}')
