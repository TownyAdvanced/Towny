package com.palmergames.bukkit.townywar;


public class CellAttackThread extends Thread {
	CellUnderAttack cell;
	boolean running = false;
	
	public CellAttackThread(CellUnderAttack cellUnderAttack) {
		this.cell = cellUnderAttack;
	}

	@Override
	public void run() {
		running = true;
		cell.drawFlag();
		while (running) {
			try {
				Thread.sleep(TownyWarConfig.getTimeBetweenFlagColorChange());
			} catch (InterruptedException e) {
				return;
			}
			if (running) {
				cell.changeFlag();
				if (cell.hasEnded()) {
					TownyWar.attackWon(cell);
				}
			}
		}
	}
	
	protected void setRunning(boolean running) {
		this.running = running;
	}
}
