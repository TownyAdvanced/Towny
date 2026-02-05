package com.palmergames.bukkit.towny.event;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

public class TownyObjectFormattedNameEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final TownyObject object;
	private String prefix = null;
	private String postfix = null;

	private Component prefixComponent = null;
	private Component postfixComponent = null;

	@ApiStatus.Internal
	public TownyObjectFormattedNameEvent(TownyObject object, String prefix, String postfix) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.object = object;
		this.prefix = Objects.requireNonNull(prefix, "prefix may not be null");
		this.postfix = Objects.requireNonNull(postfix, "postfix may not be null");
	}

	@ApiStatus.Internal
	public TownyObjectFormattedNameEvent(TownyObject object, Component prefix, Component postfix) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.object = object;
		this.prefixComponent = Objects.requireNonNull(prefix, "prefix may not be null");
		this.postfixComponent = Objects.requireNonNull(postfix, "postfix may not be null");
	}

	public String getPrefix() {
		if (this.prefix == null) {
			this.prefix = TownyComponents.toLegacy(this.prefixComponent);
		}

		return prefix;
	}

	public void setPrefix(String prefix) {
		Preconditions.checkArgument(prefix != null, "prefix may not be null");

		this.prefix = prefix;
		this.prefixComponent = null;
	}

	public String getPostfix() {
		if (this.postfix == null) {
			this.postfix = TownyComponents.toLegacy(this.postfixComponent);
		}

		return postfix;
	}

	public void setPostfix(String postfix) {
		Preconditions.checkArgument(postfix != null, "postfix may not be null");

		this.postfix = postfix;
		this.postfixComponent = null;
	}

	public Component prefix() {
		if (this.prefixComponent == null) {
			this.prefixComponent = TownyComponents.miniMessage(this.prefix);
		}

		return this.prefixComponent;
	}
	
	public void prefix(final Component prefix) {
		Preconditions.checkArgument(prefix != null, "prefix may not be null");

		this.prefixComponent = prefix;
		this.prefix = null;
	}
	
	public Component postfix() {
		if (this.postfixComponent == null) {
			this.postfixComponent = TownyComponents.miniMessage(this.postfix);
		}

		return this.postfixComponent;
	}
	
	public void postfix(final Component postfix) {
		Preconditions.checkArgument(postfix != null, "postfix may not be null");

		this.postfixComponent = postfix;
		this.postfix = null;
	}

	public TownyObject getTownyObject() {
		return object;
	}

	public boolean isResident() {
		return object instanceof Resident;
	}

	public boolean isTown() {
		return object instanceof Town;
	}

	public boolean isNation() {
		return object instanceof Nation;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
