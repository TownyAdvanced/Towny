package com.palmergames.bukkit.towny.conversation;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import net.ess3.api.events.UserTeleportHomeEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class SetupConversation extends TownyConversation {
	private int questionCount;
	private final List<Question> setupQuestions = buildQuestions();
	
	public SetupConversation(CommandSender sender) {
		questionCount = 0;
		new ConversationFactory(Towny.getPlugin())
			.withPrefix(this)
			.addConversationAbandonedListener(this)
			.withFirstPrompt(new SetupPrompt())
			.withEscapeSequence("q")
			.withTimeout(120)
			.withLocalEcho(true)
			.withModality(false)
			.buildConversation((Conversable) sender)
			.begin();
	}
	
	public class SetupPrompt extends StringPrompt {
		@NotNull
		@Override
		public String getPromptText(@NotNull ConversationContext conversationContext) {
			return setupQuestions.get(questionCount).message().forLocale(getSender(conversationContext));
		}

		@Nullable
		@Override
		public Prompt acceptInput(@NotNull ConversationContext context, @Nullable String string) {
			if ("back".equalsIgnoreCase(string)) {
				questionCount = Math.max(questionCount-1, 0);
				return new SetupPrompt();
			}
			
			Question currentQuestion = setupQuestions.get(questionCount);
			if (!currentQuestion.isValidResponse(string)) {
				TownyMessaging.sendErrorMsg(getSender(context), Translatable.of("msg_err_invalid_response"));
				return new SetupPrompt();
			}
			
			context.setSessionData(questionCount, string);
			
			++questionCount;
			findNextQuestion();
			if (questionCount >= setupQuestions.size()) {
				consumer.accept(context);
				return Prompt.END_OF_CONVERSATION;
			}
			
			return new SetupPrompt();
		}
	}
	
	private void findNextQuestion() {
		while (questionCount < setupQuestions.size()) {
			Question nextQuestion = setupQuestions.get(questionCount);
			if (nextQuestion.requiresEco() && !TownyEconomyHandler.isActive()) {
				++questionCount;
				continue;
			}
			break;
		}
	}
	
	private List<Question> buildQuestions() {
		Predicate<String> trueOrFalse = (str) -> switch (str.toLowerCase()) {
			case "y", "yes", "true", "n", "no", "false" -> true;
			default -> false;
		};

		Predicate<String> isInt = (str) -> {
			try {
				Integer.parseInt(str);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		};
		
		return Arrays.asList(
			new Question(Translatable.of("msg_setup_wildernessuse"), false, trueOrFalse),
			new Question(Translatable.of("msg_setup_revertunclaim"), false, trueOrFalse),
			new Question(Translatable.of("msg_setup_town_block_ratio"), false, isInt),
			new Question(Translatable.of("msg_setup_town_cost"), true, isInt),
			new Question(Translatable.of("msg_setup_nation_cost"), true, isInt),
			new Question(Translatable.of("msg_setup_townblock_cost"), true, isInt)
		);
	}
	
	private static class Question {
		
		private final Translatable message;
		private final boolean requiresEco;
		private final Predicate<String> predicate;
		
		public Question(Translatable message, boolean requiresEconomy, Predicate<String> predicate) {
			this.message = message;
			this.requiresEco = requiresEconomy;
			this.predicate = predicate;
		}
		
		public Translatable message() {
			return message;
		}
		
		public boolean requiresEco() {
			return requiresEco;
		}
		
		public boolean isValidResponse(String response) {
			if (response == null)
				return false;
			
			return predicate.test(response);
		}
	}
}
