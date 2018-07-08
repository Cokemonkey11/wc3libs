package net.moonlightflower.wc3libs.slk.app.splats;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import net.moonlightflower.wc3libs.dataTypes.DataType;
import net.moonlightflower.wc3libs.dataTypes.DataTypeInfo;
import net.moonlightflower.wc3libs.dataTypes.app.Bool;
import net.moonlightflower.wc3libs.dataTypes.app.Wc3Int;
import net.moonlightflower.wc3libs.dataTypes.app.Model;
import net.moonlightflower.wc3libs.dataTypes.app.SpawnId;
import net.moonlightflower.wc3libs.misc.FieldId;
import net.moonlightflower.wc3libs.misc.ObjId;
import net.moonlightflower.wc3libs.slk.ObjSLK;
import net.moonlightflower.wc3libs.slk.RawSLK;
import net.moonlightflower.wc3libs.slk.SLK;
import net.moonlightflower.wc3libs.slk.SLKState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpawnSLK extends ObjSLK<SpawnSLK, SpawnId, SpawnSLK.Obj> {
	public final static File GAME_USE_PATH = new File("Splats\\SpawnData.slk");
	
	public static class States {
		public static class State<T extends DataType> extends ObjSLK.State<T> {
			public State(String idString, DataTypeInfo typeInfo, T defVal) {
				super(idString, typeInfo, defVal);
			}

			public State(String idString, DataTypeInfo typeInfo) {
				super(idString, typeInfo);
			}

			public State(String idString, Class<T> type) {
				super(idString, type);
			}

			public State(String idString, Class<T> type, T defVal) {
				super(idString, type, defVal);
			}
		}

		public static Collection<State> values() {
			return (Collection<State>) State.values(State.class);
		}

		public final static State<SpawnId> OBJ_ID = new State<>("Name", SpawnId.class);
		
		public final static State<Model> ART_MODEL = new State<>("Model", Model.class);
		
		public final static State<Bool> EDITOR_IN_BETA = new State<>("InBeta", Bool.class);
		public final static State<Wc3Int> EDITOR_VERSION = new State<>("version", Wc3Int.class);
	}
	
	public static class Obj extends SLK.Obj<SpawnId> {
		private final Map<State, DataType> _stateVals = new LinkedHashMap<>();

		@Override
		public Map<State, DataType> getStateVals() {
			return new LinkedHashMap<>(_stateVals);
		}

		@Override
		protected void on_set(@Nonnull FieldId fieldId, @Nullable DataType val) {
			State state = State.valueByField(States.State.class, fieldId);

			if (state != null) _stateVals.put(state, val);
		}

		@Override
		protected void on_remove(@Nonnull FieldId fieldId) {
			State state = State.valueByField(States.State.class, fieldId);

			if (state != null) _stateVals.remove(state);
		}

		@Override
		protected void on_clear() {
			_stateVals.clear();
		}

		public Model getModel() {
			return get(States.ART_MODEL);
		}
		
		public void setModel(Model val) {
			set(States.ART_MODEL, val);
		}

		public <T extends DataType> T get(States.State<T> state) {
			return (T) super.get(state);
		}
		
		public <T extends DataType> void set(States.State<T> state, T val) {
			super.set(state, val);
		}
		
		public <T extends DataType> void remove(States.State<T> state) {
			super.set(state, null);
		}
		
		private void read(SLK.Obj<? extends ObjId> slkObj) {
			merge(slkObj, true);
		}
		
		public Obj(SLK.Obj<? extends ObjId> slkObj) {
			super(SpawnId.valueOf(slkObj.getId()));
			
			read(slkObj);
		}
		
		public Obj(SpawnId id) {
			super(id);

			for (States.State state : States.values()) {
				set(state, state.getDefVal());
			}
		}

		@Override
		public void reduce() {
			// TODO Auto-generated method stub
			
		}
	}
	
	//private Map<SpawnId, Camera> _objs = new LinkedHashMap<>();
	
	@Nonnull
	@Override
	public Map<SpawnId, Obj> getObjs() {
		return _objs;
	}
	
	@Override
	public void addObj(@Nonnull Obj val) {
		_objs.put(val.getId(), val);
	}
	
	@Nonnull
	@Override
	public Obj addObj(@Nonnull SpawnId id) {
		if (_objs.containsKey(id)) return _objs.get(id);
		
		Obj obj = new Obj(id);
		
		addObj(obj);
		
		return obj;
	}
	
	@Override
	protected void read(@Nonnull SLK<?, ? extends ObjId, ? extends SLK.Obj<? extends ObjId>> slk) {
		for (Entry<? extends ObjId, ? extends SLK.Obj<? extends ObjId>> slkEntry : slk.getObjs().entrySet()) {
			ObjId id = slkEntry.getKey();
			SLK.Obj<? extends ObjId> slkObj = slkEntry.getValue();
			
			Obj obj = new Obj(slkObj);
			
			addObj(obj);
		}
	}
	
	@Override
	public void read(@Nonnull File file) throws IOException {
		read(new RawSLK(file));
	}
	
	public SpawnSLK() {
		addField(States.OBJ_ID);

		for (States.State state : States.values()) {
			addField(state);
		}
	}

	@Nonnull
	@Override
	public Obj createObj(@Nonnull ObjId id) {
		return new Obj(SpawnId.valueOf(id));
	}

	@Override
	public void merge(@Nonnull SpawnSLK other, boolean overwrite) {
		// TODO Auto-generated method stub
		
	}
}
