package org.segfly.graml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

import org.segfly.graml.model.ClassmapSection;
import org.segfly.graml.model.EdgesSection;
import org.segfly.graml.model.GramlFactory;
import org.segfly.graml.model.GramlHeaderSection;
import org.segfly.graml.model.GraphSection;
import org.segfly.graml.model.VerticesSection;
import org.segfly.graml.model.impl.GramlFactoryImpl;
import org.yaml.snakeyaml.Yaml;

import com.tinkerpop.blueprints.Graph;

/**
 * Top level api and class for Graml. Using this class you can read a Graml compliant YAML file and load the graph
 * informatoin into a target Tinkerpop Blueprints {@link Graph} object. From there, you can commit, rollback, apply
 * other Graml files, etc.
 *
 * @author Nicholas Pace
 * @since Jan 3, 2015
 */
public class GramlReader {
    /** The target graph to load the graml data. */
    private Graph        target;

    /** Internal YAML processor */
    private Yaml         ymlProc;

    /** Dependency injection factory for Graml */
    private GramlFactory graml;

    /**
     * Creates a {@link GramlReader} that will load a Graml graph into the target Tinkerpop Bluprints {@link Graph}
     * object.
     *
     * @param target
     */
    public GramlReader(final Graph target) {
        this(target, new Yaml(), new GramlFactoryImpl());
    }

    /**
     * Package-private constructor for testing only. Does not use {@link GramlFactory} so that tests can easily inject
     * mocks.
     *
     * @param target
     * @param ymlProc
     * @param graml
     */
    GramlReader(final Graph target, final Yaml ymlProc, final GramlFactory graml) {
        super();
        this.target = target;
        this.ymlProc = ymlProc;
        this.graml = graml;
    }

    /**
     * Loads a Graml file from a {@link URL}.
     *
     * @param url
     * @throws IOException
     * @throws GramlException
     */
    public void load(final URL url) throws IOException, GramlException {
        InputStream stream = url.openStream();
        try {
            load(stream);
        } finally {
            stream.close();
        }
    }

    /**
     * Loads a Graml file from a {@link File}.
     *
     * @param file
     * @throws IOException
     * @throws GramlException
     */
    public void load(final File file) throws IOException, GramlException {
        load(new FileInputStream(file));
    }

    /**
     * Loads a Graml file from an {@link InputStream}.
     *
     * @param in
     * @throws IOException
     * @throws GramlException
     */
    public void load(final InputStream in) throws IOException, GramlException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(in, baos);
        baos.close();
        load(baos.toString());
    }

    /**
     * Loads Graml from a {@link String} representation.
     *
     * @param yaml
     * @throws GramlException
     */
    public void load(final String yaml) throws GramlException {
        // Read the YAML
        @SuppressWarnings("rawtypes")
        Map fullMap = (Map) ymlProc.load(yaml);

        // Decompose the sections
        GramlHeaderSection header = graml.getGramlHeaderSection(fullMap);
        ClassmapSection classmap = graml.getClassmapSection(fullMap);
        EdgesSection edgeProps = graml.getEdgesSection(fullMap);
        VerticesSection vertexProps = graml.getVerticesSection(fullMap);
        GraphSection graph = graml.getGraphSection(fullMap, classmap, vertexProps, edgeProps);

        // Alter the graph target
        graph.inject(target);
    }

    /**
     * Copies all bytes from the input stream to the output stream. Does not close or flush either stream. This is
     * copied from Guava ByteStreams. Generally, we don't want to cut/paste code, but we are trying to make the
     * dependency footprint of Graml very small.<br/>
     * <br/>
     * Special thanks to the original authors for this function.
     *
     * @param from
     *            the input stream to read from
     * @param to
     *            the output stream to write to
     * @return the number of bytes copied
     * @throws IOException
     *             if an I/O error occurs
     */
    private static long copy(final InputStream from, final OutputStream to) throws IOException {
        /*
         * Copyright (C) 2007 The Guava Authors Licensed under the Apache License, Version 2.0 (the "License"); you may
         * not use this file except in compliance with the License. You may obtain a copy of the License at
         * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
         * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
         * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
         * limitations under the License.
         */
        byte[] buf = new byte[0x1000]; // 4k
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }
}
