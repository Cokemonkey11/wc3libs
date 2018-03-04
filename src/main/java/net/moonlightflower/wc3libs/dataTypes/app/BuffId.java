package net.moonlightflower.wc3libs.dataTypes.app;

import net.moonlightflower.wc3libs.misc.ObjId;

import javax.annotation.Nonnull;

public class BuffId extends ObjId {
	protected BuffId(String idString) {
		super(idString);
	}

	@Nonnull
	public static BuffId valueOf(ObjId id) {
		return new BuffId(id.toString());
	}
}
