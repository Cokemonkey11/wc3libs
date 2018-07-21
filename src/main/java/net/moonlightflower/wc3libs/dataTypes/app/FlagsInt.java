package net.moonlightflower.wc3libs.dataTypes.app;

import java.lang.reflect.InvocationTargetException;

import com.esotericsoftware.minlog.Log;
import net.moonlightflower.wc3libs.dataTypes.DataType;
import net.moonlightflower.wc3libs.dataTypes.DataTypeInfo;
import net.moonlightflower.wc3libs.misc.Flag;

import javax.annotation.Nonnull;

public abstract class FlagsInt extends DataType {
	public interface IsFlag {
		int getVal();
	}
	
	public static class Flag extends net.moonlightflower.wc3libs.misc.Flag {
		public Flag(@Nonnull String label, int pos) {
			super(label, pos);
		}
	}
	
	//public abstract Class<? extends Enum<Flags>> getEnum();
	
	public static Class<Enum> getEnum(Class<?> c) {
		try {
			return (Class<Enum>) c.getMethod("getEnumStatic").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			Log.error(e.getMessage(), e);
		}
		
		return null;
	}
	
	private int _val;
	
	public int toInt() {
		return _val;
	}
	
	public void setVal(int val) {
		_val = val;
	}
	
	public boolean containsFlag(Flag flag) {
		return ((_val & (1 << flag.getPos())) != 0);
	}
	
	public void setPos(int pos, boolean val) {
		if (val) {
			setVal(toInt() | (1 << pos));
		} else {
			setVal(toInt() & ~(1 << pos));
		}
	}
	
	public void setFlag(Flag flag, boolean val) {
		int pos = flag.getPos();

		setPos(pos, val);
	}
	
	protected FlagsInt() {
		
	}
	
	protected FlagsInt(int val) {
		_val = val;
	}
}
