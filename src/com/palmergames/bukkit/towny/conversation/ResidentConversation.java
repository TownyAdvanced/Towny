package com.palmergames.bukkit.towny.conversation;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A conversation that is used for getting resident input via chat.
 */
public class ResidentConversation extends TownyConversation {
	public ResidentConversation(Player player) {
		new ConversationFactory(Towny.getPlugin())
			.withPrefix(this)
			.addConversationAbandonedListener(this)
			.withFirstPrompt(new ResidentPrompt())
			.withEscapeSequence("q")
			.withTimeout(30)
			.withLocalEcho(false)
			.withModality(false)
			.buildConversation(player)
			.begin();
	}
	
	private class ResidentPrompt extends StringPrompt {
		public ResidentPrompt() {
			super();
		}

		@NotNull
		@Override
		public String getPromptText(@NotNull ConversationContext context) {
			return Translatable.of("msg_resident_prompt").forLocale(getPlayer(context));
		}

		@Nullable
		@Override
		public Prompt acceptInput(@NotNull ConversationContext context, @Nullable String string) {
			if (string != null) {
				Resident resident = TownyAPI.getInstance().getResident(string);

				if (resident == null)
					TownyMessaging.sendErrorMsg(getPlayer(context), Translatable.of("msg_err_not_registered_1", string));
				else if (consumer != null)
					consumer.accept(resident);
			}
			
			return Prompt.END_OF_CONVERSATION;
		}
	}
}
