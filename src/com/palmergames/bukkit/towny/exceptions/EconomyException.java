package com.palmergames.bukkit.towny.exceptions;

/**
 * @deprecated since 0.96.7.12 - Towny no longer throws this Exception internally.
 */
@Deprecated
public class EconomyException extends Exception {

	private static final long serialVersionUID = 5273714478509976170L;
	public String error;

	/**
	 * @deprecated since 0.96.7.12 - Towny no longer throws this Exception internally.
	 */
	@Deprecated
	public EconomyException() {

		super();
		error = "unknown";
	}

	/**
	 * @deprecated since 0.96.7.12 - Towny no longer throws this Exception internally.
	 */
	@Deprecated
	public EconomyException(String error) {

		super(error);
		this.error = error;
	}

	public String getError() {

		return error;
	}
}
