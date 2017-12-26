package net.moonlightflower.wc3libs.misc.model.mdx;

import net.moonlightflower.wc3libs.bin.BinStream;
import net.moonlightflower.wc3libs.bin.Wc3BinInputStream;
import net.moonlightflower.wc3libs.bin.Wc3BinOutputStream;

import javax.annotation.Nonnull;

public class Bone {
    private Node _node;

    public Node getNode() {
        return _node;
    }

    private long _geosetId;

    public long getGeosetId() {
        return _geosetId;
    }

    private long _geosetAnimId;

    public long getGeosetAnimId() {
        return _geosetAnimId;
    }

    public void write(@Nonnull Wc3BinOutputStream stream) throws BinStream.StreamException {
        _node.write(stream);
        stream.writeUInt32(_geosetId);
        stream.writeUInt32(_geosetAnimId);
    }

    public Bone(@Nonnull Wc3BinInputStream stream) throws BinStream.StreamException {
        _node = new Node(stream);
        _geosetId = stream.readUInt32("geosetId");
        _geosetAnimId = stream.readUInt32("geosetAnimId");
    }
}
