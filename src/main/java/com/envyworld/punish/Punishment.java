package com.envyworld.punish;

import java.util.UUID;

public class Punishment {
	public String id;
	public UUID uuid;
	public String name;
	public PunishmentType type;
	public String reason = "";
	public String issuedBy = "console";
	/** Server name from the config, e.g. "ПвП". */
	public String server = "";
	public long issuedAt;
	/** 0 = permanent. */
	public long expiresAt;

	public Punishment() {
	}

	public Punishment(String id, UUID uuid, String name, PunishmentType type, String reason, String issuedBy, String server, long issuedAt, long expiresAt) {
		this.id = id;
		this.uuid = uuid;
		this.name = name;
		this.type = type;
		this.reason = reason;
		this.issuedBy = issuedBy;
		this.server = server;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	public boolean active() {
		return expiresAt <= 0L || expiresAt > System.currentTimeMillis();
	}

	public String cleanName() {
		return name == null ? "?" : name;
	}
}
