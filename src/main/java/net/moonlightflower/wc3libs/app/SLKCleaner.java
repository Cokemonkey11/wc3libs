package net.moonlightflower.wc3libs.app;

import net.moonlightflower.wc3libs.dataTypes.app.Wc3String;
import net.moonlightflower.wc3libs.slk.SLK;
import net.moonlightflower.wc3libs.slk.app.objs.*;

public class SLKCleaner {

    public static void clean(SLK slk) {
        if (slk instanceof UnitDataSLK) {
            clean((UnitDataSLK) slk);
        } else if (slk instanceof AbilSLK) {
            clean((AbilSLK) slk);
        } else if (slk instanceof BuffSLK) {
            clean((BuffSLK) slk);
        } else if (slk instanceof DestructableSLK) {
            clean((DestructableSLK) slk);
        } else if (slk instanceof ItemSLK) {
            clean((ItemSLK) slk);
        } else if (slk instanceof UnitAbilsSLK) {
            clean((UnitAbilsSLK) slk);
        } else if (slk instanceof UnitBalanceSLK) {
            clean((UnitBalanceSLK) slk);
        } else if (slk instanceof UnitWeaponsSLK) {
            clean((UnitWeaponsSLK) slk);
        } else if (slk instanceof UpgradeSLK) {
            clean((UpgradeSLK) slk);
        }
    }

    public static void clean(UnitDataSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(UnitDataSLK.States.EDITOR_COMMENTS, new Wc3String(""));
        });
    }

    public static void clean(AbilSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(AbilSLK.States.EDITOR_COMMENTS, new Wc3String(""));
        });
    }

    public static void clean(BuffSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(BuffSLK.States.EDITOR_COMMENTS, new Wc3String(""));
        });
    }

    public static void clean(DestructableSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(DestructableSLK.States.COMMENT, new Wc3String(""));
        });
    }

    public static void clean(ItemSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(ItemSLK.States.EDITOR_COMMENT, new Wc3String(""));
        });
    }

    public static void clean(UnitAbilsSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(UnitAbilsSLK.States.EDITOR_COMMENT, new Wc3String(""));
        });
    }

    public static void clean(UnitBalanceSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(UnitBalanceSLK.States.EDITOR_COMMENT, new Wc3String(""));
        });
    }

    public static void clean(UnitWeaponsSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(UnitWeaponsSLK.States.EDITOR_COMMENTS, new Wc3String(""));
        });
    }

    public static void clean(UpgradeSLK slk) {
        slk.getObjs().values().forEach(val -> {
            val.set(UpgradeSLK.States.EDITOR_COMMENTS, new Wc3String(""));
        });
    }
}
