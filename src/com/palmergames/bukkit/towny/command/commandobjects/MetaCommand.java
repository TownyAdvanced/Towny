package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.Metadatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.ChatTools;
import org.bukkit.entity.Player;

/**
 * @author Suneet Tipirneni (Siris)
 * A class that handles commands dealing with {@link Metadatable} conforming objects.
 */
public class MetaCommand {
	/**
	 * This handles the actions to be performed on the {@link Metadatable} object.
	 * @param player The sender of the command.
	 * @param split The arguments of the meta command.
	 * @param obj The {@link Metadatable} object to be operated on.
	 * @throws TownyException Thrown when there is an error, note that by default these error messages are 
	 * abstract so if a more specific error message is needed it should be thrown from the obj passed into
	 * this function.
	 */
	public static void handleMetaCommand(Player player, String[] split, Metadatable obj) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_META.getNode()))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split.length == 1) {
			if (obj.hasMeta()) {
				player.sendMessage(ChatTools.formatTitle("Custom Meta Data"));
				for (CustomDataField<?> field : obj.getMetadata().values()) {
					player.sendMessage(field.getKey() + " = " + field.getValue());
				}
			} else {
				TownyMessaging.sendErrorMsg(player, "This item has no registered metadata");
			}
			return;
		}

		if (split.length == 4) {
			String mdKey = split[2];
			String val = split[3];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)){
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return;
			} else if (split[1].equalsIgnoreCase("set")) {
				CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
				if (obj.hasMeta()) {
					CustomDataField<Object> cdf = obj.getMetadata().get(mdKey);
					
					if (cdf != null) {
						// Check if the given value is valid for this field.
						try {
							cdf.isValidType(val);
						} catch (InvalidMetadataTypeException e) {
							TownyMessaging.sendErrorMsg(player, e.getMessage());
							return;
						}

						// Change state
						cdf.setValue(val);

						// Let user know that it was successful.
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_key_x_was_successfully_updated_to_x"), mdKey, cdf.getValue()));

						return;
					}
				}

				TownyMessaging.sendErrorMsg(player, "There is not metadata on this item.");

			}
		} else if (split[1].equalsIgnoreCase("add")) {
			String mdKey = split[2];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return;
			}

			CustomDataField<Object> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
			
			if (obj.hasMeta() && obj.getMetadata().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_key_x_already_exists"), mdKey));
				return;
			}
			
			obj.addMetaData(md.newCopy());

			TownyMessaging.sendMsg(player, "Meta was added to item.");


		} else if (split[1].equalsIgnoreCase("remove")) {
			String mdKey = split[2];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return;
			}

			CustomDataField<Object> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

			if (obj.hasMeta()) {
				CustomDataField<Object> cdf = obj.getMetadata().get(mdKey);
				if (cdf != null) {
					obj.removeMetaData(cdf);
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_data_successfully_deleted"));
					return;
				}
			}

			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_key_cannot_be_deleted"));
		}
	}
}
