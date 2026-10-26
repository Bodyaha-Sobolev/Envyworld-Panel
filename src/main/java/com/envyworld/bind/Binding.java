package com.envyworld.bind;

public class Binding {
	public String id;
	public BindTrigger trigger = BindTrigger.ATTACK_ENTITY;
	/** Item id filter, {@code any} for every item. */
	public String item = "any";
	public String command = "";
    /** Delay before each command line; missing values preserve immediate execution. */
    public java.util.List<Integer> commandDelaysMs = new java.util.ArrayList<>();
    public String author = "", description = "";
	/** Optional saved custom item preset id (config/envyworld/config.json -> customItems). */
	public String customItem = "";
	/** Custom model data that must match when {@link #customItem} is set. */
	public int customModelData = 0;
	public boolean enabled = true;
	/** Minimum delay between two runs of this bind for one player. */
	public int cooldownTicks = 0;
	/** For {@link BindTrigger#KEY}: run for every entity in this radius in front of the player. 0 = run once. */
	public int radius = 0;
    /** PRESS runs a batch, HOLD runs while V is held, TOGGLE switches a batch on/off. */
    public String keyMode = "PRESS";
    /** 0 means unlimited for HOLD/TOGGLE; PRESS always runs at least once. */
    public int repeatCount = 1;
    public int repeatDelayMs = 1000;
    /** -1 follows the global macro key; -2 and lower encode mouse buttons. */
    public int keyCode = -1;
    public int keyModifiers;
    public double lookDistance = 64;

	public Binding() {
	}

	public Binding(String id, BindTrigger trigger, String item, String command) {
		this.id = id;
		this.trigger = trigger;
		this.item = item == null || item.isBlank() ? "any" : item;
		this.command = command;
	}

	public boolean matchesTrigger(BindTrigger other) {
		return enabled && trigger == other && trigger != BindTrigger.ATTACK_BLOCK && trigger != BindTrigger.PROJECTILE_HIT;
	}
}
