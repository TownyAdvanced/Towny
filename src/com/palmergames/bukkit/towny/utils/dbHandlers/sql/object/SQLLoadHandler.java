package com.palmergames.bukkit.towny.utils.dbHandlers.sql.object;

import java.sql.ResultSet;

public interface SQLLoadHandler<T> {
	T load(ResultSet resultSet);
}
