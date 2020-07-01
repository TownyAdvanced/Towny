package com.palmergames.bukkit.towny.database;

import com.palmergames.bukkit.towny.object.Identifiable;
import com.palmergames.bukkit.towny.object.Nameable;

import java.io.File;

/**
 * Marks objects are saved in the Towny database.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public interface Saveable extends Nameable, Identifiable, Changed {}
