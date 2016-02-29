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
package org.humanistika.oxygen.tei.completer.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Transforms a Response from the Server
 *
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 2016028
 */
public interface ResponseTransformer {

    /**
     * Transform the content using the transformation
     *
     * @param content The content to transform
     * @param transformation The transformation to apply to the content
     * @return The transformed content
     */
    void transform(final InputStream content, final Path transformation, final OutputStream result) throws IOException, TransformationException;
}
