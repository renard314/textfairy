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
#include "binarize.h"
#include <stdio.h>
#include <iostream>
#include <sstream>
#include <cmath>
#include "image_processing.h"

using namespace std;

int determineThresholdForTile(Pix* pixt, bool debug) {
	l_int32 start = 0, end = 255, i, error, closeSize = 0;
	l_float32 sum, moment, var, y, variance, mean, meanY, countPixels;

	NUMA* norm;
	NUMA* histo = pixGetGrayHistogram(pixt, 1);
	numaSetValue(histo, 255, 0); //ignore white pixels
	error = numaGetNonzeroRange(histo, 0, &start, &end);
	if (end == start || error == 1) {
		numaDestroy(&histo);
		return 0;
	}
	closeSize = end - start;
	if (closeSize % 2 == 0) {
		closeSize++;
	}
	norm = numaNormalizeHistogram(histo, 1);
	/*
	l_float32 median;
	numaGetMedian(histo,&median);
	printf("%2.1f\t", median);
	*/

	l_float32 iMulty;
	for (sum = 0.0, moment = 0.0, var = 0.0, countPixels = 0, i = start;
			i < end; i++) {
		numaGetFValue(norm, i, &y);
		sum += y;
		iMulty = i * y;
		moment += iMulty;
		var += i * iMulty;
		numaGetFValue(histo, i, &y);
		countPixels += y;
	}
	variance = sqrt(var / sum - moment * moment / (sum * sum));
	mean = moment / sum;
	meanY = sum / (end - start);
	int result = 0;
	if (variance < 8) {
		result = 0;
	} else {
		if ((mean * 0.5) < variance) {
			result = mean;// + variance*0.4;
		} else {
			//high variance means we have probably have a very good separation between fore and background
			//in that case the threshold be closer to the mean
			result = mean - sqrt(variance) * 3;
			//result = mean - variance*0.66;
		}
		//result = mean - variance*0.66; //0.2 when median is activated, 0.66 otherwise
	}

	if (debug == true) {
		printf("mean = %f , variance = %f, meanY = %f \n", mean, variance,
				meanY);
		printf("%i ,%i\n", start, end);
		GPLOT *gplot;
		ostringstream name;
		name << mean;
		gplot = gplotCreate(name.str().c_str(), GPLOT_X11, name.str().c_str(),
				"x", "y");
		ostringstream title;
		title << "mean = " << mean << ", " << "variance = " << variance
				<< ", thresh = " << result;
		//gplotAddPlot(gplot, NULL, diff, GPLOT_LINES, "diff histogram");
		gplotAddPlot(gplot, NULL, norm, GPLOT_LINES, title.str().c_str());
		gplotMakeOutput(gplot);
		gplotDestroy(&gplot);
	}
	numaDestroy(&norm);
	numaDestroy(&histo);

	//TODO check std dev of y values. large value > foreground and background, low value > only background
	//idea: narrow histogramm means a high likelyhood for bg
	return result;

}

Pix* createEdgeMask(Pix* pixs) {
	L_TIMER timer = startTimerNested();
	ostringstream s;
	Pix* pixConv = pixBlockconvGray(pixs, NULL, 5, 5);
	Pix* pixConvEdges = pixSobelEdgeFilter(pixConv, L_ALL_EDGES);
	pixDestroy(&pixConv);
	pixInvert(pixConvEdges, pixConvEdges);
	s << "sobel edge detection: " << stopTimerNested(timer) << std::endl;
	timer = startTimerNested();
	pixWrite("pixConvEdges.bmp", pixConvEdges, IFF_BMP);

	NUMA* histo = pixGetGrayHistogram(pixConvEdges, 8);
	NUMA* norm = numaNormalizeHistogram(histo, 1.0);
	l_float32 median, mean, variance;
	numaGetHistogramStats(norm, 0, 1, &mean, &median, NULL, &variance);
	numaDestroy(&histo);
	numaDestroy(&norm);

	l_int32 thresh = 0;
	if (variance < 1.0) {
		thresh = 255;
	} else {
		thresh = 254;
	}
	printf("mean = %f, median = %f, std dev = %f, thresh = %i\n", mean, median,
			variance, thresh);

	Pix* pixForeground = pixThresholdToBinary(pixConvEdges, thresh);
	pixDestroy(&pixConvEdges);
	pixCloseSafeBrick(pixForeground,pixForeground,64,64);

	pixWrite("foreground.bmp", pixForeground, IFF_BMP);

	s << "binarization of edge mask: " << stopTimerNested(timer) << std::endl;
	timer = startTimerNested();

	Pix* pixacc = pixBlockconvAccum(pixForeground);
	Pix* pixRank = pixBlockrank(pixForeground, pixacc, 8, 8, 0.1);

	pixDestroy(&pixacc);
	pixDestroy(&pixForeground);
	pixInvert(pixRank, pixRank);
	Pix* pixResult = pixOpenBrick(NULL, pixRank, 10, 10);
	pixDestroy(&pixRank);
	pixWrite("rank.bmp", pixResult, IFF_BMP);
	s << "mask generation: " << stopTimerNested(timer) << std::endl;
	printf("%s", s.str().c_str());

	return pixResult;
}

/**
 * determines and applies a threshold for each tile separately
 */
Pix* binarizeTiled(Pix* pixs, const l_uint32 tileSize) {
	L_TIMER timer = startTimerNested();
	l_int32 thresh, w, h;
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
	bool debug = false;
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			pixt = pixTilingGetTile(pt, i, j);
			if (i == 0 && j == 0) {
				debug = false;
			} else {
				debug = false;
			}
			thresh = determineThresholdForTile(pixt, debug);
			pixSetPixel(pixth, j, i, thresh);
			pixDestroy(&pixt);
		}
	}
	pixTilingDestroy(&pt);
	s << "local threshhold determination: " << stopTimerNested(timer)
			<< std::endl;
	timer = startTimerNested();

	pt = pixTilingCreate(pixs, nx, ny, 0, 0, 0, 0);
	pixb = pixCreate(w, h, 1);
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
	pixDestroy(&pixth);
	s << "local threshhold application: " << stopTimerNested(timer)
			<< std::endl;
	printf("%s", s.str().c_str());

	return pixb;
}

void binarize(Pix* pixGrey, Pix* pixhm, Pix** pixb) {
	Pix* pixEdgeMask;
	l_int32 width = pixGetWidth(pixGrey);
	const l_uint32 tileSize = width/15; //size of tile during threshholding
	L_TIMER timer = startTimerNested();
	ostringstream s;

	pixEdgeMask = createEdgeMask(pixGrey);

	pixSetMasked(pixGrey, pixEdgeMask, 255);
	if (pixhm != NULL) {
		//dont allow image mask to cover text
		pixAnd(pixhm, pixhm, pixEdgeMask);
	}
	pixDestroy(&pixEdgeMask);

	s << "text mask creation: " << stopTimerNested(timer) << std::endl;
	printf("%s", s.str().c_str());
	pixWrite("toThresh.bmp", pixGrey, IFF_BMP);

	/*
	 timer = startTimerNested();
	 Pix* pixMedian = pixRankFilterGray(pixGrey,3,3,0.25);
	 printf("rank filter = %f\n",stopTimerNested(timer));
	 pixWrite("rank.bmp",pixMedian,IFF_BMP);
	 */

	*pixb = binarizeTiled(pixGrey, tileSize);
	//pixDestroy(&pixMedian);

	if (pixhm != NULL) {
		pixSetMasked(*pixb, pixhm, 0);
	}
}

