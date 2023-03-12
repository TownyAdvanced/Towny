package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An object that represents an invitation.
 * 
 * @author Articdive
 */
public interface Invite {
	/**
	 * Gets the sender of the invitation
	 * 
	 * @return The {@link CommandSender} of the invitation, or null if the sender is a player and not online.
	 */
	@Nullable
	CommandSender getDirectSender();

	/**
	 * @return The sender's name, or 'CONSOLE' if the invitation was sent by console.
	 */
	@NotNull
	String getSenderName();

	/**
	 * @return The sender's uuid, or {@code null} if the invitation was sent by console.
	 */
	@Nullable
	UUID getSenderUUID();

	/**
	 * Gets the receiver of the invitation.
	 * 
	 * @return The {@link InviteReceiver} object receiving the invite.
	 */
	InviteReceiver getReceiver();

	/**
	 * Gets the sender of the invitation.
	 * 
	 * @return The {@link InviteSender} object that sent the invite.
	 */
	InviteSender getSender();

	/**
	 * Runs the accept code for the given invitation.
	 * 
	 * @throws TownyException Sends errors back up to be processed by the caller.
	 */
	void accept() throws TownyException;

	/**
	 * Runs the reject code for the given invitation.
	 * 
	 * @param fromSender Tells if invite was revoked (true) or declined (false).
	 */
	void decline(boolean fromSender);
}
