package com.bennight.bench.intersection;

import com.google.common.base.Preconditions;
import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Created by bennight on 6/13/2015.
 */
public class app {

    private static final Random RND = new Random(8675309);
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();

    public static void main(String[] args) throws IOException {

        SortedMap<Integer, DescriptiveStatistics> timeToPrepare = new TreeMap<>();
        SortedMap<Integer, SortedMap<Integer, DescriptiveStatistics>> intersects = new TreeMap<>();
        SortedMap<Integer, SortedMap<Integer, DescriptiveStatistics>> preparedIntersects = new TreeMap<>();


        //Test the time it takes to prepare geometries as a function of number of points in the geometry
        //Test values from 4 to 4000000

        Polygon queryGeom = null;
        PreparedPolygon preparedQueryGeom = null;
        long init = 0;
        long fin = 0;
        int hash = -1;




        for (int i = 10; i <= 10000000; i *= 10){
            //System.out.println(i);
            queryGeom = createPolygon(i, 0, 0, 60);
            timeToPrepare.put(i, new DescriptiveStatistics());
            for (int j = 0; j < 100; j++){
                init = System.nanoTime();
                preparedQueryGeom = new PreparedPolygon(queryGeom); //make sure we aren't getting any caching from the fctory
                fin = System.nanoTime() - init;
                timeToPrepare.get(i).addValue(fin);
            }
        }

        System.out.println("-----------------------------------------------");
        System.out.println("Sampled time to prepared geometry");
        System.out.println("Values are averaged for 100 repetitions");
        System.out.println("[NUM VERTICES] : [MSEC]  \u00B1 [RSD]");
        System.out.println("-----------------------------------------------");
        System.out.println(String.format("[100]          : %.5f \u00B1 %.2f%%", timeToPrepare.get(100).getMean() / 1000000d, timeToPrepare.get(100).getStandardDeviation() /  timeToPrepare.get(100).getMean() * 100 ));
        System.out.println(String.format("[1,000]        : %.5f \u00B1 %.2f%%", timeToPrepare.get(1000).getMean() / 1000000d, timeToPrepare.get(1000).getStandardDeviation() /  timeToPrepare.get(1000).getMean() * 100 ));
        System.out.println(String.format("[10,000]       : %.5f \u00B1 %.2f%%", timeToPrepare.get(10000).getMean() / 1000000d, timeToPrepare.get(10000).getStandardDeviation() /  timeToPrepare.get(10000).getMean() * 100 ));
        System.out.println(String.format("[100,000]      : %.5f \u00B1 %.2f%%", timeToPrepare.get(100000).getMean() / 1000000d, timeToPrepare.get(100000).getStandardDeviation() /  timeToPrepare.get(100000).getMean() * 100 ));
        System.out.println(String.format("[1,000,000]    : %.5f \u00B1 %.2f%%", timeToPrepare.get(1000000).getMean() / 1000000d, timeToPrepare.get(1000000).getStandardDeviation() /  timeToPrepare.get(1000000).getMean() * 100 ));
        System.out.println(String.format("[10,000,000]   : %.5f \u00B1 %.2f%%", timeToPrepare.get(10000000).getMean() / 1000000d, timeToPrepare.get(10000000).getStandardDeviation() /  timeToPrepare.get(10000000).getMean() * 100 ));
        System.out.println("-----------------------------------------------");




        File preparedIntersectsFile = new File("preparedIntersects.csv");
        BufferedWriter preparedIntersectsBW = new BufferedWriter(new FileWriter(preparedIntersectsFile));
        preparedIntersectsBW.write("#Target Vertices,#Query Vertices,MSEC,STDEV\r\n");


        File intersectsFile = new File("intersects.csv");
        BufferedWriter intersectsBW = new BufferedWriter(new FileWriter(intersectsFile));
        intersectsBW.write("#Target Vertices,#Query Vertices,MSEC,STDEV\r\n");



        int origNumTargetPoints = 10;
        int origNumQueryPoints = 10;

        int numTargetPoints = origNumTargetPoints;
        int numQueryPoints = origNumQueryPoints;
        int factor = 2;
        int maxPoints = 1310720;

        List<Polygon> polys = generatePolyCollections(1000, numTargetPoints);

        Polygon originalQueryGeom = createPolygon(numQueryPoints, 0, 0, 70);


        //start out at a low point count, and densify the points (interpolate) by the factor each time - for both the query and the target geometry
        while (numTargetPoints <= maxPoints) {
            numQueryPoints = origNumQueryPoints;

            preparedIntersects.put(numTargetPoints, new TreeMap<Integer, DescriptiveStatistics>());
            intersects.put(numTargetPoints, new TreeMap<Integer, DescriptiveStatistics>());

            queryGeom = geometryFactory.createPolygon(originalQueryGeom.getCoordinates());

            while (numQueryPoints <= maxPoints) {
                preparedQueryGeom = (PreparedPolygon) preparedGeometryFactory.create(geometryFactory.createPolygon(queryGeom.getCoordinates()));

                preparedIntersects.get(numTargetPoints).put(numQueryPoints, new DescriptiveStatistics());
                intersects.get(numTargetPoints).put(numQueryPoints, new DescriptiveStatistics());

                for (Polygon p : polys){
                    init = System.nanoTime();
                    preparedQueryGeom.intersects(p);
                    fin = System.nanoTime() - init;
                    preparedIntersects.get(numTargetPoints).get(numQueryPoints).addValue(fin);

                    init = System.nanoTime();
                    queryGeom.intersection(p);
                    fin = System.nanoTime() - init;
                    intersects.get(numTargetPoints).get(numQueryPoints).addValue(fin);
                }

                System.out.println("-------------------------------------------------------------------------------");
                System.out.println("Prepared: " + numTargetPoints + ":" + numQueryPoints + " = " + preparedIntersects.get(numTargetPoints).get(numQueryPoints).getMean() / 1000000d);
                System.out.println("Regular:  " + numTargetPoints + ":" + numQueryPoints + " = " + intersects.get(numTargetPoints).get(numQueryPoints).getMean() / 1000000d);
                System.out.println("-------------------------------------------------------------------------------");

                preparedIntersectsBW.write(numTargetPoints + "," + numQueryPoints +  "," + preparedIntersects.get(numTargetPoints).get(numQueryPoints).getMean() / 1000000d + "," + preparedIntersects.get(numTargetPoints).get(numQueryPoints).getStandardDeviation() / 1000000d + "\r\n");
                intersectsBW.write(numTargetPoints + "," + numQueryPoints +  "," + intersects.get(numTargetPoints).get(numQueryPoints).getMean() / 1000000d + "," + intersects.get(numTargetPoints).get(numQueryPoints).getStandardDeviation() / 1000000d + "\r\n");

                queryGeom = densifyPolygon(factor, queryGeom);
                numQueryPoints *= factor;
            }
            numTargetPoints *=  factor;
            if (numTargetPoints <= maxPoints) {
                polys = densifyPolyCollection(polys, factor);
            }

        }

        preparedIntersectsBW.close();
        intersectsBW.close();

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);


        System.out.println("-----------------------------------------------");
        System.out.println("Sampled time to intersect ");
        System.out.println("Values grouped by # vertices for query and target");
        System.out.println("[TARGET][QUERY]        : [MSEC]  \u00B1 [RSD]");
        System.out.println("-----------------------------------------------");
        for (Map.Entry<Integer, SortedMap<Integer, DescriptiveStatistics>> kvpTarget : intersects.entrySet()){
            for (Map.Entry<Integer, DescriptiveStatistics> kvpQuery : kvpTarget.getValue().entrySet()) {
                System.out.println(String.format("%-23.23s", String.format("[%s][%s]", nf.format(kvpTarget.getKey()), nf.format(kvpQuery.getKey()))) + ": "
                        + String.format("%07.5f \u00B1 %.2f%%", kvpQuery.getValue().getMean() / 1000000d,
                        kvpQuery.getValue().getStandardDeviation() /  kvpQuery.getValue().getMean() * 100));
            }
        }


        System.out.println("-----------------------------------------------");
        System.out.println("Sampled time to intersect with prepared geometry");
        System.out.println("Values grouped by # vertices for query and target");
        System.out.println("[TARGET][QUERY]        : [MSEC]  \u00B1 [RSD]");
        System.out.println("-----------------------------------------------");
        for (Map.Entry<Integer, SortedMap<Integer, DescriptiveStatistics>> kvpTarget : preparedIntersects.entrySet()){
            for (Map.Entry<Integer, DescriptiveStatistics> kvpQuery : kvpTarget.getValue().entrySet()) {
                System.out.println(String.format("%-23.23s", String.format("[%s][%s]", nf.format(kvpTarget.getKey()), nf.format(kvpQuery.getKey()))) + ": "
                        + String.format("%07.5f \u00B1 %.2f%%", kvpQuery.getValue().getMean() / 1000000d,
                        kvpQuery.getValue().getStandardDeviation() /  kvpQuery.getValue().getMean() * 100));
            }
        }

    }




    private static List<Polygon>  generatePolyCollections(int numPolygons, int numPoints){
        List<Polygon> polys = new ArrayList<>(numPolygons);

        for (int i = 0; i < numPolygons; i++){
            polys.add(createPolygon(numPoints, RND.nextInt(330) - 130, RND.nextInt(130) - 40, 40));
        }

        return polys;

    }

    private static List<Polygon> densifyPolyCollection(List<Polygon> polygons, int factor){
        Preconditions.checkArgument(factor >= 1);
        if (factor == 1) {
            return polygons;
        }

        List<Polygon> densifiedPolys = new ArrayList<>(polygons.size());
        for (Polygon p : polygons){
            densifiedPolys.add(densifyPolygon(factor, p));
        }
        return densifiedPolys;
    }

    private static Polygon createPolygon(
            final int numPoints ,
            double centerX,
            double centerY,
            double maxRadius) {

        final List<Coordinate> coords = new ArrayList<Coordinate>();

        final double increment = (double) 360 / numPoints;

        for (double theta = 0; theta <= 360; theta += increment) {
            final double radius = (RND.nextDouble() * maxRadius) + 0.1;
            final double rad = (theta * Math.PI) / 180.0;
            final double x = centerX + (radius * Math.sin(rad));
            final double y = centerY + (radius * Math.cos(rad));
            coords.add(new Coordinate(
                    x,
                    y));
        }
        coords.add(coords.get(0));
        return geometryFactory.createPolygon(coords.toArray(new Coordinate[coords.size()]));
    }

    private static Polygon densifyPolygon(final int factor, final Polygon polygon){
        Preconditions.checkArgument(factor >= 1);
        if (factor == 1) {
            return polygon;
        }

        Coordinate[] polyCords = polygon.getCoordinates();
        List<Coordinate> densifiedCoords = new ArrayList<Coordinate>(polyCords.length * factor);


        densifiedCoords.add(polyCords[0]);

        for (int i = 0; i < polyCords.length - 1; i++){
            LineString ls = geometryFactory.createLineString(new Coordinate[]{polyCords[i], polyCords[i + 1]});
            LengthIndexedLine indexedls = new LengthIndexedLine(ls);
            double lengthIncrement = ls.getLength() / (factor);
            for (int j = 1; j < factor; j++){
                densifiedCoords.add(indexedls.extractPoint(lengthIncrement * factor));
            }
            densifiedCoords.add(polyCords[i+1]);
        }

        return geometryFactory.createPolygon(densifiedCoords.toArray(new Coordinate[densifiedCoords.size()]));
    }
}
