package com.renard.ocr.documents.creation.crop;

import com.renard.ocr.BuildConfig;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

import static com.renard.ocr.documents.creation.crop.HighLightView.GROW_BOTTOM_EDGE;
import static com.renard.ocr.documents.creation.crop.HighLightView.GROW_LEFT_EDGE;
import static com.renard.ocr.documents.creation.crop.HighLightView.GROW_NONE;
import static com.renard.ocr.documents.creation.crop.HighLightView.GROW_RIGHT_EDGE;
import static com.renard.ocr.documents.creation.crop.HighLightView.GROW_TOP_EDGE;
import static com.renard.ocr.documents.creation.crop.HighLightView.MOVE;

/**
 * Created by renard.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class CroppingTrapezoidTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    private List<Point> getRectanglePoints() {
        List<Point> points = new ArrayList<Point>();
        points.add(new Point(10, 50));
        points.add(new Point(90, 50));
        points.add(new Point(50, 10));
        points.add(new Point(50, 90));
        points.add(new Point(10, 10));
        points.add(new Point(90, 10));
        points.add(new Point(10, 90));
        points.add(new Point(90, 90));
        return points;
    }

    @Test
    @Ignore
    public void testHitOutsideRectangle() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);
        List<Point> points = getRectanglePoints();
        for (Point p : points) {
            final int hit = trap.getHit(p.x, p.y, 0);
            Assert.assertEquals("testing " + p.toString(), GROW_NONE, hit);
        }
        for (Point p : points) {
            final int hit = trap.getHit(p.x, p.y, 5);
            Assert.assertEquals(GROW_NONE, hit);
        }
    }

    @Test
    @Ignore
    public void testHitEdgesOfRectangle() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);
        List<Point> points = getRectanglePoints();

        int hit = trap.getHit(points.get(0).x, points.get(0).y, 10);
        Assert.assertEquals(GROW_LEFT_EDGE, hit);

        hit = trap.getHit(points.get(1).x, points.get(1).y, 10);
        Assert.assertEquals(GROW_RIGHT_EDGE, hit);

        hit = trap.getHit(points.get(2).x, points.get(2).y, 10);
        Assert.assertEquals(GROW_TOP_EDGE, hit);

        hit = trap.getHit(points.get(3).x, points.get(3).y, 10);
        Assert.assertEquals(GROW_BOTTOM_EDGE, hit);

    }

    @Test
    @Ignore
    public void testHitCornersRectangle() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);
        List<Point> points = getRectanglePoints();

        int hit = trap.getHit(points.get(4).x, points.get(4).y, 15);
        Assert.assertEquals(GROW_LEFT_EDGE | GROW_TOP_EDGE, hit);

        hit = trap.getHit(points.get(5).x, points.get(5).y, 15);
        Assert.assertEquals(GROW_RIGHT_EDGE | GROW_TOP_EDGE, hit);

        hit = trap.getHit(points.get(6).x, points.get(6).y, 15);
        Assert.assertEquals(GROW_LEFT_EDGE | GROW_BOTTOM_EDGE, hit);

        hit = trap.getHit(points.get(7).x, points.get(7).y, 15);
        Assert.assertEquals(GROW_BOTTOM_EDGE | GROW_RIGHT_EDGE, hit);
    }

    @Test
    @Ignore
    public void testHitInsideRectangle() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);

        int hit = trap.getHit(30, 30, 0);
        Assert.assertEquals(MOVE, hit);

        hit = trap.getHit(50, 50, 29);
        Assert.assertEquals(MOVE, hit);

        hit = trap.getHit(78, 78, 1);
        Assert.assertEquals(MOVE, hit);

        hit = trap.getHit(22, 22, 1);
        Assert.assertEquals(MOVE, hit);
    }

    @Test
    @Ignore
    public void testGrowBy() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);
        trap.growBy(GROW_TOP_EDGE | GROW_RIGHT_EDGE, -30, 0);
        final Point bottomLeft = trap.getBottomLeft();
        Assert.assertEquals(bottomLeft.x, 20);
        Assert.assertEquals(bottomLeft.y, 80);

        Point topLeft = trap.getTopLeft();
        Assert.assertEquals(topLeft.x, 20);
        Assert.assertEquals(topLeft.y, 20);

        Point bottomRight = trap.getBottomRight();
        Assert.assertEquals(bottomRight.x, 80);
        Assert.assertEquals(bottomRight.y, 80);

        Point topRight = trap.getTopRight();
        Assert.assertEquals(70, topRight.x);
        Assert.assertEquals(20, topRight.y);

        trap.growBy(GROW_RIGHT_EDGE | GROW_BOTTOM_EDGE, -30, -30);
        bottomRight = trap.getBottomRight();
        Assert.assertEquals(bottomRight.x, 70);
        Assert.assertEquals(bottomRight.y, 70);

        trap.growBy(GROW_TOP_EDGE, 5, 5);
        topLeft = trap.getTopLeft();
        Assert.assertEquals(25, topLeft.x);
        Assert.assertEquals(20, topLeft.y);
        topRight = trap.getTopRight();
        Assert.assertEquals(75, topRight.x);
        Assert.assertEquals(20, topRight.y);
    }


    @Test
    @Ignore
    public void testHitEdgesOfTrapezoid() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);
        trap.growBy(GROW_TOP_EDGE | GROW_RIGHT_EDGE, -30, 0);

        int hit = trap.getHit(75, 50, 1);
        Assert.assertEquals(GROW_RIGHT_EDGE, hit);

        hit = trap.getHit(75, 30, 1);
        Assert.assertEquals(GROW_NONE, hit);

        hit = trap.getHit(55, 50, 1);
        Assert.assertEquals(MOVE, hit);

    }
}
