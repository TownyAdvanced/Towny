package ca.xshade.bukkit.towny;


public class NotRegisteredException extends TownyException {
	private static final long serialVersionUID = 175945283391669005L;

	public NotRegisteredException() {
		super();
		error = "Not registered.";
	}

	public NotRegisteredException(String error) {
		super(error);
		this.error = error;
	}
}
