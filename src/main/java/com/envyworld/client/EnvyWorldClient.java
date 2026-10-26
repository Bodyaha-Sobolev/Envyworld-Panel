package com.envyworld.client;

import com.envyworld.bind.BindTrigger;
import com.envyworld.client.gui.EnvyScreen;
import com.envyworld.items.CustomItems;
import com.envyworld.logs.OnlineScanner;
import com.envyworld.logs.PunishLogClient;
import com.envyworld.logs.PunishNotify;
import com.envyworld.ranks.RankDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint: the tabbed GUI and two keybinds.
 *
 * <ul>
 *     <li>{@code RSHIFT} — open the EnvyWorld menu.</li>
 *     <li>{@code V} — run the macro bound to the item in the main hand.</li>
 * </ul>
 *
 * <p>Everything runs locally. Commands are typed into chat, not sent to a server mod.</p>
 */
public class EnvyWorldClient implements ClientModInitializer {
	private static KeyMapping menuKey;
	private static KeyMapping macroKey;
    private static long nextLogPoll;
    public static String macroKeyName(){return macroKey==null?"V":macroKey.getTranslatedKeyMessage().getString();}

	@Override
	public void onInitializeClient() {
		menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.envyworld.menu", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
		macroKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.envyworld.macro", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
				com.mojang.blaze3d.platform.InputConstants.KEY_V, KeyMapping.Category.MISC));

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide() && player instanceof LocalPlayer local
					&& local == Minecraft.getInstance().player) {
				ClientMacros.trigger(BindTrigger.ATTACK_ENTITY, local, entity);
			}
			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClientSide() && player instanceof LocalPlayer local
					&& local == Minecraft.getInstance().player) {
				ClientMacros.trigger(BindTrigger.USE_ITEM, local, null);
			}
			return InteractionResult.PASS;
		});

		PunishNotify.registerSound();
        TargetInspector.register();
        ClientProtection.start();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(c->{com.envyworld.logs.SessionChat.flush();RemoteMiniApp.stop();ClientProtection.close();});
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(com.envyworld.client.gui.SpectatorOverlay::reset));
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {if(type.isAdvanced())CustomItems.appendTooltip(stack, lines);else CustomItems.normalTooltip(stack,lines);});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if(!overlay){com.envyworld.logs.SessionState.observe(message.getString());PunishNotify.observeChat(message);com.envyworld.logs.SessionChat.accept("Сервер",message.getString());}
			if (!overlay) com.envyworld.client.gui.ProfileChecks.observe(message);
			RankDetector.observe(message);
		});

        ClientReceiveMessageEvents.CHAT.register((message,signedMessage,sender,params,time)->com.envyworld.logs.SessionChat.accept("Чат",message.getString()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
			RankDetector.tick(client);
            ServerScoreboard.tick(client);
            var server=client.getCurrentServer();
            String host=server!=null?server.ip:client.hasSingleplayerServer()?"Одиночная игра":"Вне сервера";
            String mode=com.envyworld.ranks.RankDetector.detectedMode();
            boolean connected=client.player!=null&&client.getConnection()!=null;
            com.envyworld.logs.SessionChat.context(!connected?"Вне сервера":server==null?"Одиночная игра":host+" · "+(mode.isBlank()||mode.equals("—")?"Режим не определён":mode));
            com.envyworld.logs.SessionState.tick(client);
            com.envyworld.logs.TelegramNotify.tick();
            RemoteGame.tick();
            ClientProtection.heartbeat();
            TargetInspector.tick(client);
			CustomItems.refreshInventoryModels(client);
			OnlineScanner.tick(client);
            PunishNotify.tick(client);
            com.envyworld.logs.OnlineWatch.tick(client);
			com.envyworld.client.gui.ProfileChecks.tick(client);
			// Background poll so the "Логи" tab stays fresh even when not open.
            if (client.player == null || client.getConnection() == null) nextLogPoll = 0;
            else if (System.currentTimeMillis() >= nextLogPoll) {
                nextLogPoll = System.currentTimeMillis() + 10_000;
                PunishLogClient.tickAll("", "all");
            }

			while (menuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new EnvyScreen());
				}
			}

            boolean macroPressed = false;
            while (macroKey.consumeClick()) macroPressed = true;
            ClientMacros.tick(client, macroKey.isDown(), macroPressed);
		});
	}
}
