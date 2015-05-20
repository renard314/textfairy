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
	Pix* pixScaled;
	if(pixGetWidth(pixGrey)>=800){
	    pixScaled = pixScaleBySamplingToSize(pixGrey,800,0);
	} else {
		pixScaled = pixClone(pixGrey);
	}

	L_TIMER timer;
	if(mDebug){
		timer = startTimerNested();
	}
	//Pix* pixMedian = pixMedianFilter(pixScaled,4,4);
	Pix* pixMedian = pixClone(pixScaled);
	if(mDebug){
		printf("%s, median: %f\n", __FUNCTION__, stopTimerNested(timer));
		timer = startTimerNested();
	}
	Pix* pixBinaryEdges;
	Pix* blurMeasure = pixMakeBlurMask(pixScaled, pixMedian, blurValue, &pixBinaryEdges);

	if(mDebug){
		Pix* pixEdgeMasked = pixCopy(NULL,pixScaled);
		pixSetMasked(pixEdgeMasked,pixBinaryEdges,255);
		pixWrite("pixEdgesMasked.png",pixEdgeMasked,IFF_PNG);
		pixDestroy(&pixEdgeMasked);
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
	Pix* pixBlended = pixConvert8To32(pixScaled);
	pixTintMasked(pixBlended,pixBlendMask);
	if(mDebug){
		printf("paint mask: %f\n", stopTimerNested(timer));
	}
	pixWrite("blurMeasure.png",blurMeasure, IFF_PNG);
	pixWrite("pixBinaryEdges.png",pixBinaryEdges, IFF_PNG);
	pixWrite("meanBlurMask.png",pixBlendMask, IFF_PNG);
    pixWrite("mask.png",blurMeasure, IFF_PNG);
    pixWrite("blended.png",pixBlended, IFF_PNG);
    pixWrite("textEdges.png",pixBinaryEdges, IFF_PNG);
	boxaDestroy(&boxa);
    pixDestroy(&test);
    pixDestroy(&pixMedian);
    pixDestroy(&pixBinaryEdges);
    pixaDestroy(&componentEdgeMask);
    pixaDestroy(&componentBlurMask);
	pixDestroy(&pixBlendMask);
	pixDestroy(&pixGrey);
	pixDestroy(&pixScaled);
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
	//pixContrastTRC(pixs,pixs,1);
	Pix* pixConv = pixBlockconvGray(pixs, NULL, 0, 2);
	Pix* pixConvEdges = pixTwoSidedEdgeFilter(pixConv,L_HORIZONTAL_EDGES);
	//Pix* pixConvEdges = pixSobelEdgeFilter(pixConv,L_HORIZONTAL_EDGES);

	NUMA* histo = pixGetGrayHistogram(pixConvEdges, 8);
	l_int32 thresh = 2;
	numaSplitDistribution(histo,0.01,&thresh,NULL,NULL,NULL,NULL,NULL);
	if(mDebug){
		printf("thresh=%i\n",thresh);
	}
	Pix* pixForeground = pixThresholdToBinary(pixConvEdges, thresh);
	//pixWrite("pixForeground.png",pixForeground,IFF_PNG);
	//pixWrite("pixConvEdges.png",pixConvEdges,IFF_PNG);
	pixDestroy(&pixConvEdges);
	pixDestroy(&pixConv);
	numaDestroy(&histo);
	return pixForeground;
}


void PixBlurDetect::getValueBetweenTwoFixedColors(float value, int h1, int s1, int v1, int h2, int s2, int v2, int &red, int &green, int &blue) {

    // adjust so that the shortest path on the color wheel will be taken
    if (h1 - h2 > 120) {
        h2 += 240;
    } else if (h2 - h1 > 120) {
        h1 += 240;
    }

    // Interpolate using calculated ratio
    int h3 = (h2-h1) * (value) + h1;
    int s3 = (s2-s1) * (value) + s1;
    int v3 = (v2-v1) * (value) + v1;
    convertHSVToRGB(h3,s3,v3,&red,&green,&blue);
}




Pix* PixBlurDetect::pixMakeBlurMask(Pix* pixGrey, Pix* pixMedian, l_float32* blurValue, Pix** pixBinary) {
	l_int32    width, height, wpld, wplbx, wplm;
	l_int32    y, x;
	l_uint32  *datad, *databx,*datam;
	width = pixGetWidth(pixMedian);
	height = pixGetHeight(pixMedian);
	Pix* blurMeasure = pixCreate(width,height,8);
	Pix* pixBinaryx = makeEdgeMask(pixMedian);
	//pixBinaryx = pixMorphCompSequenceDwa(pixBinaryx,"o1.4+c1.3",0);
	//pixDilateBrickDwa(pixBinaryx,pixBinaryx,1,3);
	//pixCloseBrickDwa(pixBinaryx,pixBinaryx,1,3);
	if(pixBinary!=NULL){
		*pixBinary =pixCopy(NULL,pixBinaryx);
	}

    datad = pixGetData(blurMeasure);
    databx = pixGetData(pixBinaryx);
    datam = pixGetData(pixMedian);
    wpld = pixGetWpl(blurMeasure);
    wplbx = pixGetWpl(pixBinaryx);
    wplm = pixGetWpl(pixMedian);
    RunningStats stats;
    for (x = 1; x < width-1; x++) {
        for (y = 1; y < height-1; y++) {
        	bool hasy = !GET_DATA_BIT(databx + y * wplbx, x);
            if (hasy) {
        		l_int32 bottom;
                pixGetLastOffPixelInRun(pixBinaryx, x, y, L_FROM_TOP, &bottom);
        		l_uint8 edgeWidth = (bottom - y)+1;
                l_uint8 leftColor = GET_DATA_BYTE(datam + (y-1) * wplm,x);
				l_uint8 rightColor = GET_DATA_BYTE(datam + (bottom+1) * wplm,x);
                int intensity = abs((int)(rightColor-leftColor));
                double slope = (intensity/edgeWidth)/255.0;
				stats.Push(slope);
				for(int i = y; i<=bottom; i++){
					SET_DATA_BIT(databx + i * wplbx,x);
				}

				double maxSharpness = 0.045;
				double minSharpness = 0.03;
				double clamped = min(maxSharpness,slope);
				clamped = max(minSharpness,clamped);
				//scale range to 0-1
				clamped = 1-(clamped-minSharpness)/(maxSharpness-minSharpness);
				//clamped = pow(clamped,5);
				//float val = clamped*255;
				double val = max(1.0,clamped*255);
				//printf("slope = %f -> %f; intensity = %i, length = %i\n",slope,val,intensity,edgeWidth);
				SET_DATA_BYTE(datad + y * wpld,x, val);
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
	int red = 0;
	int green = 255;
	int blue =0;
    datad = pixGetData(pixd);
    wpld = pixGetWpl(pixd);
    datam = pixGetData(pixmask);
    wplm = pixGetWpl(pixmask);


	width = pixGetWidth(pixd);
	height = pixGetHeight(pixd);
	int r1 = 40;
	int g1= 110;
	int b1= 0;
	int r2 = 255;
	int g2= 0;
	int b2= 0;
	int h1, s1, v1, h2, s2, v2;
	convertRGBToHSV(r1,g1,b1,&h1, &s1, &v1);
	convertRGBToHSV(r2,g2,b2,&h2, &s2, &v2);

	for (y = 0; y < height; y++) {
		lined = datad + y * wpld;
		linem = datam + y * wplm;
		for (x = 0; x < width; x++) {
			extractRGBValues(lined[x], &rval, &gval, &bval);
			l_uint8 blurriness=GET_DATA_BYTE(linem,x);
			if(blurriness>0){
				float frac = ((float)blurriness)/255.0;
				getValueBetweenTwoFixedColors(frac,h1,s1,v1,h2,s2,v2,red,green,blue);
				composeRGBPixel(red, green, blue, lined + x);
			}
		}
	}
}




