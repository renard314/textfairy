package com.renard.ocr.cropimage;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.renard.CustomRobolectricRunner;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static com.renard.ocr.cropimage.HighlightView.GROW_BOTTOM_EDGE;
import static com.renard.ocr.cropimage.HighlightView.GROW_LEFT_EDGE;
import static com.renard.ocr.cropimage.HighlightView.GROW_NONE;
import static com.renard.ocr.cropimage.HighlightView.GROW_RIGHT_EDGE;
import static com.renard.ocr.cropimage.HighlightView.GROW_TOP_EDGE;
import static com.renard.ocr.cropimage.HighlightView.MOVE;

/**
 * Created by renard on 26/02/15.
 */
@RunWith(CustomRobolectricRunner.class)
public class CroppingTrapezoidTest {

    private List<Point> getRectanglePoints() {
        List<Point> points = new ArrayList<>();
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
    public void testHitInsideRectangle() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);

        int hit = trap.getHit(30, 30, 0);
        Assert.assertEquals(MOVE, hit);

        hit = trap.getHit(50, 50, 29);
        Assert.assertEquals( MOVE, hit);

        hit = trap.getHit(78, 78, 1);
        Assert.assertEquals(MOVE, hit);

        hit = trap.getHit(22, 22, 1);
        Assert.assertEquals( MOVE, hit);
    }

    @Test
    public void testGrowBy() {
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);
        trap.growBy(GROW_TOP_EDGE|GROW_RIGHT_EDGE, -30,0);
        final Point bottomLeft = trap.getBottomLeft();
        Assert.assertEquals(bottomLeft.x,20);
        Assert.assertEquals(bottomLeft.y,80);

        Point topLeft = trap.getTopLeft();
        Assert.assertEquals(topLeft.x,20);
        Assert.assertEquals(topLeft.y,20);

        Point bottomRight = trap.getBottomRight();
        Assert.assertEquals(bottomRight.x,80);
        Assert.assertEquals(bottomRight.y,80);

        Point topRight = trap.getTopRight();
        Assert.assertEquals(topRight.x,50);
        Assert.assertEquals(topRight.y,20);

        trap.growBy(GROW_RIGHT_EDGE|GROW_BOTTOM_EDGE,-30,-30);
        bottomRight=trap.getBottomRight();
        Assert.assertEquals(bottomRight.x, 50);
        Assert.assertEquals(bottomRight.y,50);

        trap.growBy(GROW_TOP_EDGE,5,5);
        topLeft = trap.getTopLeft();
        Assert.assertEquals(topLeft.x,25);
        Assert.assertEquals(topLeft.y,25);
        topRight = trap.getTopRight();
        Assert.assertEquals(topRight.x, 55);
        Assert.assertEquals(topRight.y,25);
    }


    @Test
    public void testHitEdgesOfTrapezoid(){
        Rect imageRect = new Rect(0, 0, 100, 100);
        RectF cropRect = new RectF(20, 20, 80, 80);
        CroppingTrapezoid trap = new CroppingTrapezoid(cropRect, imageRect);
        trap.growBy(GROW_TOP_EDGE|GROW_RIGHT_EDGE, -30,0);

        int hit = trap.getHit(65,50,1);
        Assert.assertEquals(GROW_RIGHT_EDGE,hit);

        hit = trap.getHit(65,30,1);
        Assert.assertEquals(GROW_NONE,hit);

        hit = trap.getHit(65,70,1);
        Assert.assertEquals(MOVE,hit);

    }
}
