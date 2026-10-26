package com.envyworld.client;

import com.envyworld.EnvyWorld;
import com.envyworld.bind.Binding;

import java.util.List;

/**
 * Client-side bind list, read directly from the local config file.
 *
 * <p>The mod is fully client-side: macros are stored in
 * {@code config/envyworld/config.json} and never synchronised with a server.</p>
 */
public final class ClientBinds {
	private ClientBinds() {
	}

	/** Kept for compatibility with the GUI; there is nothing to request from a server. */
	public static void request() {
	}

	public static List<Binding> all() {
		return EnvyWorld.config().sortedBinds().stream().filter(b->b.trigger!=com.envyworld.bind.BindTrigger.PROJECTILE_HIT&&b.trigger!=com.envyworld.bind.BindTrigger.ATTACK_BLOCK).toList();
	}
}
