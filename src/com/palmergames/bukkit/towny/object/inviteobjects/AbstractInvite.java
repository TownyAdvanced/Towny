package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import org.bukkit.command.CommandSender;

import java.util.Objects;

abstract class AbstractInvite<S extends InviteSender, R extends InviteReceiver>
	implements Invite {

	private final CommandSender directSender;
	private final R receiver;
	private final S sender;

	AbstractInvite(CommandSender directSender, R receiver, S sender) {
		this.directSender = directSender;
		this.receiver = receiver;
		this.sender = sender;
	}
	
	@Override
	public CommandSender getDirectSender() {
		return directSender;
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
		if (!(o instanceof AbstractInvite)) return false;
		AbstractInvite<?, ?> that = (AbstractInvite<?, ?>) o;
		return Objects.equals(getDirectSender(), that.getDirectSender()) &&
			Objects.equals(getReceiver(), that.getReceiver()) &&
			Objects.equals(getSender(), that.getSender());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDirectSender(), getReceiver(), getSender());
	}
}
