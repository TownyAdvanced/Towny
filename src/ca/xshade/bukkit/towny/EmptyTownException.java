package ca.xshade.bukkit.towny;

import ca.xshade.bukkit.towny.object.Town;

public class EmptyTownException extends Exception {
	private static final long serialVersionUID = 5058583908170407803L;
	private EmptyNationException emptyNationException;
	private Town town;

	public EmptyTownException(Town town) {
		setTown(town);
	}

	public EmptyTownException(Town town,
			EmptyNationException emptyNationException) {
		setTown(town);
		setEmptyNationException(emptyNationException);
	}

	public boolean hasEmptyNationException() {
		return emptyNationException != null;
	}

	public EmptyNationException getEmptyNationException() {
		return emptyNationException;
	}

	public void setEmptyNationException(
			EmptyNationException emptyNationException) {
		this.emptyNationException = emptyNationException;
	}

	public void setTown(Town town) {
		this.town = town;
	}

	public Town getTown() {
		return town;
	}
}
