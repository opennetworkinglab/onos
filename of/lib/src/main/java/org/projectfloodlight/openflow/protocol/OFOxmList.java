package org.projectfloodlight.openflow.protocol;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.PrimitiveSinkable;
import org.projectfloodlight.openflow.util.ChannelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.PrimitiveSink;

public class OFOxmList implements Iterable<OFOxm<?>>, Writeable, PrimitiveSinkable {
    private static final Logger logger = LoggerFactory.getLogger(OFOxmList.class);

    private final Map<MatchFields, OFOxm<?>> oxmMap;

    public final static OFOxmList EMPTY = new OFOxmList(ImmutableMap.<MatchFields, OFOxm<?>>of());

    private OFOxmList(Map<MatchFields, OFOxm<?>> oxmMap) {
        this.oxmMap = oxmMap;
    }

    @SuppressWarnings("unchecked")
    public <T extends OFValueType<T>> OFOxm<T> get(MatchField<T> matchField) {
        return (OFOxm<T>) oxmMap.get(matchField.id);
    }

    public static class Builder {
        private final Map<MatchFields, OFOxm<?>> oxmMap;

        public Builder() {
            oxmMap = new EnumMap<MatchFields, OFOxm<?>>(MatchFields.class);
        }

        public Builder(EnumMap<MatchFields, OFOxm<?>> oxmMap) {
            this.oxmMap = oxmMap;
        }

        public <T extends OFValueType<T>> void set(OFOxm<T> oxm) {
            oxmMap.put(oxm.getMatchField().id, oxm);
        }

        public <T extends OFValueType<T>> void unset(MatchField<T> matchField) {
            oxmMap.remove(matchField.id);
        }

        public OFOxmList build() {
            return OFOxmList.ofList(oxmMap.values());
        }
    }

    @Override
    public Iterator<OFOxm<?>> iterator() {
        return oxmMap.values().iterator();
    }

    public static OFOxmList ofList(Iterable<OFOxm<?>> oxmList) {
        Map<MatchFields, OFOxm<?>> map = new EnumMap<MatchFields, OFOxm<?>>(
                MatchFields.class);
        for (OFOxm<?> o : oxmList) {
            OFOxm<?> canonical = o.getCanonical();

            if(logger.isDebugEnabled() && !Objects.equal(o, canonical)) {
                logger.debug("OFOxmList: normalized non-canonical OXM {} to {}", o, canonical);
            }

            if(canonical != null)
                map.put(canonical.getMatchField().id, canonical);

        }
        return new OFOxmList(map);
    }

    public static OFOxmList of(OFOxm<?>... oxms) {
        Map<MatchFields, OFOxm<?>> map = new EnumMap<MatchFields, OFOxm<?>>(
                MatchFields.class);
        for (OFOxm<?> o : oxms) {
            OFOxm<?> canonical = o.getCanonical();

            if(logger.isDebugEnabled() && !Objects.equal(o, canonical)) {
                logger.debug("OFOxmList: normalized non-canonical OXM {} to {}", o, canonical);
            }

            if(canonical != null)
                map.put(canonical.getMatchField().id, canonical);
        }
        return new OFOxmList(map);
    }

    public static OFOxmList readFrom(ChannelBuffer bb, int length,
            OFMessageReader<OFOxm<?>> reader) throws OFParseError {
        return ofList(ChannelUtils.readList(bb, length, reader));
    }

    @Override
    public void writeTo(ChannelBuffer bb) {
        for (OFOxm<?> o : this) {
            o.writeTo(bb);
        }
    }

    public OFOxmList.Builder createBuilder() {
        return new OFOxmList.Builder(new EnumMap<MatchFields, OFOxm<?>>(oxmMap));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oxmMap == null) ? 0 : oxmMap.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OFOxmList other = (OFOxmList) obj;
        if (oxmMap == null) {
            if (other.oxmMap != null)
                return false;
        } else if (!oxmMap.equals(other.oxmMap))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "OFOxmList" + oxmMap;
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        for (OFOxm<?> o : this) {
            o.putTo(sink);
        }
    }


}
