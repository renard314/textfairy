/*  This file is part of Text Fairy.
 
 Text Fairy is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Text Fairy is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Text Fairy.  If not, see <http://www.gnu.org/licenses/>.
 */
#include "blur_detect.h"
#include <algorithm>    // std::max
#include <math.h>       /* pow */
#include "RunningStats.h"
#include <string>       // std::string
#include <iostream>     // std::cout
#include <sstream>
using namespace std;


Pix* pixApplyThreshold(Pix* pixs, Pix* pixth, const l_uint32 tileSize){
	L_TIMER timer = startTimerNested();
	l_int32 w, h;
	ostringstream s;
	pixGetDimensions(pixs, &w, &h, NULL);
	l_int32 nx = L_MAX(1, w / tileSize);
	l_int32 ny = L_MAX(1, h / tileSize);
	Pix* pixt;
	PIXTILING* pt = pixTilingCreate(pixs, nx, ny, 0, 0, 0, 0);
	Pix* pixb = pixCreate(w, h, 1);
	l_uint32 val;
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			pixt = pixTilingGetTile(pt, i, j);
			pixGetPixel(pixth, j, i, &val);
			Pix* pixbTile = pixThresholdToBinary(pixt, val);
			pixTilingPaintTile(pixb, i, j, pixbTile, pt);
			pixDestroy(&pixt);
			pixDestroy(&pixbTile);
		}
	}
	pixTilingDestroy(&pt);
	//s << "local threshhold application: " << stopTimerNested(timer) << endl;
	printf("%s", s.str().c_str());
	return pixb;
}

Pix* binarizeEdge(Pix* pixs) {
	NUMA* histo = pixGetGrayHistogram(pixs, 8);
	NUMA* norm = numaNormalizeHistogram(histo, 1.0);
	l_float32 median, mean, variance;
	numaGetHistogramStats(norm, 0, 1, &mean, &median, NULL, &variance);
	l_int32 thresh = 0;
	if (variance < 1.0) {
		thresh = 3;
	} else {
		thresh = 4;
	}
	printf("mean = %f, median = %f, variance = %f, thresh = %i\n", mean, median,variance, thresh);

	Pix* pixForeground = pixThresholdToBinary(pixs, thresh);

	numaDestroy(&histo);
	numaDestroy(&norm);

	return pixForeground;

}


/**
 * determines and applies a threshold for each tile separately
 */
Pix* binarizeEdgeTiled(Pix* pixs, const l_uint32 tileSize, Pix** pixThresh) {
	L_TIMER timer = startTimerNested();
	l_int32 w, h;
	Pix* pixb;
	ostringstream s;

	pixGetDimensions(pixs, &w, &h, NULL);
	l_int32 nx = L_MAX(1, w / tileSize);
	l_int32 ny = L_MAX(1, h / tileSize);
	l_int32 ox = L_MAX(1,nx/6);
	l_int32 oy = L_MAX(1,ny/6);
	PIXTILING* pt = pixTilingCreate(pixs, nx, ny, 0, 0, ox, oy);
	Pix* pixth = pixCreate(nx, ny, 8);
	Pix* pixt;
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			pixt = pixTilingGetTile(pt, i, j);
			NUMA* na = pixGetGrayHistogram(pixt, 1);
			int thresh;
			numaSplitDistribution(na, 0.1, &thresh, NULL, NULL, NULL, NULL, NULL);
			if(thresh==1){
				thresh++;
			}
			numaDestroy(&na);
			pixSetPixel(pixth, j, i, thresh);
			pixDestroy(&pixt);
		}
	}
	pixTilingDestroy(&pt);
	s << "local threshhold determination: " << stopTimerNested(timer) << std::endl;
	//printf("%s", s.str().c_str());
	pixb = pixApplyThreshold(pixs,pixth,tileSize);
	if(pixThresh!=NULL){
		*pixThresh=pixClone(pixth);
	}
	pixDestroy(&pixth);

	return pixb;
}



void getValueBetweenTwoFixedColors(float value, int r, int g, int b, int &red, int &green, int &blue) {
  int bR = 255; int bG = 0; int bB=0;    // RGB for our 2nd color (red in this case).

  red   = (float)(bR - r) * value + r;      // Evaluated as -255*value + 255.
  green = (float)(bG - g) * value + g;      // Evaluates as 0.
  blue  = (float)(bB - b) * value + b;      // Evaluates as 255*value + 0.
}




Pix* pixMakeBlurMask(Pix* pixGrey, Pix* pixMedian, l_float32* blurValue, Pix** pixBinary) {
	l_int32    width, height, wpld, wplbx, wplby, wplm,wpls;
	l_int32    y, x, k;
	l_uint32  *datad, *databx,*databy, *datas,*datam, *lined, *linebx, *lineby, *lines, *linem;


	Pix* edgesx = pixSobelEdgeFilter(pixMedian, L_VERTICAL_EDGES);
	Pix* edgesy = pixSobelEdgeFilter(pixMedian, L_HORIZONTAL_EDGES);
	//Pix* test;
	//pixOtsuAdaptiveThreshold(edgesx, 1,1,1,1,0.5,NULL,&test);
	//pixWrite("otsu.png",test, IFF_PNG);

/*
	NUMA* na = pixGetGrayHistogram(pixMedian, 8);
	int thresh;
	numaSplitDistribution(na, 0.1, &thresh, NULL, NULL, NULL, NULL, NULL);
	numaDestroy(&na);
	Pix* test = pixThresholdToBinary(pixMedian,thresh);
	pixWrite("pixBinarMedian.png",test,IFF_PNG);
	*/



	width = pixGetWidth(edgesx);
	height = pixGetHeight(edgesx);
	l_uint8 tileSize = max(30,width/6);
	Pix* blurMeasure = pixCreate(width,height,8);
	//Pix* pixBinaryx = binarizeEdge(edgesx);
	//Pix* pixBinaryy = binarizeEdge(edgesy);
	Pix* pixBinaryx = binarizeEdgeTiled(edgesx,tileSize,NULL);
	Pix* pixBinaryy = binarizeEdgeTiled(edgesy,tileSize, NULL);
/*
	Pix* pixThinx= pixThin(pixBinaryx, L_THIN_BG, 4, 0);
	Pix* pixThiny = pixThin(pixBinaryy,L_THIN_BG, 4, 0);

	pixBinaryx = pixClone(pixThinx);
	pixBinaryy = pixClone(pixThiny);

	pixDestroy(&pixThinx);
	pixDestroy(&pixThiny);
*/
	if(pixBinary!=NULL){
		*pixBinary =pixAnd(NULL,pixBinaryx, pixBinaryy);
	}
	pixWrite("pixBinaryy.png",pixBinaryy,IFF_PNG);


    datas = pixGetData(pixGrey);
    datad = pixGetData(blurMeasure);
    databx = pixGetData(pixBinaryx);
    databy = pixGetData(pixBinaryy);
    datam = pixGetData(pixMedian);
    wpls = pixGetWpl(pixGrey);
    wpld = pixGetWpl(blurMeasure);
    wplbx = pixGetWpl(pixBinaryx);
    wplby = pixGetWpl(pixBinaryy);
    wplm = pixGetWpl(pixMedian);
    RunningStats stats;
    Numa* numaValues = numaCreate(0);
    l_int32 w = 2;
    l_int32 w2 = w*2;
    l_int32 w3 = 6;
    l_int32 w4 = w3*2;
    for (y = w4; y < height-w4; y++) {
        linem = datam + y * wplm;
        lined = datad + y * wpld;
        linebx = databx + y * wplbx;
        lineby = databy + y * wplby;
        lines = datas + y * wpls;
        for (x = w4; x < width-w4; x++) {
        	bool hasx = !GET_DATA_BIT(linebx, x);
        	bool hasy = !GET_DATA_BIT(lineby, x);
            if (hasx||hasy) {
            	l_uint32 domx = 0;
            	l_uint32 contrastx = 0;
            	l_uint32 domy = 0;
            	l_uint32 contrasty = 0;
            	for(k = -w;k <=w; k++){
            		//vertical dom
            		if(hasy){
            			l_uint32 row = y+k;
						l_uint8 y1 = GET_DATA_BYTE(datam + (row+w3) * wplm,x);
						l_uint8 y2 = GET_DATA_BYTE(datam + row * wplm,x);
						l_uint8 y3 = GET_DATA_BYTE(datam + (row-w3) * wplm,x);
						domy+=abs((y1-y2)-(y2-y3));
						y1 = GET_DATA_BYTE(datam + (row-1) * wpls,x);
						y2 = GET_DATA_BYTE(datam + (row) * wpls,x);
						contrasty+= abs(y1-y2);
	                	if(x==44 && y==33){
	                		//printf("(%i,%i) =  %i\n", x, row,y2);
	                	}

            		}

                    //horizontal dom
            		if(hasx){
            			l_uint32 column = x+k;
            			l_uint8 x1 = GET_DATA_BYTE(linem,column);
            			l_uint8 x2 = GET_DATA_BYTE(linem,column+w3);
            			l_uint8 x3 = GET_DATA_BYTE(linem,column-w3);

            			domx+= abs((x2 - x1)-(x1 - x3));

						x1 = GET_DATA_BYTE(linem,column);
						x2 = GET_DATA_BYTE(linem,column-1);
						contrastx+= abs(x1-x2);
            		}


            	}
            	//if(x==44 && y==33){
            	//}
            	double sharpnessx = 0;
            	double sharpnessy = 0;
            	double sharpness = 0;
            	if(contrastx>0){
    				sharpnessx = (((float)domx)/((float)contrastx));
            	}
            	if(contrasty>0){
    				sharpnessy = (((float)domy)/((float)contrasty));
            	}

            	if(sharpnessx>0 && sharpnessy>0){
            		sharpness = sqrt(sharpnessx*sharpnessx + sharpnessy * sharpnessy);
            		//printf("x= %f, y = %f, total = %f, hasx=%i, hasy=%i\n",sharpnessx,sharpnessy,sharpness, hasx, hasy);
            	} else if(sharpnessx>0){
            		sharpness= sharpnessx;
            	} else {
            		sharpness= sharpnessy;
            	}
            	if(domy>0){
            		//printf("(%i,%i) -> blur = %f, d = %i, c = %i\n", x,y,sharpnessy, domy, contrasty);
            	}

				numaAddNumber(numaValues,sharpness);
				stats.Push(sharpness);
				//3= sharp, 1.2 not sharp
				double maxSharpness = 5.0;
				double minSharpness = 1.0;
				double clamped = min(maxSharpness,sharpness);
				clamped = max(minSharpness,clamped);
				//scale range to 0-1
				clamped = 1-(clamped-minSharpness)/(maxSharpness-minSharpness);
				//clamped = pow(clamped,2);
				//float val = clamped*255;
				float val = clamped*255;
				SET_DATA_BYTE(lined,x, val);
            }
        }
    }

    if(blurValue!=NULL){
    	*blurValue = stats.Mean();
    	//numaGetRankValue(numaValues,0.5,NULL,0,blurValue);
    }
    pixWrite("pixBinaryx.png",pixBinaryx, IFF_PNG);
    pixWrite("edgesx.png",edgesx, IFF_PNG);

	pixDestroy(&pixBinaryx);
	pixDestroy(&pixBinaryy);
	pixDestroy(&edgesx);
	pixDestroy(&edgesy);
	return blurMeasure;
}

void pixTintMasked(Pix* pixd, Pix* pixmask) {
	l_int32    width, height, wpld, wplm;
	l_int32    y, x, rval, gval, bval;
	l_uint32  *datad, *lined, *datam, *linem;
	int red, green, blue;
    datad = pixGetData(pixd);
    wpld = pixGetWpl(pixd);
    datam = pixGetData(pixmask);
    wplm = pixGetWpl(pixmask);


	width = pixGetWidth(pixd);
	height = pixGetHeight(pixd);
	for (y = 0; y < height; y++) {
		lined = datad + y * wpld;
		linem = datam + y * wplm;
		for (x = 0; x < width; x++) {
			extractRGBValues(lined[x], &rval, &gval, &bval);
			l_uint8 blurriness=GET_DATA_BYTE(linem,x);
			if(blurriness>0){
				float frac = ((float)blurriness)/255.0;
				getValueBetweenTwoFixedColors(frac,rval,gval,bval,red, green, blue);
				composeRGBPixel(red, green, blue, lined + x);
			}
		}
	}
}




