package net.moonlightflower.wc3libs.misc.model.mdx;

import net.moonlightflower.wc3libs.bin.BinInputStream;
import net.moonlightflower.wc3libs.bin.BinStream;
import net.moonlightflower.wc3libs.bin.Wc3BinInputStream;
import net.moonlightflower.wc3libs.bin.Wc3BinOutputStream;
import net.moonlightflower.wc3libs.misc.Id;
import net.moonlightflower.wc3libs.misc.model.MDX;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class VertexGroupChunk extends Chunk {
    public static Id TOKEN = Id.valueOf("GNDX");

    @Override
    public Id getToken() {
        return TOKEN;
    }

    private List<VertexGroup> _vertexGroups = new ArrayList<>();

    public List<VertexGroup> getVertexGroups() {
        return new ArrayList<>(_vertexGroups);
    }

    public void addVertexGroup(@Nonnull VertexGroup val) {
        if (!_vertexGroups.contains(val)) {
            _vertexGroups.add(val);
        }
    }

    @Override
    public void write(@Nonnull Wc3BinOutputStream stream, @Nonnull MDX.EncodingFormat format) throws BinInputStream.StreamException {
        stream.writeId(TOKEN);

        stream.writeUInt32(getVertexGroups().size());

        for (VertexGroup vertexGroup : getVertexGroups()) {
            vertexGroup.write(stream);
        }
    }

    public void write(@Nonnull Wc3BinOutputStream stream) throws BinStream.StreamException {
        write(stream, MDX.EncodingFormat.AUTO);
    }

    public VertexGroupChunk(@Nonnull Wc3BinInputStream stream) throws BinStream.StreamException {
        Id token = stream.readId("token");

        if (!token.equals(getToken())) throw new IllegalArgumentException("invalid " + getToken() + " startToken (" + token + ")");

        long vertexGroupsCount = stream.readUInt32("vertexGroupsCount");

        while (vertexGroupsCount > 0) {
            addVertexGroup(new VertexGroup(stream));

            vertexGroupsCount--;
        }
    }
}