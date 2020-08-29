package com.palmergames.bukkit.towny.db;

import java.util.Collection;

public interface Data<T> {
	boolean save(T obj);
	boolean update(T obj);
	boolean delete(T obj);
	Collection<T> loadAll();
}
