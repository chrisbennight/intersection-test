package com.bennight.bench.intersection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by bennight on 6/13/2015.
 */
public class app {

    private static final Random RND = new Random(8675309);
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();

    public static void main(String[] args) throws IOException {

        Map<Integer, DescriptiveStatistics> timeToPrepare = new HashMap<>();
        Map<Integer, Map<Integer, DescriptiveStatistics>> intersects = new HashMap<>();
        Map<Integer, Map<Integer, DescriptiveStatistics>> preparedIntersects = new HashMap<>();


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


        //Test to find the time to perform and intersection on non prepared geometries, as a function of # vetices

        File preparedIntersectsFile = new File("preparedIntersects.csv");
        BufferedWriter preparedIntersectsBW = new BufferedWriter(new FileWriter(preparedIntersectsFile));
        preparedIntersectsBW.write("#Target Vertices,#Query Vertices,MSEC,STDEV\r\n");

        for (int i = 0; i <= 100000; i += 1000) {
            if (i < 4) continue;
            List<Polygon> polygons = generatePolyCollections(10, i);
            preparedIntersects.put(i, new HashMap<Integer, DescriptiveStatistics>());
            for (int j = 0; j <= 100000; j += 1000) {
                if (j < 4) continue;
                queryGeom = createPolygon(j, 0, 0, 60);
                preparedQueryGeom = (PreparedPolygon) preparedGeometryFactory.create(queryGeom);
                preparedIntersects.get(i).put(j, new DescriptiveStatistics());
                for (Polygon p : polygons){
                    init = System.nanoTime();
                    preparedQueryGeom.intersects(p);
                    fin = System.nanoTime() - init;
                    preparedIntersects.get(i).get(j).addValue(fin);
                }
                preparedIntersectsBW.write(i + "," + j +  "," + preparedIntersects.get(i).get(j).getMean() / 1000000d + "," + preparedIntersects.get(i).get(j).getStandardDeviation() / 1000000d + "\r\n");
            }
        }

        preparedIntersectsBW.close();



        System.out.println("-----------------------------------------------");
        System.out.println("Sampled time to intersect with prepared geometry");
        System.out.println("Values grouped by # vertices for query and target");
        System.out.println("[TARGET][QUERY]    : [MSEC]  \u00B1 [RSD]");
        System.out.println("-----------------------------------------------");
        System.out.println(String.format("[1,000][1,000]     : %.5f \u00B1 %.2f%%", preparedIntersects.get(1000).get(1000).getMean() / 1000000d, preparedIntersects.get(1000).get(1000).getStandardDeviation() /  preparedIntersects.get(1000).get(1000).getMean() * 100 ));
        System.out.println(String.format("[10,000][1,000]    : %.5f \u00B1 %.2f%%", preparedIntersects.get(10000).get(1000).getMean() / 1000000d, preparedIntersects.get(10000).get(1000).getStandardDeviation() /  preparedIntersects.get(10000).get(1000).getMean() * 100 ));
        System.out.println(String.format("[100,000][1,000]   : %.5f \u00B1 %.2f%%", preparedIntersects.get(100000).get(1000).getMean() / 1000000d, preparedIntersects.get(100000).get(1000).getStandardDeviation() /  preparedIntersects.get(100000).get(1000).getMean() * 100 ));
        System.out.println(String.format("[1,000][10,000]    : %.5f \u00B1 %.2f%%", preparedIntersects.get(1000).get(10000).getMean() / 1000000d, preparedIntersects.get(1000).get(10000).getStandardDeviation() /  preparedIntersects.get(1000).get(10000).getMean() * 100 ));
        System.out.println(String.format("[10,000][10,000]   : %.5f \u00B1 %.2f%%", preparedIntersects.get(10000).get(10000).getMean() / 1000000d, preparedIntersects.get(10000).get(10000).getStandardDeviation() /  preparedIntersects.get(10000).get(10000).getMean() * 100 ));
        System.out.println(String.format("[100,000][10,000]  : %.5f \u00B1 %.2f%%", preparedIntersects.get(100000).get(10000).getMean() / 1000000d, preparedIntersects.get(100000).get(10000).getStandardDeviation() /  preparedIntersects.get(100000).get(10000).getMean() * 100 ));
        System.out.println(String.format("[1,000][100,000]   : %.5f \u00B1 %.2f%%", preparedIntersects.get(1000).get(100000).getMean() / 1000000d, preparedIntersects.get(1000).get(100000).getStandardDeviation() /  preparedIntersects.get(1000).get(100000).getMean() * 100 ));
        System.out.println(String.format("[10,000][100,000]  : %.5f \u00B1 %.2f%%", preparedIntersects.get(10000).get(100000).getMean() / 1000000d, preparedIntersects.get(10000).get(100000).getStandardDeviation() /  preparedIntersects.get(10000).get(100000).getMean() * 100 ));
        System.out.println(String.format("[100,000][100,000] : %.5f \u00B1 %.2f%%", preparedIntersects.get(100000).get(100000).getMean() / 1000000d, preparedIntersects.get(100000).get(100000).getStandardDeviation() /  preparedIntersects.get(100000).get(100000).getMean() * 100 ));
        System.out.println("-----------------------------------------------");




        //Test to find the time to perform and intersection on non prepared geometries, as a function of # vetices

        File intersectsFile = new File("intersects.csv");
        BufferedWriter intersectsBW = new BufferedWriter(new FileWriter(intersectsFile));
        intersectsBW.write("#Target Vertices,#Query Vertices,MSEC,STDEV\r\n");

        for (int i = 0; i <= 10000; i += 500) {
            if (i < 4) continue;
            List<Polygon> polygons = generatePolyCollections(10, i);
            intersects.put(i, new HashMap<Integer, DescriptiveStatistics>());
            for (int j = 0; j <= 10000; j += 500) {
                if (j < 4) continue;
                queryGeom = createPolygon(j, 0, 0, 60);
                intersects.get(i).put(j, new DescriptiveStatistics());
                for (Polygon p : polygons){
                    init = System.nanoTime();
                    queryGeom.intersects(p);
                    fin = System.nanoTime() - init;
                    intersects.get(i).get(j).addValue(fin);
                }
                intersectsBW.write(i + "," + j + ","  + intersects.get(i).get(j).getMean() / 1000000d + "," + intersects.get(i).get(j).getStandardDeviation() / 1000000d + "\r\n");
            }
        }

        intersectsBW.close();

        System.out.println("-----------------------------------------------");
        System.out.println("Sampled time to intersect");
        System.out.println("Values grouped by # vertices for query and target");
        System.out.println("[TARGET][QUERY]  : [MSEC]  \u00B1 [RSD]");
        System.out.println("-----------------------------------------------");
        System.out.println(String.format("[500][500]       : %.5f \u00B1 %.2f%%", intersects.get(500).get(500).getMean() / 1000000d, intersects.get(500).get(500).getStandardDeviation() /  intersects.get(500).get(500).getMean() * 100 ));
        System.out.println(String.format("[1,000][500]     : %.5f \u00B1 %.2f%%", intersects.get(1000).get(500).getMean() / 1000000d, intersects.get(1000).get(500).getStandardDeviation() /  intersects.get(1000).get(500).getMean() * 100 ));
        System.out.println(String.format("[10,000][500]    : %.5f \u00B1 %.2f%%", intersects.get(10000).get(500).getMean() / 1000000d, intersects.get(10000).get(500).getStandardDeviation() /  intersects.get(10000).get(500).getMean() * 100 ));
        System.out.println(String.format("[500][1000]      : %.5f \u00B1 %.2f%%", intersects.get(500).get(1000).getMean() / 1000000d, intersects.get(500).get(1000).getStandardDeviation() /  intersects.get(500).get(1000).getMean() * 100 ));
        System.out.println(String.format("[1,000][1000]    : %.5f \u00B1 %.2f%%", intersects.get(1000).get(1000).getMean() / 1000000d, intersects.get(1000).get(1000).getStandardDeviation() /  intersects.get(1000).get(1000).getMean() * 100 ));
        System.out.println(String.format("[10,000][1000]   : %.5f \u00B1 %.2f%%", intersects.get(10000).get(1000).getMean() / 1000000d, intersects.get(10000).get(1000).getStandardDeviation() /  intersects.get(10000).get(1000).getMean() * 100 ));
        System.out.println(String.format("[500][10,000]    : %.5f \u00B1 %.2f%%", intersects.get(500).get(10000).getMean() / 1000000d, intersects.get(500).get(10000).getStandardDeviation() /  intersects.get(500).get(10000).getMean() * 100 ));
        System.out.println(String.format("[1,000][10,000]  : %.5f \u00B1 %.2f%%", intersects.get(1000).get(10000).getMean() / 1000000d, intersects.get(1000).get(10000).getStandardDeviation() /  intersects.get(1000).get(10000).getMean() * 100 ));
        System.out.println(String.format("[10,000][10,000] : %.5f \u00B1 %.2f%%", intersects.get(10000).get(10000).getMean() / 1000000d, intersects.get(10000).get(10000).getStandardDeviation() /  intersects.get(10000).get(10000).getMean() * 100 ));
        System.out.println("-----------------------------------------------");
    }




    private static List<Polygon>  generatePolyCollections(int numPolygons, int numPoints){
        List<Polygon> polys = new ArrayList<>(numPolygons);

        for (int i = 0; i < numPolygons; i++){
            polys.add(createPolygon(numPoints, RND.nextInt(330) - 130, RND.nextInt(130) - 40, 40));
        }

        return polys;

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
}
