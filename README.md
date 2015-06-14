# intersection-test
Intersection is at the core of many geospatial operations.  This project experiments with a few different permutations of JTS's prepared and regular geometry, with different geometry sizes for each.

# to run
```
mvn clean compile
mvn exec:exec
```
# results of run

## Preparing the geometry
JTS has a concept of [prepared geometries](http://lin-ear-th-inking.blogspot.com/2007/08/preparedgeometry-efficient-batch.html), which have some initial overhead of creation/indexing in tradeoff for faster relation operations (some - but intersects is one of the accelerated one).

So what kind of overhead are we looking at?

```
-----------------------------------------------
Sampled time to prepared geometry
Values are averaged for 100 repetitions
[NUM VERTICES] : [MSEC]  ± [RSD]
-----------------------------------------------
[100]          : 0.00184 ± 131.10%
[1,000]        : 0.00183 ± 164.59%
[10,000]       : 0.00095 ± 135.38%
[100,000]      : 0.00066 ± 74.20%
[1,000,000]    : 0.00117 ± 340.84%
[10,000,000]   : 0.00086 ± 445.26%
-----------------------------------------------
```
From this we can see that the time it takes to prepare a geometry (either with the PreparedGeometryFactory.create() method, or by the new method for PreparedGeometry) is pretty invariant with respect to number of vertices, at least up to 10,000,000 - and it's fast in an absolute sense as well. (units are in milliseconds).


## Intersecting with regular geometries

An intersection takes one geometry (called the query geometry here), and compares it to another geometry (called the target here), and tells us if they overlap.  The more complicated the geometry the longer it takes.  

Here we created random sets of geometries (both target and query) with different amounts of vertices, and measured the average time it takes to perform an intersection

```
-----------------------------------------------
Sampled time to intersect
Values grouped by # vertices for query and target
[TARGET][QUERY]  : [MSEC]  ± [RSD]
-----------------------------------------------
[500][500]       : 6.47448 ± 110.51%
[1,000][500]     : 2.26866 ± 125.66%
[10,000][500]    : 63.53468 ± 163.02%
[500][1000]      : 7.06141 ± 83.30%
[1,000][1000]    : 3.95251 ± 137.43%
[10,000][1000]   : 80.16658 ± 165.63%
[500][10,000]    : 147.31575 ± 55.39%
[1,000][10,000]  : 130.28493 ± 96.62%
[10,000][10,000] : 417.78303 ± 173.57%
-----------------------------------------------
```

Here we can see that we are already many order of magnitude above the cost of creating a PreparedGeometry.  
Additionally, as the the number of vertices in both the query and target goemtries increase we see a non trivial increase in time.   Comparing a 10,000 vertex geometry to a 10,000 vertex geometry takes almost half a second.  If we start looking at filtering geomtries in the millions range (not a huge GIS data set) we are already at 5 days.

Here's a plot of # vertices in the target geometry vs time, groupsed by # vertices in the query geometry:
![plot1](https://raw.githubusercontent.com/chrisbennight/intersection-test/master/src/main/resources/Plot%20target%20geom%20vs%20time%20by%20query%20geom%20-%20non-prepared.png)


Here's a plot of # vertices in the query geometry vs time, groupsed by # vertices in the target geometry:
![plot2](https://raw.githubusercontent.com/chrisbennight/intersection-test/master/src/main/resources/Plot%20query%20geom%20vs%20time%20by%20target%20geom%20-%20non-prepared.png)

So we can definitely see that #vertices in the query geometry has a definite upward trend.

One thing to note, on the ordering of the series, is that the randomly generated geometry sets are different for each collection.  Based on the ordering not following number of vertices it stands to reason that something else is impacting this here.  My working assumption is that the actual distribution of geometries is the causative factor here - probably in some cases geometries can be excluded based on envelope comparisons.   A better modification ot this test would be to generate one set of geometries, and increase the vertex count by densifying the geometry.  

## Intersecting with prepared geometries

Here we see the results of the same type of test, this time with a prepared query geometry.

```
-----------------------------------------------
Sampled time to intersect with prepared geometry
Values grouped by # vertices for query and target
[TARGET][QUERY]    : [MSEC]  ± [RSD]
-----------------------------------------------
[1,000][1,000]     : 3.41264 ± 262.72%
[10,000][1,000]    : 0.21383 ± 115.74%
[100,000][1,000]   : 1.19995 ± 111.06%
[1,000][10,000]    : 1.14917 ± 244.89%
[10,000][10,000]   : 0.66846 ± 248.22%
[100,000][10,000]  : 1.63555 ± 153.14%
[1,000][100,000]   : 7.60860 ± 270.40%
[10,000][100,000]  : 7.03418 ± 282.36%
[100,000][100,000] : 7.73182 ± 190.80%
-----------------------------------------------
```

On first run the results were soo much faster that we had to add another order of magnitude to the # of vertices to just see a difference.  (this goes up to a 100,000 x 100,000 vertex comparison;  the previous test only went up to 10,000 x 10,000).

One interesting thing to note - even at the much faster speed the time to compute an intersection is orders of magnitude slower than the time to prepare.


Here's a plot of # vertices in the target geometry vs time, groupsed by # vertices in the query geometry:
![plot3](https://raw.githubusercontent.com/chrisbennight/intersection-test/master/src/main/resources/Plot%20target%20geom%20vs%20time%20by%20query%20geom%20-%20prepared.png)


Here's a plot of # vertices in the query geometry vs time, groupsed by # vertices in the target geometry:
![plot4](https://raw.githubusercontent.com/chrisbennight/intersection-test/master/src/main/resources/Plot%20query%20geom%20vs%20time%20by%20target%20geom%20-%20prepared.png)

We see the same trend here as before - but note we are at a *much* faster and *much* larger number of vertex scale.  The pattern is similar, just much better (speed, scaling)

# Summary

At first glance PreparedGeometries seem to almost always be a "win" whenever at least one intersection is being performed.
If we note the relative standard deviation though, until we get to ~ 1000 vertices in one of the geometry the difference is within the error bars.  

When looking at the intersection performance, there's another interesting excerpt:

```
[TARGET][QUERY]    : [MSEC]  ± [RSD]
-----------------------------------------------
[100,000][1,000]   : 1.19995 ± 111.06%
[1,000][100,000]   : 7.60860 ± 270.40%
-----------------------------------------------
```

Since interection is invertable  (if A intersects B then B intersects A) this brings up the question of optimizing the case when one geometry has orders of magnitude more vertices than the other.  If we take at look at the time to create a new PreparedGeometry instance for 100,000 vertices

```
-----------------------------------------------
[NUM VERTICES] : [MSEC]  ± [RSD]
[100,000]      : 0.00066 ± 74.20%
-----------------------------------------------
```

it's basically irrelevant (with respect to speed).   So in the case above, where geometry A = 100,000 vertices, and geometry B = 1,000 vertices if we had geometry B prepared, it would still be a net win performance wise to prepare a new instance of geometry A and compare that way. I'm not sure why this is the case.

Also note that this is going to potentially result in lots of GC churn  (if we compare one geometry to millions, we can either prepare one geometry or millions) - and since we are only looking at msec of difference anyway the first garbage collection might use up our gains.




