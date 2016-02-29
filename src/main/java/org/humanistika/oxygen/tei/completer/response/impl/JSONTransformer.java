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

import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.file.Files;
import java.nio.file.Path;

import org.humanistika.oxygen.tei.completer.response.TransformationException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.json.JsonParser;

/**
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 2016028
 */
public class JSONTransformer implements ResponseTransformer {
    final static int OPTIMIZATION_LEVEL = 3;
    final static int LANGUAGE_VERSION = Context.VERSION_1_7;

    @Override
    public void transform(final InputStream content, final Path transformation, final OutputStream result) throws TransformationException, IOException {
        try {
            final Context context = Context.enter();
            context.setOptimizationLevel(OPTIMIZATION_LEVEL);
            context.setLanguageVersion(LANGUAGE_VERSION);

            final Scriptable scope = context.initStandardObjects();

            //read in the content
            final Object jsonObj = parseJson(context, scope, content);

            //load the javascript with the transform function
            try(final Reader reader = Files.newBufferedReader(transformation, UTF_8)) {
                context.evaluateReader(scope, reader, transformation.getFileName().toString(), 1, null);
            }

            final Object fnTransformObj = scope.get("transform", scope);
            if(!(fnTransformObj instanceof Function)) {
              throw new TransformationException("Function `transform` is not defined!");
            } else {
                final Object functionArgs[] = { jsonObj };
                final Function fnTransform = (Function)fnTransformObj;
                final Object resultObj = fnTransform.call(context, scope, scope, functionArgs);
                final String jsonResult = (String)NativeJSON.stringify(context, scope, resultObj, null, null);

                final char buf[] = new char[4096];
                int read = -1;
                try(final Reader jsonReader = new StringReader(jsonResult);
                    final Writer writer = new OutputStreamWriter(result)) {
                    while((read = jsonReader.read(buf)) > -1) {
                        writer.write(buf, 0, read);
                    }
                }
            }
        } finally {
            Context.exit();
        }
    }

    private Object parseJson(final Context context, final Scriptable scope, final InputStream json) throws IOException, TransformationException {
        try {
            final char buf[] = new char[4096];
            int read = -1;
            final StringBuilder builder = new StringBuilder();
            try (final Reader reader = new InputStreamReader(json, UTF_8)) {
                while ((read = reader.read(buf)) > -1) {
                    builder.append(buf, 0, read);
                }
            }
            return new JsonParser(context, scope).parseValue(builder.toString());
        } catch(final JsonParser.ParseException e) {
            throw new TransformationException("Unable to parse JSON", e);
        }
    }
}
