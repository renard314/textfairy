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
#include "PixBlurDetect.h"
#include <algorithm>    // std::max
#include <math.h>       /* pow */
#include "RunningStats.h"
#include <string>       // std::string
#include <iostream>     // std::cout
#include <sstream>
using namespace std;


PixBlurDetect::PixBlurDetect(bool debug) {
	mDebug= debug;
}

PixBlurDetect::~PixBlurDetect() {
}


Pix* PixBlurDetect::makeBlurIndicator(Pix* pixOrg, l_float32* blurValue) {
	Pix* pixGrey;
	switch(pixGetDepth(pixOrg)){
		case 1:
			pixGrey = pixConvert1To8(NULL,pixOrg,0,255);
			break;
		case 8:
			pixGrey = pixClone(pixOrg);
			break;
		case 32:
			pixGrey = pixConvertRGBToGrayFast(pixOrg);
			break;
	}
	L_TIMER timer;
	if(mDebug){
		timer = startTimerNested();
	}
	//Pix* pixMedian = pixMedianFilter(pixGrey,4,4);
	Pix* pixMedian = pixClone(pixGrey);
	if(mDebug){
		printf("%s, median: %f\n", __FUNCTION__, stopTimerNested(timer));
		timer = startTimerNested();
	}
	Pix* pixBinaryEdges;
	Pix* blurMeasure = pixMakeBlurMask(pixGrey, pixMedian, blurValue, &pixBinaryEdges);

	if(mDebug){
		printf("%s, blur mask: %f\n", __FUNCTION__, stopTimerNested(timer));
		timer = startTimerNested();
	}

	//Use blur mask to paint the edge mask to indicate blurry regions.
	pixInvert(pixBinaryEdges,pixBinaryEdges);
	Pixa* componentEdgeMask;
	Boxa* boxa =pixConnCompPixa(pixBinaryEdges,&componentEdgeMask,4);
	Pixa* componentBlurMask = pixaCreateFromBoxa(blurMeasure,boxa,NULL);
	l_int32 compCount = pixaGetCount(componentEdgeMask);
	for(int i = 0; i<compCount; i++) {
		Pix* pixBlurComp = pixaGetPix(componentBlurMask,i,L_CLONE);
		Pix* pixEdgeMask = pixaGetPix(componentEdgeMask,i,L_CLONE);
		l_float32 mean;
		l_uint32 grayValue = 0;
		l_uint32 error = pixGetAverage(pixBlurComp,&mean);
		if(!error){
			grayValue = lept_roundftoi(mean);
			pixClearAll(pixBlurComp);
			pixSetMasked(pixBlurComp,pixEdgeMask,grayValue);
		}

		pixDestroy(&pixEdgeMask);
		pixDestroy(&pixBlurComp);
	}
	Pix* test = pixaDisplayOnColor(componentBlurMask,0,0,0);

	Pix* pixBlendMask = pixBlockconvGray(test,NULL,2,2);
	Pix* pixBlended = pixConvert8To32(pixGrey);
	pixTintMasked(pixBlended,pixBlendMask);
	if(mDebug){
		printf("paint mask: %f\n", stopTimerNested(timer));
	}
	/*
	pixWrite("blurMeasure.png",blurMeasure, IFF_PNG);
	pixWrite("pixBinaryEdges.png",pixBinaryEdges, IFF_PNG);
	pixWrite("meanBlurMask.png",test, IFF_PNG);
    pixWrite("mask.png",blurMeasure, IFF_PNG);
    pixWrite("blended.png",pixBlended, IFF_PNG);
    pixWrite("textEdges.png",pixBinaryEdges, IFF_PNG);
    */
	boxaDestroy(&boxa);
    pixDestroy(&test);
    pixDestroy(&pixMedian);
    pixDestroy(&pixBinaryEdges);
    pixaDestroy(&componentEdgeMask);
    pixaDestroy(&componentBlurMask);
	pixDestroy(&pixBlendMask);
	pixDestroy(&pixGrey);
	pixDestroy(&blurMeasure);
	return pixBlended;
}

l_int32 PixBlurDetect::pixGetAverage(PIX *pixs,  l_float32  *pval) {
l_int32    i, j, w, h, d, wplg, val, count;
l_uint32  *datag, *lineg;
l_float64  sumave, summs, ave;
PIX       *pixg;

    PROCNAME("pixGetAverage");

    if (!pval)
        return ERROR_INT("&val not defined", procName, 1);
    *pval = 0.0;
    if (!pixs)
        return ERROR_INT("pixs not defined", procName, 1);
    d = pixGetDepth(pixs);
    if (d != 8 )
        return ERROR_INT("pixs not 8", procName, 1);

    if (pixGetColormap(pixs))
        pixg = pixRemoveColormap(pixs, REMOVE_CMAP_TO_GRAYSCALE);
    else
        pixg = pixClone(pixs);

    pixGetDimensions(pixg, &w, &h, &d);
    datag = pixGetData(pixg);
    wplg = pixGetWpl(pixg);

    sumave = summs = 0.0;
    count = 0;
	for (i = 0; i < h; i++) {
		lineg = datag + i * wplg;
		for (j = 0; j < w; j++) {
			val = GET_DATA_BYTE(lineg, j);
			if(val>0){
				sumave += val;
				count++;
			}
		}
	}


    pixDestroy(&pixg);
    if (count == 0) {
        return 1;
    }
    ave = sumave / (l_float64)count;
	*pval = (l_float32)ave;
    return 0;
}

Pix* PixBlurDetect::makeEdgeMask(Pix* pixs) {
	Pix* pixConv = pixBlockconvGray(pixs, NULL, 4, 1);
	Pix* pixConvEdges = pixTwoSidedEdgeFilter(pixConv,L_VERTICAL_EDGES);
	NUMA* histo = pixGetGrayHistogram(pixConvEdges, 8);
	NUMA* norm = numaNormalizeHistogram(histo, 1.0);
	l_float32 median, mean, variance;
	numaGetHistogramStats(norm, 0, 1, &mean, &median, NULL, &variance);
	l_int32 thresh = 0;
	double stdev  = sqrt(variance);
	//printf("mean=%f, stdev=%f\n",mean, stdev);
	if (stdev < 1.5) {
		thresh = 1;
	} else {
		thresh = 2;
	}
	//pixWrite("pixConvEdges.png",pixConvEdges,IFF_PNG);
	Pix* pixForeground = pixThresholdToBinary(pixConvEdges, thresh);
	pixDestroy(&pixConvEdges);
	pixDestroy(&pixConv);
	numaDestroy(&histo);
	numaDestroy(&norm);
	return pixForeground;
}


void PixBlurDetect::getValueBetweenTwoFixedColors(float value, int r, int g, int b, int &red, int &green, int &blue) {
  int bR = 255; int bG = 0; int bB=0;    // RGB for our 2nd color (red in this case).

  red   = (float)(bR - r) * value + r;      // Evaluated as -255*value + 255.
  green = (float)(bG - g) * value + g;      // Evaluates as 0.
  blue  = (float)(bB - b) * value + b;      // Evaluates as 255*value + 0.
}




Pix* PixBlurDetect::pixMakeBlurMask(Pix* pixGrey, Pix* pixMedian, l_float32* blurValue, Pix** pixBinary) {
	l_int32    width, height, wpld, wplbx, wplm;
	l_int32    y, x;
	l_uint32  *datad, *databx,*datam, *lined, *linebx, *linem;
	width = pixGetWidth(pixMedian);
	height = pixGetHeight(pixMedian);
	Pix* blurMeasure = pixCreate(width,height,8);
	Pix* pixBinaryx = makeEdgeMask(pixMedian);
	pixDilateBrickDwa(pixBinaryx,pixBinaryx,3,3);
	//pixCloseBrickDwa(pixBinaryx,pixBinaryx,3,3);
	if(pixBinary!=NULL){
		*pixBinary =pixClone(pixBinaryx);
	}

    datad = pixGetData(blurMeasure);
    databx = pixGetData(pixBinaryx);
    datam = pixGetData(pixMedian);
    wpld = pixGetWpl(blurMeasure);
    wplbx = pixGetWpl(pixBinaryx);
    wplm = pixGetWpl(pixMedian);
    RunningStats stats;
    for (y = 1; y < height-1; y++) {
        linem = datam + y * wplm;
        lined = datad + y * wpld;
        linebx = databx + y * wplbx;
        for (x = 1; x < width-1; x++) {
        	bool hasx = !GET_DATA_BIT(linebx, x);
            if (hasx) {
        		l_int32 right;
                pixGetLastOffPixelInRun(pixBinaryx, x, y, L_FROM_LEFT, &right);
        		l_uint8 edgeWidth = (right - x)+1;
                l_uint8 leftColor = GET_DATA_BYTE(datam + (y) * wplm,right+1);
				l_uint8 rightColor = GET_DATA_BYTE(datam + (y) * wplm,x-1);
                int intensity = abs((int)(rightColor-leftColor));
                double slope = (intensity/edgeWidth)/255.0;
                //printf("(%i,%i) with=%i, intensity diff = %i, slope=%f\n", x,y, edgeWidth, intensity, slope);
				stats.Push(slope);
				x=right;

				double maxSharpness = 0.040;
				double minSharpness = 0.01;
				double clamped = min(maxSharpness,slope);
				clamped = max(minSharpness,clamped);
				//scale range to 0-1
				clamped = 1-(clamped-minSharpness)/(maxSharpness-minSharpness);
				clamped = pow(clamped,5);
				//float val = clamped*255;
				double val = max(1.0,clamped*255);
				//printf("slope = %f -> %f\n",slope,val);
				SET_DATA_BYTE(lined,x, val);
            }
        }
    }

    if(blurValue!=NULL){
    	*blurValue = stats.Mean();
    }
	pixDestroy(&pixBinaryx);
	return blurMeasure;
}

void PixBlurDetect::pixTintMasked(Pix* pixd, Pix* pixmask) {
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




