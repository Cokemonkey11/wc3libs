package net.moonlightflower.wc3libs.app;

import com.esotericsoftware.minlog.Log;
import net.moonlightflower.wc3libs.antlr.JassLexer;
import net.moonlightflower.wc3libs.bin.ObjMod;
import net.moonlightflower.wc3libs.bin.ObjMod.ObjPack;
import net.moonlightflower.wc3libs.bin.Wc3BinOutputStream;
import net.moonlightflower.wc3libs.bin.app.DOO;
import net.moonlightflower.wc3libs.bin.app.objMod.*;
import net.moonlightflower.wc3libs.dataTypes.DataList;
import net.moonlightflower.wc3libs.dataTypes.DataType;
import net.moonlightflower.wc3libs.dataTypes.DataTypeInfo;
import net.moonlightflower.wc3libs.dataTypes.app.War3String;
import net.moonlightflower.wc3libs.misc.*;
import net.moonlightflower.wc3libs.misc.Math;
import net.moonlightflower.wc3libs.port.JMpqPort;
import net.moonlightflower.wc3libs.port.MpqPort;
import net.moonlightflower.wc3libs.port.Orient;
import net.moonlightflower.wc3libs.slk.RawMetaSLK;
import net.moonlightflower.wc3libs.slk.SLK;
import net.moonlightflower.wc3libs.slk.SLKState;
import net.moonlightflower.wc3libs.slk.app.doodads.DoodSLK;
import net.moonlightflower.wc3libs.slk.app.meta.*;
import net.moonlightflower.wc3libs.slk.app.objs.*;
import net.moonlightflower.wc3libs.txt.Jass;
import net.moonlightflower.wc3libs.txt.Profile;
import net.moonlightflower.wc3libs.txt.TXTSectionId;
import net.moonlightflower.wc3libs.txt.WTS;
import net.moonlightflower.wc3libs.txt.app.profile.CampaignUnitStrings;
import org.antlr.v4.runtime.Token;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.Predicate;

public class ObjMerger {
    private final Collection<SLK> _inSlks = new LinkedHashSet<>();
    private final Map<File, SLK> _outSlks = new LinkedHashMap<>();

    private void addSlk(@Nonnull File inFile, @Nonnull SLK otherSlk) {
        _inSlks.add(otherSlk);

        SLK slk = _outSlks.computeIfAbsent(inFile, SLK::createFromInFile);

        slk.merge(otherSlk);
    }

    private final RawMetaSLK _metaSlk = new RawMetaSLK();

    private void addMetaSlk(@Nonnull RawMetaSLK slk) {
        _metaSlk.merge(slk);
    }

    private final Collection<Profile> _inProfiles = new LinkedHashSet<>();
    private final Profile _outProfile = new Profile();

    private void addProfile(@Nonnull Profile otherProfile) {
        _inProfiles.add(otherProfile);

        _outProfile.merge(otherProfile);
    }

    private final Collection<ObjMod> _inObjMods = new LinkedHashSet<>();
    private final Map<File, ObjMod> _outObjMods = new LinkedHashMap<>();

    private void addObjMod(@Nonnull File inFile, @Nonnull ObjMod otherObjMod) throws Exception {
        _inObjMods.add(otherObjMod);

        ObjPack pack = otherObjMod.reduce(_metaSlk, Collections.singletonList(W3D.class));

        Map<ObjId, ObjId> baseObjIds = pack.getBaseObjIds();

        Set<ObjId> objIds = new HashSet<>();

        for (Map.Entry<File, SLK> slkEntry : pack.getSlks().entrySet()) {
            File file = slkEntry.getKey();
            SLK otherSlk = slkEntry.getValue();

            Map<ObjId, SLK.Obj> otherObjs = new LinkedHashMap<>(((Map<ObjId, SLK.Obj>) otherSlk.getObjs()));

            otherSlk.clearObjs();

            for (Map.Entry<ObjId, SLK.Obj> objEntry : otherObjs.entrySet()) {
                ObjId objId = objEntry.getKey();

                objIds.add(objId);

                SLK.Obj otherObj = objEntry.getValue();

                SLK.Obj obj = otherSlk.addObj(objId);

                obj.clear();

                ObjId baseId = baseObjIds.get(objId);

                if (baseId != null) {
                    SLK slk = _outSlks.get(file);

                    if (slk != null) {
                        SLK.Obj baseObj = slk.getObj(baseId);

                        if (baseObj == null) throw new IllegalStateException("obj " + objId + " is based on " + baseId + " which does not exist");

                        obj.merge(baseObj);
                    }
                }

                obj.merge(otherObj);
            }

            addSlk(file, otherSlk);
        }

        for (File baseSLKFile : otherObjMod.getSLKs()) {
            SLK newSLK = SLK.createFromInFile(baseSLKFile);

            for (ObjId objId : objIds) {
                SLK reducedSLK = pack.getSlks().get(baseSLKFile);

                if ((reducedSLK != null) && reducedSLK.containsObj(objId)) continue;

                ObjId baseId = baseObjIds.get(objId);

                if (baseId != null) {
                    SLK.Obj baseObj = _outSlks.get(baseSLKFile).getObj(baseId);

                    if (baseObj != null) {
                        SLK.Obj obj = newSLK.addObj(objId);

                        obj.merge(baseObj);
                    }
                }
            }

            addSlk(baseSLKFile, newSLK);
        }

        //
        Profile otherProfile = pack.getProfile();

        Map<TXTSectionId, Profile.Obj> otherObjs = new LinkedHashMap<>(otherProfile.getObjs());

        otherProfile.clearObjs();

        for (Map.Entry<TXTSectionId, Profile.Obj> objEntry : otherObjs.entrySet()) {
            ObjId objId = ObjId.valueOf(objEntry.getKey().toString());
            Profile.Obj otherObj = objEntry.getValue();

            Profile.Obj obj = otherProfile.addObj(TXTSectionId.valueOf(objId.toString()));

            ObjId baseId = baseObjIds.get(objId);

            if (baseId != null) {
                Profile.Obj baseObj = _outProfile.getObj(TXTSectionId.valueOf(baseId.toString()));

                if (baseObj != null) {
                    obj.merge(baseObj);
                }
            }

            obj.merge(otherObj);
        }

        //pack.getProfile().print();

        addProfile(pack.getProfile());

        ObjMod objMod = _outObjMods.computeIfAbsent(inFile, k -> ObjMod.createFromInFile(inFile));

        objMod.merge(pack.getObjMod());
    }

    private boolean isObjModFileExtended(File inFile) {
        return (inFile.equals(W3A.GAME_PATH) || inFile.equals(W3D.GAME_PATH) || inFile.equals(W3Q.GAME_PATH));
    }

    public interface Filter {
        Predicate<Id> calcRemovedIds(Collection<Id> allIds);
    }

    public Filter FILTER_MODDED_OR_CUSTOM = allIds -> {
        Collection<Id> moddedIds = new LinkedHashSet<>();

        for (ObjMod objMod : _inObjMods) {
            for (ObjMod.Obj obj : objMod.getObjsList()) {
                moddedIds.add(obj.getId());
            }
        }

        return id -> !moddedIds.contains(id);
    };

    private Collection<ObjId> findObjRefs(@Nonnull Id id) {
        /*Collection<ObjId> refs = new LinkedHashSet<>();

        if (id instanceof ObjId) {
            for (Map.Entry<File, SLK> slkEntry : _outSlks.entrySet()) {
                File slkFile = slkEntry.getKey();
                SLK slk = slkEntry.getValue();

                if (slk.containsObj((ObjId) id)) {
                    SLK.Obj slkObj = slk.getObj((ObjId) id);

                    Map<FieldId, DataType> map = slkObj.getVals();

                    for (Map.Entry<FieldId, DataType> valEntry : map.entrySet()) {
                        FieldId fieldId = valEntry.getKey();

                        for (RawMetaSLK.Obj metaObj : _metaSlk.getObjs().values()) {
                            File slkName = MetaSLK.convertSLKName(metaObj.getS(FieldId.valueOf("slk")));

                            if (slkName != null && slkName.equals(slkFile)) {
                                FieldId otherFieldId = FieldId.valueOf(metaObj.getS(MetaFieldId.valueOf("field")));

                                Integer repeatI = MetaSLK.getRepeatI(metaObj);

                                if (repeatI == null) {
                                    otherFieldId = MetaSLK.getSLKField(slkFile, metaObj, null);

                                    if (otherFieldId.equals(fieldId)) {
                                        //found matching meta field

                                        if (metaObj.getS(MetaFieldId.valueOf("type")).equals("UnitID")) {
                                            DataType val = valEntry.getValue();

                                            refs.add(ObjId.valueOf(val.toString()));
                                        }
                                    }
                                } else {
                                    for (int level = 1; level <= repeatI; level++) {
                                        otherFieldId = MetaSLK.getSLKField(slkFile, metaObj, level);

                                        if (metaObj.getS(MetaFieldId.valueOf("type")).equals("UnitID")) {
                                            DataType val = valEntry.getValue();

                                            refs.add(ObjId.valueOf(val.toString()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }*/

        Collection<ObjId> refs = new LinkedHashSet<>();

        if (id instanceof ObjId) {
            for (ObjMod objMod : _outObjMods.values()) {
                if (objMod.containsObj((ObjId) id)) {
                    ObjMod.Obj objModObj = objMod.getObj((ObjId) id);

                    for (Map.Entry<MetaFieldId, ObjMod.Obj.Field> fieldEntry : objModObj.getFields().entrySet()) {
                        MetaFieldId fieldId = fieldEntry.getKey();
                        ObjMod.Obj.Field field = fieldEntry.getValue();

                        for (ObjMod.Obj.Field.Val val : field.getVals().values()) {
                            DataType realVal = val.getVal();

                            if (realVal != null) {
                                String[] vals = realVal.toString().split(",");

                                for (String valSingle : vals) {
                                    ObjId ref = ObjId.valueOf(valSingle);

                                    if (ref.toString().length() == 4) {
                                        refs.add(ref);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (Map.Entry<File, SLK> slkEntry : _outSlks.entrySet()) {
                File slkFile = slkEntry.getKey();
                SLK slk = slkEntry.getValue();

                if (slk.containsObj((ObjId) id)) {
                    SLK.Obj slkObj = slk.getObj((ObjId) id);

                    Map<SLKState, DataType> stateVals = slkObj.getStateVals();

                    for (Map.Entry<SLKState, DataType> stateVal : stateVals.entrySet()) {
                        SLKState state = stateVal.getKey();
                        DataType val = stateVal.getValue();

                        if (val != null) {
                            DataTypeInfo stateInfo = state.getInfo();

                            Class<? extends DataType> stateType = stateInfo.getType();
                            //System.out.println(state.getFieldId() + ";" + stateType);
                            if (ObjId.class.isAssignableFrom(stateType)) {
                                //System.out.println(id + " add " + val);
                                try {
                                    refs.add((ObjId) stateInfo.tryCastVal(val));
                                } catch (DataTypeInfo.CastException e) {
                                    Log.error(e.getMessage(), e);
                                }
                            } else if (stateType == DataList.class) {
                                for (DataTypeInfo subInfo : stateInfo.getGenerics()) {

                                    Class<? extends DataType> subType = subInfo.getType();

                                    if (ObjId.class.isAssignableFrom(subType)) {
                                        DataList valList;

                                        if (val instanceof War3String) {
                                            valList = new DataList(ObjId.class);

                                            for (String s : val.toString().split(",")) {
                                                valList.add(ObjId.valueOf(s));
                                            }
                                        } else {
                                            valList = (DataList) val;
                                        }

                                        for (Object valSingle : valList) {
                                            if (valSingle instanceof DataType) {
                                                try {
                                                    refs.add((ObjId) subInfo.tryCastVal((DataType) valSingle));
                                                } catch (DataTypeInfo.CastException e) {
                                                    Log.error(e.getMessage(), e);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (_outProfile.containsObj(TXTSectionId.valueOf(id.toString()))) {
                Profile.Obj profileObj = _outProfile.getObj(TXTSectionId.valueOf(id.toString()));

                for (Map.Entry<FieldId, Profile.Obj.Field> fieldEntry : profileObj.getFields().entrySet()) {
                    FieldId fieldId = fieldEntry.getKey();
                    Profile.Obj.Field field = fieldEntry.getValue();

                    for (DataType val : field.getVals()) {
                        if (val != null) {
                            ObjId ref = ObjId.valueOf(val.toString());

                            if (ref.toString().length() == 4) {
                                refs.add(ref);
                            }
                        }
                    }
                }
            }
        }

        //System.out.println("found " + refs);

        return refs;
    }

    public void filter(@Nonnull Filter filter) throws IOException {
        Collection<Id> allIds = new LinkedHashSet<>();

        Log.info("filter slk");
        for (SLK slk : _outSlks.values()) {
            Collection<SLK.Obj> objs = new ArrayList<>(slk.getObjs().values());

            for (SLK.Obj obj : objs) {
                allIds.add(obj.getId());
            }
        }
        Log.info("filter profile");
        for (Profile.Obj obj : _outProfile.getObjs().values()) {
            TXTSectionId objId = obj.getId();

            if (objId != null && objId.toString().length() == 4) {
                allIds.add(objId);
            }
        }

        Log.info("calc removed ids");
        Predicate<Id> predicate = filter.calcRemovedIds(allIds);

        Collection<Id> removedIds = new LinkedHashSet<>(allIds);

        removedIds.removeIf(predicate.negate());

        Log.info("remove special ids");
        removedIds.remove(Id.valueOf("Avul"));
        removedIds.remove(Id.valueOf("Aloc"));
        removedIds.remove(Id.valueOf("Aeth"));
        removedIds.remove(Id.valueOf("Abdt"));
        removedIds.remove(Id.valueOf("Apit"));
        removedIds.remove(Id.valueOf("AInv"));
        removedIds.remove(Id.valueOf("Ahrp"));
        removedIds.remove(Id.valueOf("Adtg"));
        removedIds.remove(Id.valueOf("Ane2"));
        removedIds.remove(Id.valueOf("Aalr"));

        Log.info("find j refed ids");
        Collection<Id> jRefedIds = new LinkedHashSet<>();

        for (File jFile : _jFiles) {
            Jass j = new Jass(jFile);

            Log.info("examine tokens");
            for (Token token : j.getTokens()) {
                if (token.getType() == JassLexer.INT_LITERAL) {
                    String text = token.getText();

                    int val;

                    if (text.startsWith("0x") || text.startsWith("0X")) {
                        val = Math.decode(text.substring(2).toLowerCase(), Math.CODE_HEX);
                    } else if (text.startsWith("$")) {
                        val = Math.decode(text.substring(1), Math.CODE_HEX);
                    } else if (text.startsWith("0")) {
                        val = Math.decode(text.substring(1), Math.CODE_OCT);
                    } else if (text.startsWith("'")) {
                        val = Math.decode(text.substring(1, text.length() - 1), Math.CODE_ASCII);
                    } else {
                        val = Math.decode(text, Math.CODE_DEC);
                    }

                    Id id = Id.valueOf(Math.encode(val, Math.CODE_ASCII));

                    if (id.toString().length() == 4) {
                        jRefedIds.add(id);
                    }
                }
            }
        }

        Log.info("jRefedIds: " + jRefedIds);

        /*HashSet<Id> intersection = new LinkedHashSet<>(jRefedIds);

        intersection.removeIf(id -> !removedIds.contains(id));

        System.out.println(intersection);*/

        removedIds.removeAll(jRefedIds);

        Log.info("find doo refed ids");
        Collection<ObjId> dooRefedIds = new LinkedHashSet<>();

        for (File file : _dooFiles) {
            DOO doo = new DOO(file);

            for (DOO.Dood dood : doo.getDoods()) {
                ObjId id = dood.getTypeId();

                dooRefedIds.add(id);
            }

            for (DOO.SpecialDood dood : doo.getSpecialDoods()) {
                ObjId id = dood.getTypeId();

                dooRefedIds.add(id);
            }
        }

        Log.info("dooRefedIds: " + dooRefedIds);

        removedIds.removeAll(dooRefedIds);

        Collection<Id> remainingIds = new LinkedHashSet<>(allIds);

        remainingIds.removeAll(removedIds);

        Log.info("find link refed ids");
        Collection<ObjId> linkRefedIds = new LinkedHashSet<>();

        for (Id id : new LinkedHashSet<>(remainingIds)) {
            Collection<ObjId> refedObjs = findObjRefs(id);

            linkRefedIds.addAll(refedObjs);
        }

        Log.info("linkRefedIds: " + linkRefedIds);

        removedIds.removeAll(linkRefedIds);

        for (SLK slk : _outSlks.values()) {
            for (Id id : removedIds) {
                if (id instanceof ObjId) {
                    slk.removeObj((ObjId) id);
                }
            }
        }

        for (Id id : removedIds) {
            if (id instanceof TXTSectionId) {
                _outProfile.removeObj((TXTSectionId) id);
            }
        }
    }

    private final Collection<File> _jFiles = new LinkedHashSet<>();
    private final Collection<File> _dooFiles = new LinkedHashSet<>();

    private void addFiles(Map<File, File> metaSlkFiles, Map<File, File> slkFiles, Map<File, File> profileFiles, Map<File, File> objModFiles, File wtsFile,
                          File jFile, File dooFile) throws Exception {
        Log.info("adding exported files to object merger");
        if (wtsFile == null) {
            Log.info("no WTS file");
        } else {
            WTS wts = new WTS(wtsFile);

            Translator translator = new Translator();

            translator.addTXT(wts.toTXT());

            _outProfile.setTranslator(translator);
        }

        Log.info("add meta slks");
        for (Map.Entry<File, File> fileEntry : metaSlkFiles.entrySet()) {
            File outFile = fileEntry.getValue();

            RawMetaSLK metaSlk = new RawMetaSLK(outFile);

            addMetaSlk(metaSlk);
        }

        Log.info("add slks");

        for (Map.Entry<File, File> fileEntry : slkFiles.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            SLK slk = SLK.createFromInFile(inFile, outFile);

            addSlk(inFile, slk);
        }

        Log.info("add profiles");
        for (Map.Entry<File, File> fileEntry : profileFiles.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            Profile profile = new Profile(outFile);

            addProfile(profile);
        }
        Log.info("add objmods");
        for (Map.Entry<File, File> fileEntry : objModFiles.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            ObjMod objMod = ObjMod.createFromInFile(inFile, outFile);

            addObjMod(inFile, objMod);
        }

        if (jFile != null) {
            Log.info("add j");
            _jFiles.add(jFile);
        }

        if (dooFile != null) {
            Log.info("add doo");
            _dooFiles.add(dooFile);
        }
    }


    private void exportFiles(File dir, Map<File, File> fileEntries) throws IOException {
        Orient.removeDir(dir);
        Orient.createDir(dir);

        for (Map.Entry<File, File> fileEntry : fileEntries.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            Orient.copyFile(outFile, new File(dir, inFile.toString()), true);
        }
    }

    private void exportFiles(File outDir, Map<File, File> metaSlkFiles, Map<File, File> slkFiles, Map<File, File> profileFiles, Map<File, File> objModFiles,
                             File wtsFile, File jFile, File dooFile) throws IOException {
        Map<File, File> fileEntries = new LinkedHashMap<>();

        for (Map.Entry<File, File> fileEntry : metaSlkFiles.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            fileEntries.put(inFile, outFile);
        }

        for (Map.Entry<File, File> fileEntry : slkFiles.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            fileEntries.put(inFile, outFile);
        }

        for (Map.Entry<File, File> fileEntry : profileFiles.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            fileEntries.put(inFile, outFile);
        }

        for (Map.Entry<File, File> fileEntry : objModFiles.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            fileEntries.put(inFile, outFile);
        }

        if (wtsFile != null) {
            fileEntries.put(WTS.GAME_PATH, wtsFile);
        }

        if (jFile != null) {
            fileEntries.put(Jass.GAME_PATH, jFile);
        }

        if (dooFile != null) {
            fileEntries.put(DOO.GAME_PATH, dooFile);
        }

        exportFiles(outDir, fileEntries);
    }

    private final static Collection<File> _metaSlkInFiles = Arrays.asList(
            //DoodadsMetaSLK.GAME_PATH,
            AbilityBuffMetaSLK.GAME_PATH,
            AbilityMetaSLK.GAME_PATH,
            DestructableMetaSLK.GAME_PATH,
            UnitMetaSLK.GAME_PATH,
            UpgradeMetaSLK.GAME_PATH);

    private final static Collection<File> _slkInFiles = Arrays.asList(
            //DoodSLK.GAME_PATH,
            UnitAbilsSLK.GAME_PATH,
            UnitBalanceSLK.GAME_PATH,
            UnitDataSLK.GAME_PATH,
            UnitUISLK.GAME_PATH,
            UnitWeaponsSLK.GAME_PATH,
            ItemSLK.GAME_PATH,
            DestructableSLK.GAME_PATH,
            AbilSLK.GAME_PATH,
            BuffSLK.GAME_PATH,
            UpgradeSLK.GAME_PATH);

    private final static Collection<File> _profileInFiles = Arrays.asList(Profile.getNativePaths());

    private final static Collection<File> _objModInFiles = Arrays.asList(
            W3A.GAME_PATH,
            W3B.GAME_PATH,
            W3D.GAME_PATH,
            W3H.GAME_PATH,
            W3Q.GAME_PATH,
            W3T.GAME_PATH,
            W3U.GAME_PATH);

    public void addDir(File dir) throws Exception {
        Log.info("Adding directory of files to be merged: " + dir.getAbsolutePath());
        Map<File, File> files = new LinkedHashMap<>();

        for (File outFile : Orient.getFiles(dir)) {
            File inFile = new File(outFile.toString().substring(dir.toString().length() + 1));

            files.put(inFile, outFile);
        }

        Map<File, File> metaSlkFiles = new LinkedHashMap<>();
        Map<File, File> slkFiles = new LinkedHashMap<>();
        Map<File, File> profileFiles = new LinkedHashMap<>();
        Map<File, File> objModFiles = new LinkedHashMap<>();
        File wtsFile = null;
        File jFile = null;
        File dooFile = null;

        for (Map.Entry<File, File> fileEntry : files.entrySet()) {
            File inFile = fileEntry.getKey();
            File outFile = fileEntry.getValue();

            if (_metaSlkInFiles.contains(inFile)) {
                metaSlkFiles.put(inFile, outFile);
            }
            if (_slkInFiles.contains(inFile)) {
                slkFiles.put(inFile, outFile);
            }
            if (_profileInFiles.contains(inFile)) {
                profileFiles.put(inFile, outFile);
            }
            if (_objModInFiles.contains(inFile)) {
                objModFiles.put(inFile, outFile);
            }
            if (WTS.GAME_PATH.equals(inFile)) {
                wtsFile = outFile;
            }
            if (Jass.GAME_PATH.equals(inFile)) {
                jFile = outFile;
            }
            if (DOO.GAME_PATH.equals(inFile)) {
                dooFile = outFile;
            }
        }

        addFiles(metaSlkFiles, slkFiles, profileFiles, objModFiles, wtsFile, jFile, dooFile);
    }

    private void addExports(File outDir, MpqPort.Out.Result metaSlkResult, MpqPort.Out.Result slkResult, MpqPort.Out.Result profileResult, MpqPort.Out.Result
            objModResult, MpqPort.Out.Result wtsResult, MpqPort.Out.Result jResult, MpqPort.Out.Result dooResult) throws Exception {
        Map<File, File> metaSlkFiles = new LinkedHashMap<>();

        processSegments(metaSlkResult, metaSlkFiles);

        Map<File, File> slkFiles = new LinkedHashMap<>();

        processSegments(slkResult, slkFiles);

        Map<File, File> profileFiles = new LinkedHashMap<>();

        processSegments(profileResult, profileFiles);

        Map<File, File> objModFiles = new LinkedHashMap<>();

        processSegments(objModResult, objModFiles);

        File wtsFile = null;

        try {
            wtsFile = wtsResult.getFile(WTS.GAME_PATH);
        } catch (NoSuchFileException ignored) {
        }

        File jFile = null;

        try {
            jFile = wtsResult.getFile(Jass.GAME_PATH);
        } catch (NoSuchFileException ignored) {
        }

        File dooFile = null;

        try {
            dooFile = dooResult.getFile(DOO.GAME_PATH);
        } catch (NoSuchFileException ignored) {
        }

        exportFiles(outDir, metaSlkFiles, metaSlkFiles, profileFiles, objModFiles, wtsFile, jFile, dooFile);

        addFiles(metaSlkFiles, metaSlkFiles, profileFiles, objModFiles, wtsFile, jFile, dooFile);
    }

    private void processSegments(MpqPort.Out.Result metaSlkResult, Map<File, File> metaSlkFiles) throws IOException {
        for (MpqPort.Out.Result.Segment segment : metaSlkResult.getExports().values()) {
            File inFile = segment.getExport().getInFile();

            try {
                File outFile = metaSlkResult.getFile(inFile);

                metaSlkFiles.put(inFile, outFile);
            } catch (NoSuchFileException ignored) {
            }
        }
    }

    private final static File PROFILE_OUTPUT_PATH = CampaignUnitStrings.GAME_PATH;

    public void writeToDir(File outDir, boolean clean) throws Exception {
        Orient.removeDir(outDir);
        Orient.createDir(outDir);
        _outSlks.remove(DoodSLK.GAME_PATH);

        for (Map.Entry<File, SLK> slkEntry : _outSlks.entrySet()) {
            File inFile = slkEntry.getKey();
            SLK slk = slkEntry.getValue();

            if (clean) {
                slk.cleanEmptyColumns();
                SLKCleaner.clean(slk);
            }
            File outFile = new File(outDir, inFile.toString());

            slk.write(outFile);
        }

        File emptyFile = new File(outDir, "empty.txt");

        OutputStream emptyFileStream = Orient.createFileOutputStream(emptyFile);

        //emptyFileStream.write(0x00);

        emptyFileStream.close();

        //Orient.createFile(emptyFile);

        for (File inFile : _profileInFiles) {
            File outFile = new File(outDir, inFile.toString());

            Orient.copyFile(emptyFile, outFile, true);
        }

        File profileOutFile = new File(outDir, PROFILE_OUTPUT_PATH.toString());

        if(clean) {
            ProfileCleaner.clean(_outProfile);
        }

        _outProfile.write(profileOutFile);

        for (Map.Entry<File, ObjMod> objModEntry : _outObjMods.entrySet()) {
            File inFile = objModEntry.getKey();
            ObjMod objMod = objModEntry.getValue();
            if (objMod instanceof W3A) {
                objMod.print();
            }
            File outFile = new File(outDir, inFile.toString());

            Wc3BinOutputStream outStream = new Wc3BinOutputStream(outFile);

            objMod.write(outStream, isObjModFileExtended(inFile));

            outStream.close();
        }
    }

    public void writeToMap(File mapFile, File outDir) throws Exception {
        //File outDir = _workDir;

        Orient.removeDir(outDir);
        Orient.createDir(outDir);

        MpqPort.In portIn = new JMpqPort.In();
        _outSlks.remove(DoodSLK.GAME_PATH);
        for (Map.Entry<File, SLK> slkEntry : _outSlks.entrySet()) {
            File inFile = slkEntry.getKey();
            SLK slk = slkEntry.getValue();

            File outFile = new File(outDir, inFile.toString());

            slk.cleanEmptyColumns();
            slk.write(outFile);

            portIn.add(outFile, inFile);
        }

        for (File inFile : _profileInFiles) {
            File outFile = new File(outDir, inFile.toString());

            new Profile().write(outFile);
        }

        File profileOutFile = new File(outDir, PROFILE_OUTPUT_PATH.toString());

        _outProfile.write(profileOutFile);

        for (File inFile : _profileInFiles) {
            File outFile = new File(outDir, inFile.toString());

            portIn.add(outFile, inFile);
        }

        for (Map.Entry<File, ObjMod> objModEntry : _outObjMods.entrySet()) {
            File inFile = objModEntry.getKey();
            ObjMod objMod = objModEntry.getValue();

            File outFile = new File(outDir, inFile.toString());

            Wc3BinOutputStream outStream = new Wc3BinOutputStream(outFile);

            objMod.write(outStream, isObjModFileExtended(inFile));

            outStream.close();

            portIn.add(outFile, inFile);
        }

        //if(true) return;
        portIn.commit(mapFile);
    }

    public void exportMap(File mapFile, boolean includeNativeData, File wc3Dir, File outDir) throws Exception {
        Log.info("Exporting map: " + mapFile.getAbsolutePath());
        Vector<File> mpqFiles = new Vector<>();

        mpqFiles.add(mapFile);

        if (includeNativeData) {
            if (wc3Dir == null) {
                //throw new Exception("no wc3Dir");
                mpqFiles.add(new MpqPort.ResourceFile(""));
            } else {
                mpqFiles.addAll(JMpqPort.getWc3Mpqs(wc3Dir));
            }
        }

        Vector<File> metaMpqFiles = new Vector<>();

        metaMpqFiles.add(mapFile);

        if (wc3Dir == null) {
            //throw new Exception("no wc3Dir");
            metaMpqFiles.add(new MpqPort.ResourceFile(""));
        } else {
            metaMpqFiles.addAll(JMpqPort.getWc3Mpqs(wc3Dir));
        }

        MpqPort.Out metaPortOut = new JMpqPort.Out();

        for (File inFile : _metaSlkInFiles) {
            metaPortOut.add(inFile);
        }

        MpqPort.Out.Result metaPortResult = metaPortOut.commit(metaMpqFiles);

        Map<File, File> outFiles = new LinkedHashMap<>();

        for (Map.Entry<File, MpqPort.Out.Result.Segment> segmentEntry : metaPortResult.getExports().entrySet()) {
            File inFile = segmentEntry.getKey();

            try {
                File outFile = metaPortResult.getFile(inFile);

                outFiles.put(inFile, outFile);
            } catch (NoSuchFileException ignored) {
            }
        }

        MpqPort.Out portOut = new JMpqPort.Out();

        Collection<File> slkFiles = new ArrayList<>(_slkInFiles);

        for (File inFile : slkFiles) {
            portOut.add(inFile);
        }

        Collection<File> profileFiles = new ArrayList<>(_profileInFiles);

        for (File inFile : profileFiles) {
            portOut.add(inFile);
        }

        for (File inFile : _objModInFiles) {
            portOut.add(inFile);
        }

        portOut.add(WTS.GAME_PATH);

        portOut.add(Jass.GAME_PATH);

        portOut.add(DOO.GAME_PATH);

        MpqPort.Out.Result portResult = portOut.commit(mpqFiles);

        for (Map.Entry<File, MpqPort.Out.Result.Segment> segmentEntry : portResult.getExports().entrySet()) {
            File inFile = segmentEntry.getKey();

            try {
                File outFile = portResult.getFile(inFile);

                outFiles.put(inFile, outFile);
            } catch (NoSuchFileException ignored) {
            }
        }
        Log.info("Extracting following files: " + Arrays.toString(outFiles.values().toArray()));

        exportFiles(outDir, outFiles);
    }

    public void exportMap(File mapFile, File outDir) throws Exception {
        exportMap(mapFile, true, MpqPort.getWc3Dir(), outDir);
    }

    public void readFromMap(File mapFile, boolean includeNativeData, File wc3Dir, File outDir) throws Exception {
        //File outDir = _workDir;

        Orient.removeDir(outDir);
        Orient.createDir(outDir);

        exportMap(mapFile, includeNativeData, wc3Dir, outDir);

        addDir(outDir);
    }

    public void readFromMap2(File mapFile, boolean includeNativeData, File wc3Dir, File outDir) throws Exception {
        Vector<File> mpqFiles = new Vector<>();

        mpqFiles.add(mapFile);

        if (includeNativeData) {
            if (wc3Dir == null) throw new Exception("no wc3Dir");

            mpqFiles.addAll(JMpqPort.getWc3Mpqs(wc3Dir));
        }

        MpqPort.Out metaSlkPortOut = new JMpqPort.Out();

        for (File inFile : _metaSlkInFiles) {
            metaSlkPortOut.add(inFile);
        }

        MpqPort.Out.Result metaSlkResult = metaSlkPortOut.commit(mpqFiles);

        MpqPort.Out slkPortOut = new JMpqPort.Out();

        Collection<File> slkFiles = new ArrayList<>(_slkInFiles);

        for (File inFile : slkFiles) {
            slkPortOut.add(inFile);
        }

        MpqPort.Out.Result slkResult = slkPortOut.commit(mpqFiles);

        Collection<File> profileFiles = new ArrayList<>(_profileInFiles);

        MpqPort.Out profilePortOut = new JMpqPort.Out();

        for (File inFile : profileFiles) {
            profilePortOut.add(inFile);
        }

        MpqPort.Out.Result profileResult = profilePortOut.commit(mpqFiles);

        MpqPort.Out objModPortOut = new JMpqPort.Out();

        for (File inFile : _objModInFiles) {
            objModPortOut.add(inFile);
        }

        MpqPort.Out.Result objModResult = objModPortOut.commit(mapFile);

        MpqPort.Out wtsPortOut = new JMpqPort.Out();

        wtsPortOut.add(WTS.GAME_PATH);

        MpqPort.Out.Result wtsResult = wtsPortOut.commit(mapFile);

        MpqPort.Out jPortOut = new JMpqPort.Out();

        jPortOut.add(Jass.GAME_PATH);

        MpqPort.Out.Result jResult = jPortOut.commit(mapFile);

        MpqPort.Out dooPortOut = new JMpqPort.Out();

        dooPortOut.add(DOO.GAME_PATH);

        MpqPort.Out.Result dooResult = dooPortOut.commit(mapFile);

        addExports(outDir, metaSlkResult, slkResult, profileResult, objModResult, wtsResult, jResult, dooResult);
    }

    public void readFromMap(File mapFile, boolean includeNativeData, File outDir) throws Exception {
        readFromMap(mapFile, includeNativeData, MpqPort.getWc3Dir(), outDir);
    }

}