package bobba.mod.client.players;

/** Opacity carried on a player's render state from extraction to drawing; added by a mixin. */
public interface PlayerLookState {
	float bobbamod$modelAlpha();

	float bobbamod$armorAlpha();

	void bobbamod$setAlphas(float model, float armor);

	static float modelAlpha(Object state) {
		return state instanceof PlayerLookState look ? look.bobbamod$modelAlpha() : 1f;
	}

	static float armorAlpha(Object state) {
		return state instanceof PlayerLookState look ? look.bobbamod$armorAlpha() : 1f;
	}

	/** {@code argb} with its alpha multiplied by {@code alpha}. */
	static int fade(int argb, float alpha) {
		int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
		return (a << 24) | (argb & 0xFFFFFF);
	}
}
