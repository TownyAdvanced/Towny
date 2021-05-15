package com.palmergames.bukkit.towny.war.flagwar;

import java.util.TimerTask;

/** @deprecated for removal in a future release. Please use <a href="https://github.com/TownyAdvanced/Flagwar">the FlagWar plugin</a> for continued support. */
@Deprecated
public class CellAttackThread extends TimerTask {

	CellUnderAttack cell;

	public CellAttackThread(CellUnderAttack cellUnderAttack) {

		this.cell = cellUnderAttack;
	}

	@Override
	public void run() {

		cell.changeFlag();
		if (cell.hasEnded())
			FlagWar.attackWon(cell);
	}
}