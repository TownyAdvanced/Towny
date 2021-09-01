package com.palmergames.bukkit.towny.conversation;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TownyConversation implements ConversationPrefix, ConversationAbandonedListener {
	
	public Consumer<Object> consumer;
	
	@NotNull
	@Override
	public String getPrefix(@NotNull ConversationContext context) {
		return Translatable.of("default_towny_prefix").forLocale(getPlayer(context));
	}
	
	public Player getPlayer(@NotNull ConversationContext context) {
		return (Player) context.getForWhom();
	}

	public void runOnResponse(Consumer<Object> consumer) {
		this.consumer = consumer;
	}

	@Override
	public void conversationAbandoned(@NotNull ConversationAbandonedEvent event) {
		if (!event.gracefulExit())
			TownyMessaging.sendMsg(getPlayer(event.getContext()), Translatable.of("msg_prompt_cancel"));
	}
}
