package net.moonlightflower.wc3libs.misc;

import net.moonlightflower.wc3libs.dataTypes.app.Coords2DI;

import javax.annotation.Nonnull;

public class Size extends Coords2DI {
	public int getWidth() {
		return getX();
	}

	public int getHeight() {
		return getY();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Size)
			return equals((Size) other);
		
		return super.equals(other);
	}
	
	public boolean equals(Size other) {
		return getWidth() == other.getWidth() &&
				getHeight() == other.getHeight();
	}
	
	public int getArea() {
		return getWidth() * getHeight();
	}
	
	@Override
	public String toString() {
		return String.format("%dx%d", getWidth(), getHeight());
	}
	
	@Nonnull
    @Override
	public Size scale(double scale) {
		return new Size((int) (getX() * scale), (int) (getY() * scale));
	}
	
	public Size(int x, int y) {
		super(x, y);
	}
}
