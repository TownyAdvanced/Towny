package com.palmergames.bukkit.towny.database.dbHandlers.sql.object;

import java.sql.ResultSet;

public interface SQLLoadHandler<T> {
	T load(ResultSet resultSet);
}
