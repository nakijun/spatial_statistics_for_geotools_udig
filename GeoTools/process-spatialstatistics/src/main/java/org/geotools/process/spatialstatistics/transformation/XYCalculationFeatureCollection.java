/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.transformation;

import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * XYCalculation SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class XYCalculationFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(XYCalculationFeatureCollection.class);

    private String xField;

    private String yField;

    private boolean useInside;

    private SimpleFeatureType schema;

    public XYCalculationFeatureCollection(SimpleFeatureCollection delegate, boolean useInside) {
        this(delegate, "xcoord", "ycoord", useInside);
    }

    public XYCalculationFeatureCollection(SimpleFeatureCollection delegate, String xField,
            String yField, boolean useInside) {
        super(delegate);

        if (xField == null || xField.isEmpty()) {
            throw new NullPointerException("x field is null");
        }

        if (yField == null || yField.isEmpty()) {
            throw new NullPointerException("y field is null");
        }
        
        this.xField = xField;
        this.yField = yField;
        this.useInside = useInside;

        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.schema = FeatureTypes.add(schema, xField, Double.class, 38);
        this.schema = FeatureTypes.add(schema, yField, Double.class, 38);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new XYCalculationFeatureIterator(delegate.features(), getSchema(), xField, yField,
                useInside);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    static class XYCalculationFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private String xField;

        private String yField;

        private boolean useInside = false;

        private SimpleFeatureBuilder builder;

        public XYCalculationFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, String xField, String yField, boolean useInside) {
            this.delegate = delegate;

            this.xField = xField;
            this.yField = yField;
            this.useInside = useInside;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature sourceFeature = delegate.next();
            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());

            // transfer attributes
            transferAttribute(sourceFeature, nextFeature);

            // calculate xy coordinates
            Geometry g = (Geometry) sourceFeature.getDefaultGeometry();
            Point center = useInside ? g.getInteriorPoint() : g.getCentroid();

            nextFeature.setAttribute(xField, center.getX());
            nextFeature.setAttribute(yField, center.getY());
            return nextFeature;
        }
    }
}
