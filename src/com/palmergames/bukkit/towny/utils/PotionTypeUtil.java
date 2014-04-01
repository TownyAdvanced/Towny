package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.potion.PotionType;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.util.JavaUtil;

public class PotionTypeUtil {

	public static boolean isInstanceOfAny(List<Class<?>> classesOfPotionTypes, Object obj) {

		for (Class<?> c : classesOfPotionTypes)
			if (c.isInstance(obj))
				return true;
		return false;
	}

	public static List<Class<?>> parsePotionTypeClassNames(List<String> potionClassNames, String errorPrefix) {

		List<Class<?>> potionTypeClasses = new ArrayList<Class<?>>();
		for (String potionClassName : potionClassNames) {
			if (potionClassName.isEmpty())
				continue;

			try {
				Class<?> c = Class.forName("org.bukkit.potion.PotionType." + potionClassName);
				if (JavaUtil.isSubInterface(PotionType.class, c))
					potionTypeClasses.add(c);
				else
					throw new Exception();
			} catch (ClassNotFoundException e) {
				TownyMessaging.sendErrorMsg(String.format("%s%s is not an acceptable class.", errorPrefix, potionClassName));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(String.format("%s%s is not an acceptable potion type.", errorPrefix, potionClassName));
			}
		}
		return potionTypeClasses;
	}
}