package org.onosproject.ipran.serializers.impl;

import org.onosproject.net.Link;
import org.onosproject.net.link.LinkEvent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class LinkEventSerializer extends Serializer<LinkEvent> {

	/**
	 * Creates {@link LinkEvent} serializer instance.
	 */
	public LinkEventSerializer() {
		// non-null, immutable
		super(false, true);
	}

	@Override
	public void write(Kryo kryo, Output output, LinkEvent object) {
		kryo.writeClassAndObject(output, object.type());
		kryo.writeClassAndObject(output, object.subject());
		kryo.writeClassAndObject(output, object);

	}

	@Override
	public LinkEvent read(Kryo kryo, Input input, Class<LinkEvent> type) {

		org.onosproject.net.link.LinkEvent.Type linkType = (org.onosproject.net.link.LinkEvent.Type) kryo
				.readClassAndObject(input);
		Link link = (Link) kryo.readClassAndObject(input);
		long time = (long) kryo.readClassAndObject(input);
		return new LinkEvent(linkType, link, time);
	}

}
