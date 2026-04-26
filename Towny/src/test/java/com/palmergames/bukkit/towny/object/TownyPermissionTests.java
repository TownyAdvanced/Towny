package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyPermission.PermLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TownyPermissionTests {
	private TownyPermission permission;
	
	@BeforeEach
	void setup() {
		permission = new TownyPermission();
	}

	@Test
	void testSetAllToTrue() {
		permission.setAll(true);

		for (final PermLevel permLevel : PermLevel.values()) {
			for (final ActionType actionType : ActionType.values()) {
				assertTrue(permission.getPerm(permLevel, actionType), permLevel.name() + " and " + actionType.getCommonName());
			}
		}
	}

	@Test
	void testSetAllToFalse() {
		permission.setAll(false);

		for (final PermLevel permLevel : PermLevel.values()) {
			for (final ActionType actionType : ActionType.values()) {
				assertFalse(permission.getPerm(permLevel, actionType), permLevel.name() + " and " + actionType.getCommonName());
			}
		}
	}

	// test setting values to true

	@ParameterizedTest
	@MethodSource("combinedProvider")
	void testSetSpecificValueTrue(PermLevel permLevel, ActionType actionType) {
		permission.set(permLevel, actionType, true);

		for (final PermLevel level : PermLevel.values()) {
			for (final ActionType type : ActionType.values()) {
				assertEquals(level == permLevel && type == actionType, permission.getPerm(level, type), level.name() + " and " + type.getCommonName());
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = PermLevel.class)
	void testSetPermLevelRangeTrue(PermLevel permLevel) {
		permission.setPermLevel(permLevel, true);

		for (final PermLevel level : PermLevel.values()) {
			for (final ActionType type : ActionType.values()) {
				assertEquals(permLevel == level, permission.getPerm(level, type), level.name() + " and " + type.getCommonName());
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = ActionType.class)
	void testSetActionTypeRangeTrue(ActionType actionType) {
		permission.setActionType(actionType, true);

		for (final PermLevel level : PermLevel.values()) {
			for (final ActionType type : ActionType.values()) {
				assertEquals(actionType == type, permission.getPerm(level, type), level.name() + " and " + type.getCommonName());
			}
		}
	}

	// test setting values to false

	@ParameterizedTest
	@MethodSource("combinedProvider")
	void testSetSpecificValueFalse(PermLevel permLevel, ActionType actionType) {
		permission.setAll(true);
		permission.set(permLevel, actionType, false);

		for (final PermLevel level : PermLevel.values()) {
			for (final ActionType type : ActionType.values()) {
				assertEquals(level != permLevel || type != actionType, permission.getPerm(level, type), level.name() + " and " + type.getCommonName());
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = PermLevel.class)
	void testSetPermLevelRangeFalse(PermLevel permLevel) {
		permission.setAll(true);
		permission.setPermLevel(permLevel, false);

		for (final PermLevel level : PermLevel.values()) {
			for (final ActionType type : ActionType.values()) {
				assertEquals(permLevel != level, permission.getPerm(level, type), level.name() + " and " + type.getCommonName());
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = ActionType.class)
	void testSetActionTypeRangeFalse(ActionType actionType) {
		permission.setAll(true);
		permission.setActionType(actionType, false);

		for (final PermLevel level : PermLevel.values()) {
			for (final ActionType type : ActionType.values()) {
				assertEquals(actionType != type, permission.getPerm(level, type), level.name() + " and " + type.getCommonName());
			}
		}
	}

	static Stream<Arguments> combinedProvider() {
		return Arrays.stream(PermLevel.values()).flatMap(perm -> Arrays.stream(ActionType.values()).map(action -> Arguments.of(perm, action)));
	}
}
