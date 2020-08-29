package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.object.Town;

import java.util.Collection;

public class TownData implements Data<Town> {
	@Override
	public boolean save(Town obj) {
		return false;
	}

	@Override
	public boolean update(Town obj) {
		return false;
	}

	@Override
	public boolean delete(Town obj) {
		return false;
	}

	@Override
	public Collection<Town> loadAll() {
		return null;
	}
}
