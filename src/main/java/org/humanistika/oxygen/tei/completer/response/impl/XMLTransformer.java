/**
 * TEI Completer
 * An Oxygen XML Editor plugin for customizable attribute and value completion for TEI P5 documents
 * Copyright (C) 2016 Belgrade Center for Digital Humanities
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.humanistika.oxygen.tei.completer.response.impl;

import org.humanistika.oxygen.tei.completer.response.ResponseTransformer;
import org.humanistika.oxygen.tei.completer.response.TransformationException;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 2016028
 */
public class XMLTransformer implements ResponseTransformer {
    static {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
    }
    final static TransformerFactory factory = TransformerFactory.newInstance();

    final ConcurrentHashMap<Path, TimestampedTemplates> cache = new ConcurrentHashMap<>();

    @Override
    public void transform(final InputStream content, final Path transformation, final OutputStream result) throws IOException, TransformationException {
        try {
            final Templates templates = getTemplates(transformation);
            final Transformer transformer = templates.newTransformer();
            transformer.transform(new StreamSource(content), new StreamResult(result));
        } catch (final TransformerException e) {
            throw new TransformationException(e);
        }
    }

    private Templates getTemplates(final Path transformation) throws IOException, TransformerConfigurationException {
        final TimestampedTemplates cached = cache.get(transformation);
        if (cached == null || cached.timestamp < Files.getLastModifiedTime(transformation).toMillis()) {
            return cacheTemplates(compileTemplates(transformation), transformation);
        } else {
            return cached.templates;
        }
    }

    private TimestampedTemplates compileTemplates(final Path transformation) throws IOException, TransformerConfigurationException {
        final long timestamp = Files.getLastModifiedTime(transformation).toMillis();
        try(final InputStream is = Files.newInputStream(transformation)) {
            final Templates templates = factory.newTemplates(new StreamSource(is, transformation.toAbsolutePath().toString()));
            return new TimestampedTemplates(timestamp, templates);
        }
    }

    private Templates cacheTemplates(final TimestampedTemplates timestampedTemplates, final Path transformation) {
        cache.put(transformation, timestampedTemplates);
        return timestampedTemplates.templates;
    }

    public class TimestampedTemplates {
        public final long timestamp;
        public final Templates templates;

        public TimestampedTemplates(final long timestamp, final Templates templates) {
            this.timestamp = timestamp;
            this.templates = templates;
        }
    }
}
