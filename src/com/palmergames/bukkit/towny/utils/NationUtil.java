package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.ChatTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anonymoose on 26/05/2019.
 */
public class NationUtil {

    //This is a shared method used to remove a town from a nation
    public static void removeTownFromNation(Town town, Towny plugin) {

        try {
            Nation nation = town.getNation();
            nation.removeTown(town);

			/*
			 * Remove all resident titles/nationRanks before saving the town itself.
			 */
            List<Resident> titleRemove = new ArrayList<Resident>(town.getResidents());

            for (Resident res : titleRemove) {
                if (res.hasTitle() || res.hasSurname()) {
                    res.setTitle("");
                    res.setSurname("");
                }
                res.updatePermsForNationRemoval(); // Clears the nationRanks.
                TownyUniverse.getDataSource().saveResident(res);
            }
            TownyUniverse.getDataSource().saveNation(nation);
            TownyUniverse.getDataSource().saveNationList();

            plugin.resetCache();

            TownyMessaging.sendNationMessage(nation, ChatTools.color(String.format(TownySettings.getLangString("msg_nation_town_left"), town.getName())));
            TownyMessaging.sendTownMessage(town, ChatTools.color(String.format(TownySettings.getLangString("msg_town_left_nation"), nation.getName())));
        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(x.getMessage());
            return;
        } catch (EmptyNationException en) {
            TownyUniverse.getDataSource().removeNation(en.getNation());
            TownyUniverse.getDataSource().saveNationList();
            TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(TownySettings.getLangString("msg_del_nation"), en.getNation().getName())));
        } finally {
            TownyUniverse.getDataSource().saveTown(town);
        }
    }

    public static void addTownToNation(Town town, Nation nation, Towny plugin) {

    }
}
