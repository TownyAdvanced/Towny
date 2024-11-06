package com.palmergames.bukkit.towny.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Outpost;
import com.palmergames.bukkit.towny.object.Position;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.towny.utils.BorderUtil.FloodfillResult;

public class LegacyOutpostConversionTask extends TownyTimerTask {

	final Position pos;
	final Town town;

	public LegacyOutpostConversionTask(Towny plugin, Position pos, Town town) {
		super(plugin);
		this.pos = pos;
		this.town = town;
	}

	@Override
	public void run() {
		if (plugin.isError())
			return;

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(pos.asLocation());
		if (!town.hasTownBlock(townBlock)) {
			TownyMessaging.sendErrorMsg(String.format("%s tried to load an outpost located at %s, which is not a TownBlock owned by %s.", town.getName(), pos.toString(), town.getName()));
			return;
		}

		String outpostName = !townBlock.getName().isEmpty() ? townBlock.getName() : "UnnamedOutpost" + String.valueOf(town.getMaxOutpostSpawn());
		Outpost outpost = new Outpost(UUID.randomUUID(), outpostName);
		outpost.setSpawn(pos);
		outpost.addTownblock(townBlock);
		outpost.save();
		townBlock.setOutpostObject(outpost);
		townBlock.save();
		town.addOutpost(outpost);

		WorldCoord coord = townBlock.getWorldCoord();
		FloodfillResult result = null; 
		try {
			result = BorderUtil.getFloodFillableCoordsForOutpostConversion(town, coord);
			if (result.type() != BorderUtil.FloodfillResult.Type.SUCCESS)
				throw result.feedback() != null ? new TownyException(result.feedback()) : new TownyException();
			else if (result.feedback() != null)
				TownyMessaging.sendMsg(result.feedback());
		} catch (TownyException e) {
			TownyMessaging.sendMsg(e.getMessage());
			return;
		}

		List<WorldCoord> selection = new ArrayList<>(result.coords());
		for (WorldCoord wc : selection) {
			TownBlock tb = wc.getTownBlockOrNull();
			if (tb != null) {
				outpost.addTownblock(tb);
				tb.setOutpostObject(outpost);
				tb.save();
			}
		}

		TownyMessaging.sendMsg(String.format("%s imported a legacy outpost located at %s, total size: %s.", town.getName(), pos.toString(), selection.size()));
	}
}