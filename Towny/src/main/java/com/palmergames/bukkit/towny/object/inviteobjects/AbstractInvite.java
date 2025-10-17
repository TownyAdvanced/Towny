package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

abstract class AbstractInvite<S extends InviteSender, R extends InviteReceiver> implements Invite {
	private final UUID senderUUID;
	private final String senderName;
	private final R receiver;
	private final S sender;

	AbstractInvite(@NotNull CommandSender directSender, @NotNull R receiver, @NotNull S sender) {
		this(directSender.getName(), directSender instanceof Player player ? player.getUniqueId() : null, receiver, sender);
	}
	
	AbstractInvite(@NotNull String senderName, @Nullable UUID senderUUID, @NotNull R receiver, @NotNull S sender) {
		this.senderName = senderName;
		this.senderUUID = senderUUID;
		this.receiver = receiver;
		this.sender = sender;
	}

	@Nullable
	@Override
	public CommandSender getDirectSender() {
		return this.senderUUID == null ? Bukkit.getConsoleSender() : Bukkit.getPlayer(this.senderUUID);
	}

	@NotNull
	@Override
	public String getSenderName() {
		return senderName;
	}

	@Nullable
	@Override
	public UUID getSenderUUID() {
		return senderUUID;
	}

	@Override
	public R getReceiver() {
		return receiver;
	}

	@Override
	public S getSender() {
		return sender;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbstractInvite<?, ?> that)) return false;

		return Objects.equals(getSenderName(), that.getSenderName()) &&
			Objects.equals(getSenderUUID(), that.getSenderUUID()) &&
			Objects.equals(getReceiver(), that.getReceiver()) &&
			Objects.equals(getSender(), that.getSender());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSenderUUID(), getReceiver(), getSender());
	}
}
