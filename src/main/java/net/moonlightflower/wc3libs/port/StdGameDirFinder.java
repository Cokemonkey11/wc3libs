package net.moonlightflower.wc3libs.port;

import net.moonlightflower.wc3libs.port.mac.MacGameDirFinder;

import javax.annotation.Nonnull;
import java.io.File;

public class StdGameDirFinder implements GameDirFinder {
    public StdGameDirFinder() {

    }

    protected GameDirFinder getMacGameDirFinder() {
        return new MacGameDirFinder();
    }

    private boolean _entered = false;

    @Nonnull
    @Override
    public File get() throws NotFoundException {
        if (_entered) throw new NotFoundException(new Exception("already tried"));

        _entered = true;
        
        try {
            String envWar3DirS = System.getenv("war3dir");

            if (envWar3DirS != null) {
                File envWar3Dir = new File(envWar3DirS);

                if (envWar3Dir.exists()) {
                    return envWar3Dir;
                }
            }

            if (Orient.isMacSystem()) {
                GameDirFinder macGameDirFinder = getMacGameDirFinder();

                try {
                    return macGameDirFinder.get();
                } catch (NotFoundException e) {
                    throw e;
                }
            } else if (Orient.isWindowsSystem()) {


                throw new NotFoundException();
            } else {
                throw new NotFoundException(new Exception("system not supported: " + Orient.getSystem()));
            }
        } finally {
            _entered = false;
        }
    }
}