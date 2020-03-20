package com.palmergames.bukkit.towny.utils.dbHandlers.sql.object;

import com.palmergames.bukkit.towny.object.Resident;

import java.sql.ResultSet;
import java.util.List;

public class SQLResidentHandler implements SQLDatabaseHandler<List<Resident>> {

	@Override
	public List<Resident> load(ResultSet resultSet) {
		return null;
	}

	@Override
	public SQLData<List<Resident>> save(List<Resident> object) {
		return null;
	}
	
}
