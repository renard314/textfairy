/*
 * text_search.cpp
 *
 *  Created on: Jan 31, 2014
 *      Author: renard
 */
#include "text_search.h"
#include <allheaders.h>
#include <sstream>
#include <iostream>
#include "RunningTextlineStats.h"
#include "leptonica_legacy.h"
using namespace std;

/*
 * returns a pixa with the vertical white space that is near the left or right edge
 */
Pixa* pixFindVerticalWhiteSpaceAtEdges(Pix* pixTextBlock) {

	//find long vertical lines
	Pix* pixvl = pixOpenBrickDwa(NULL, pixTextBlock, 1, 20);
	Pixa* pixavl;
	pixConnComp(pixvl, &pixavl, 4);
	//find those that are near the left or right edge
	int count = pixaGetCount(pixavl);
	Numa* numaCenter = numaCreate(0);
	for (int i = 0; i < count; i++) {
		Box* box = pixaGetBox(pixavl, i, L_CLONE);
		l_float32 px, py;
		boxGetCenter(box, &px, &py);
		numaAddNumber(numaCenter, px);
		boxDestroy(&box);
	}
	l_int32 width = pixGetWidth(pixTextBlock);
	l_float32 rightThresh = width * 0.90;
	l_float32 leftThresh = width * 0.10;

	Numa* naRight = numaMakeThresholdIndicator(numaCenter, rightThresh, L_SELECT_IF_GTE);
	Numa* naLeft = numaMakeThresholdIndicator(numaCenter, leftThresh, L_SELECT_IF_LTE);

	Numa* navl = numaLogicalOp(NULL, naRight, naLeft, L_UNION);
	Pixa* pixaVlFiltered = pixaCreate(0);
	for (int i = 0; i < count; i++) {
		l_int32 ival;
		numaGetIValue(navl, i, &ival);
		if (ival == 1) {
			Pix* p = pixaGetPix(pixavl, i, L_CLONE);
			Box* b = pixaGetBox(pixavl, i, L_CLONE);
			pixaAddPix(pixaVlFiltered, p, L_CLONE);
			pixaAddBox(pixaVlFiltered, b, L_CLONE);
			boxDestroy(&b);
			pixDestroy(&p);
		}
	}
	numaDestroy(&naRight);
	numaDestroy(&naLeft);
	numaDestroy(&numaCenter);
	numaDestroy(&navl);
	pixaDestroy(&pixavl);
	pixDestroy(&pixvl);
	return pixaVlFiltered;
}

Pix* pixThresholdToBinary(Pix* pixg) {
	Pix* reduced = pixScaleGrayRank2(pixg, 2);
	Pix* pixTopHat = pixFastTophat(reduced, 2, 2, L_TOPHAT_BLACK);
	Pix* pixb;
	pixSauvolaBinarize(pixTopHat, 16, 0.33, 1, NULL, NULL, NULL, &pixb);
	pixDestroy(&reduced);
	pixDestroy(&pixTopHat);
	return pixb;
}

Pix* pixCreateTextBlockMask(Pix* pixb) {
	l_int32 width = pixGetWidth(pixb);
	l_int32 height = pixGetHeight(pixb);

	//connect text lines vertically to create text block mask
	Pix* pixTextBlock = pixOpenBrickDwa(NULL, pixb, 1, 5);
	//remove noise
	pixCloseBrickDwa(pixTextBlock, pixTextBlock, 2, 2);

	//create vertical whitespace mask to separate columns again
	Pix* pixhs = pixOpenBrickDwa(NULL, pixb, width / 2, 2);
	//open up vertical whitespace on text block mask and invert it in one step
	pixRasterop(pixTextBlock, 0, 0, width, height, PIX_NOT(PIX_PAINT), pixhs, 0, 0);
	pixDestroy(&pixhs);
	return pixTextBlock;
}


/*
 * find all components that are likely to contain text blocks
 *
 */
Numa* pixaFindTextBlockIndicator(Pixa* pixaText, l_int32 width, l_int32 height, Numa** numaEdgeCandidates) {
	Numa* naw;
	Numa* nah;
	Numa* nawhr;
	Numa* nafr;
	Numa *na1, *na2, *na3, *na4, *na5, *na6,*na7,*na8, *nad;

	pixaFindDimensions(pixaText, &naw, &nah);
	nafr = pixaFindAreaFraction(pixaText);
	nawhr = pixaFindWidthHeightRatio(pixaText);
	//selection criteria
	//1. at least 15% of height
	//2. at least 15% of width
	//3. areafraction > 50%
	//4. width height ratio 0.2 - 5
	// Build the indicator arrays for the set of components,
	// based on thresholds and selection criteria.
	na1 = numaMakeThresholdIndicator(nah, height * 0.15, L_SELECT_IF_GTE);
	na2 = numaMakeThresholdIndicator(naw, width * 0.15, L_SELECT_IF_GTE);
	na3 = numaMakeThresholdIndicator(nafr, 0.5, L_SELECT_IF_GTE);
	na4 = numaMakeThresholdIndicator(nawhr, 0.2, L_SELECT_IF_GTE);
	na5 = numaMakeThresholdIndicator(nawhr, 5, L_SELECT_IF_LTE);

	na6 = numaMakeThresholdIndicator(nafr, 0.6, L_SELECT_IF_GTE); //minimum area fraction of an edge text block
	na7 = numaMakeThresholdIndicator(nah, 4, L_SELECT_IF_GTE); //  minimum height of an top or bottom edge block
	na8 = numaMakeThresholdIndicator(naw, 4, L_SELECT_IF_GTE); // minimum width of an left or right edge block

	// Combine the indicator arrays logically to find
	// the components that will be retained.
	nad = numaLogicalOp(NULL, na1, na2, L_INTERSECTION);
	numaLogicalOp(nad, nad, na3, L_INTERSECTION);
	numaLogicalOp(nad, nad, na4, L_INTERSECTION);
	numaLogicalOp(nad, nad, na5, L_INTERSECTION);
	// Invert to get the components that will be removed.
	numaInvert(nad, nad);

	if (numaEdgeCandidates != NULL) {
		//numa 6 contains the area fraction values that apply to textblocks at the edges
		//it is higher treshold than for textblocks that do not touch edges
		//select all pix that
		//1. touch an edge
		//2. have an area fraction of a textblock
		//3. have a minimum width or height, depending whether they touch a vertical or horizontal edge
		l_int32 count = pixaGetCount(pixaText);
		for (int i = 0; i < count; i++) {
			l_int32 ival;
			numaGetIValue(na6, i, &ival);
			if (ival == 1) {
				//area fraction is high enough to be a text block
				numaSetValue(na3, i, 1);

				l_int32 x, y, w, h, ival;
				pixaGetBoxGeometry(pixaText, i, &x, &y, &w, &h);
				bool touchTopOrBottom = y == 0 || (y + h) == height;
				bool touchLeftOrRight = x == 0 || (x + w) == width;
				Numa* widthRequirement;
				Numa* heightRequirement;

				if (!touchLeftOrRight && !touchTopOrBottom) {
					//box does not touch an edge at all
					//remove it from the indicator
					numaSetValue(na3, i, 0);
					continue;
				} else if (touchLeftOrRight && !touchTopOrBottom) {
					//it touches right or left but not top or bottom
					heightRequirement = na1; //original height requirement
					widthRequirement = na8; //width requirement is relaxed
				} else if (touchTopOrBottom && !touchLeftOrRight) {
					//it touches top or bottom but not left or right
					//width requirement applies but not height requirement
					widthRequirement = na2; //original width requirement
					heightRequirement = na7; //height requirement is relaxed
				} else {
					heightRequirement = na7; //height requirement is relaxed
					widthRequirement = na8; //width requirement is relaxed
				}
				Numa* edgeConstraints = numaLogicalOp(NULL, heightRequirement, widthRequirement, L_INTERSECTION);
				numaGetIValue(edgeConstraints, i, &ival);
				if (ival == 0) {
					//remove it
					numaSetValue(na3, i, 0);
				} else {
					//check for edge smoothness
//					l_float32  jpl, jspl, rpl;
//					Pix* pixEdge = pixaGetPix(pixaText,i,L_CLONE);
//					l_int32 side;
//					if (touchLeftOrRight){
//						side = L_FROM_TOP;
//					} else {
//						side = L_FROM_LEFT;
//					}
//
//					pixMeasureEdgeSmoothness(pixEdge, side, 2, 3, &jpl, &jspl, &rpl, "/tmp/junkedge.png");
//					printf( "side = %d: jpl = %6.3f, jspl = %6.3f, rpl = %6.3f\n", side, jpl, jspl, rpl);
//					printf("%i - edge text (%i,%i) at (%i,%i) \n", i, w,h,x,y);
//
				}
				numaDestroy(&edgeConstraints);


			} else {
				numaSetValue(na3, i, 0);
			}
		}
		*numaEdgeCandidates = na3;
	} else {
		numaDestroy(&na3);
	}

	numaDestroy(&naw);
	numaDestroy(&nah);
	numaDestroy(&nawhr);
	numaDestroy(&nafr);
	numaDestroy(&na1);
	numaDestroy(&na2);
	numaDestroy(&na4);
	numaDestroy(&na5);
	numaDestroy(&na6);
	numaDestroy(&na7);
	numaDestroy(&na8);
	return nad;
}


Pixa* pixFindTextBlocks(Pix* pixg) {
	Pix* pixb = pixThresholdToBinary(pixg);
	Pix* pixTextBlock = pixCreateTextBlockMask(pixb);

	l_int32 width = pixGetWidth(pixTextBlock);
	l_int32 height = pixGetHeight(pixTextBlock);

	Pixa* pixaComp;
	pixConnCompPixa(pixTextBlock, &pixaComp, 4);
	Numa* numaEdgeTextBlockIndicator;
	Numa* nad = pixaFindTextBlockIndicator(pixaComp, width, height, &numaEdgeTextBlockIndicator);

	Pixa* pixaVerticalLines = pixFindVerticalWhiteSpaceAtEdges(pixTextBlock);


	l_int32 textBlockCount = pixaGetCount(pixaComp);
	Pixa* pixaTextBlocks = pixaCreate(0);
	//go through each cc
	//if it is no text block, check if it contains a long vertical line
	//if so remove that line and find new cc inside that block and check if they meet the text block criteria
	for (int i = 0; i < textBlockCount; i++) {
		l_int32 ival;
		numaGetIValue(nad, i, &ival);
		Box* box = pixaGetBox(pixaComp, i, L_CLONE);
		Pix* pix = pixaGetPix(pixaComp, i, L_CLONE);
		if (ival == 0) {
			//valid text block, keep it if it does not touch any edge
			l_int32 x, y, w, h;
			boxGetGeometry(box, &x, &y, &w, &h);
			//if it touches top or bottom the height requirement does not apply
			bool touchTopOrBottom = y == 0 || (y + h) == height;
			bool touchLeftOrRight = x == 0 || (x + width) == width;
			if (!touchLeftOrRight && !touchTopOrBottom){
				pixaAddPix(pixaTextBlocks, pix, L_CLONE);
				pixaAddBox(pixaTextBlocks, box, L_CLONE);
			}
		} else {
			l_int32 verticalLineCount = pixaGetCount(pixaVerticalLines);
			for (int j = 0; j < verticalLineCount; j++) {
				Box* bvl = pixaGetBox(pixaVerticalLines, j, L_CLONE);
				Pix* pixvl = pixaGetPix(pixaVerticalLines, j, L_CLONE);
				l_int32 contains;
				boxContains(box, bvl, &contains);
				if (contains) {
					l_int32 x1, y1, w1, h1, x2, y2, w2, h2;
					//vertical line is contained in a box that would be removed
					//check what happens if we remove that line from the box and recalculate the cc.
					boxGetGeometry(bvl, &x2, &y2, &w2, &h2);
					boxGetGeometry(box, &x1, &y1, &w1, &h1);
					//clear the line
					Pix* pixWithoutLine = pixCreate(w1,h1,1);
					pixRasterop(pixWithoutLine, x2 - x1, y2 - y1, w2, h2, PIX_DST & PIX_NOT(PIX_SRC), pixvl, 0, 0);
					//get cc
					Pixa* pixaSplit;
					pixConnCompPixa(pixWithoutLine, &pixaSplit, 4);
					//find text blocks in new pixa
					Numa* splitTextBoxIndicators = pixaFindTextBlockIndicator(pixaSplit, width, height, NULL);
					l_int32 splitTextCount = numaGetCount(splitTextBoxIndicators);
					for (int h = 0; h < splitTextCount; h++) {
						l_int32 ival;
						numaGetIValue(splitTextBoxIndicators, h, &ival);
						if (ival == 0) {
							printf("found a new textblock after removing noise at edge\n");
							l_int32 x3, y3, w3, h3;
							Pix* pixSplit = pixaGetPix(pixaSplit, h, L_CLONE);
							pixaGetBoxGeometry(pixaSplit, h, &x3, &y3, &w3, &h3);
							Box* boxSplit = boxCreate(x1 + x3, y1 + y3, w3, h3);
							//valid text block, keep it
							pixaAddPix(pixaTextBlocks, pixSplit, L_CLONE);
							pixaAddBox(pixaTextBlocks, boxSplit, L_INSERT);
							pixDestroy(&pixSplit);
						}
					}
					pixDestroy(&pixWithoutLine);
					pixaDestroy(&pixaSplit);
					numaDestroy(&splitTextBoxIndicators);
				}
				pixDestroy(&pixvl);
				boxDestroy(&bvl);
			}
		}
		pixDestroy(&pix);
		boxDestroy(&box);
	}
	//iterate over all edge candidates and add those to the list of text blocks as well
	for (int i = 0; i < textBlockCount; i++) {
		l_int32 ival;
		numaGetIValue(numaEdgeTextBlockIndicator, i, &ival);
		if(ival==1){
			Box* box = pixaGetBox(pixaComp, i, L_CLONE);
			Pix* pix = pixaGetPix(pixaComp, i, L_CLONE);
			l_int32 x, y, w, h;
			boxGetGeometry(box, &x, &y, &w, &h);
			printf("%i - edge text (%i,%i) at (%i,%i) \n", i, w,h,x,y);

			pixaAddPix(pixaTextBlocks, pix, L_CLONE);
			pixaAddBox(pixaTextBlocks, box, L_CLONE);
			pixDestroy(&pix);
			boxDestroy(&box);
		}
	}

	//assemble all text blocks into one pix to debug it
	Pix* pixTextBlocks = pixCreate(width, height, 1);
	l_int32 n = pixaGetCount(pixaTextBlocks);
	for (int i = 0; i < n; i++) {
		Pix* pix = pixaGetPix(pixaTextBlocks, i, L_CLONE);
		Box* box = pixaGetBox(pixaTextBlocks, i, L_CLONE);
		l_int32 x, y, w, h;
		boxGetGeometry(box, &x, &y, &w, &h);
		pixRasterop(pixTextBlocks, x, y, w, h, PIX_SRC, pix, 0, 0);
		boxDestroy(&box);
		pixDestroy(&pix);
	}
	pixDisplay(pixTextBlocks,0,0);
	pixDestroy(&pixTextBlocks);

	numaDestroy(&nad);
	numaDestroy(&numaEdgeTextBlockIndicator);
	pixaDestroy(&pixaComp);
	pixaDestroy(&pixaVerticalLines);
	pixDestroy(&pixTextBlock);
	pixDestroy(&pixb);
	return pixaTextBlocks;

}

Pix* pixThreshold3(Pix* pixg) {
	L_TIMER timer = startTimerNested();
	Pix* reduced = pixScaleGrayRank2(pixg, 2);

	l_int32 width = pixGetWidth(reduced);
	l_int32 height = pixGetHeight(reduced);

	Pix* pixTopHat = pixFastTophat(reduced, 2, 2, L_TOPHAT_BLACK);
	Pix* pixb;
	pixSauvolaBinarize(pixTopHat, 16, 0.33, 1, NULL, NULL, NULL, &pixb);
	pixDestroy(&reduced);
	pixDestroy(&pixTopHat);

	printf("binarisation = %f\n", stopTimerNested(timer));

	timer = startTimerNested();

	ostringstream s;
	//s << "o1.5 + c2.2";
	Pix* pixo = pixOpenBrickDwa(NULL, pixb, 1, 5);
	pixCloseBrickDwa(pixo, pixo, 2, 2);

	Pix* pixhs = pixOpenBrickDwa(NULL, pixb, width / 2, 1);
	pixRasterop(pixhs, 0, 0, width, height, PIX_NOT(PIX_PAINT), pixo, 0, 0);

	pixInvert(pixo, pixo);
	Pixa* pixaComp;
	pixConnCompPixa(pixo, &pixaComp, 4);
	Numa* naw;
	Numa* nah;
	Numa* nawhr;
	Numa* nafr;
	Numa *na1, *na2, *na3, *na4, *na5, *nad;

	pixaFindDimensions(pixaComp, &naw, &nah);
	nafr = pixaFindAreaFraction(pixaComp);
	nawhr = pixaFindWidthHeightRatio(pixaComp);

	//selection criteria
	//1. at least 20% of height
	//2. at least 20% of width
	//3. areafraction > 50%
	//4. width height ratio 0.3 - 0.7
	// Build the indicator arrays for the set of components,
	// based on thresholds and selection criteria.
	na1 = numaMakeThresholdIndicator(nah, height / 5, L_SELECT_IF_GTE);
	na2 = numaMakeThresholdIndicator(naw, width / 5, L_SELECT_IF_GTE);
	na3 = numaMakeThresholdIndicator(nafr, 0.5, L_SELECT_IF_GTE);
	na4 = numaMakeThresholdIndicator(nawhr, 0.3, L_SELECT_IF_GTE);
	na5 = numaMakeThresholdIndicator(nawhr, 0.7, L_SELECT_IF_LTE);
	// Combine the indicator arrays logically to find
	// the components that will be retained.
	nad = numaLogicalOp(NULL, na1, na2, L_INTERSECTION);
	numaLogicalOp(nad, nad, na3, L_INTERSECTION);
	numaLogicalOp(nad, nad, na4, L_INTERSECTION);
	numaLogicalOp(nad, nad, na5, L_INTERSECTION);
	// Invert to get the components that will be removed.
	numaInvert(nad, nad);
	// Remove the components, in-place.
	pixRemoveWithIndicator(pixo, pixaComp, nad);

	numaDestroy(&naw);
	numaDestroy(&nah);
	numaDestroy(&nawhr);
	numaDestroy(&nafr);
	numaDestroy(&na1);
	numaDestroy(&na2);
	numaDestroy(&na3);
	numaDestroy(&na4);
	numaDestroy(&na5);
	numaDestroy(&nad);

	printf("comp = %f\n", stopTimerNested(timer));

	pixaDestroy(&pixaComp);
	pixDestroy(&pixb);
	return pixo;
}

Pix* pixTreshold2(Pix* pix) {
	L_TIMER timer = startTimerNested();
	Pix* reduced = pixScaleGrayRank2(pix, 2);
	Pix* pixTopHat = pixFastTophat(reduced, 4, 4, L_TOPHAT_BLACK);
	pixDestroy(&reduced);
	int nx = 0;
	int ny = 0;

	PIXTILING* pt = pixTilingCreate(pixTopHat, 0, 0, 15, 15, 0, 0);
	pixTilingGetCount(pt, &nx, &ny);
	Pix* pixth2 = pixCreate(nx, ny, 8);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, i, j);

			NUMA* histo = pixGetGrayHistogram(pixt, 1);
			NUMA* norm = numaNormalizeHistogram(histo, 1);
			l_float32 sum, moment, var, y, variance, mean, countPixels;
			l_int32 start = 0, end = 255, n, error;

			error = numaGetNonzeroRange(histo, 0, &start, &end);
			if (end == start || error == 1) {
				pixSetPixel(pixth2, j, i, 255);
				continue;
			}

			l_float32 iMulty;
			for (sum = 0.0, moment = 0.0, var = 0.0, countPixels = 0, n = start; n < end; n++) {
				numaGetFValue(norm, n, &y);
				sum += y;
				iMulty = n * y;
				moment += iMulty;
				var += n * iMulty;
				numaGetFValue(histo, n, &y);
				countPixels += y;
			}
			variance = sqrt(var / sum - moment * moment / (sum * sum));
			mean = moment / sum;

			l_int32 thresh;
			l_float32 p1, p2;
			numaSplitDistribution(norm, 0.0, &thresh, NULL, NULL, &p1, &p2, NULL);
			bool isVarianceLow = variance < 7;
			l_float32 isP2gP1 = p2 / p1;
			if (isVarianceLow && isP2gP1 < 2) {
				thresh = 255;
			}
			pixSetPixel(pixth2, j, i, thresh);
			pixDestroy(&pixt);
			numaDestroy(&histo);
			numaDestroy(&norm);
		}
	}
	int w = pixGetWidth(pixTopHat);
	int h = pixGetHeight(pixTopHat);

	Pix* pixb = pixCreate(w, h, 1);
	l_int32 tw, th;
	pixTilingGetSize(pt, &tw, &th);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			l_uint32 t;
			pixGetPixel(pixth2, j, i, &t);
			Pix* pixt = pixTilingGetTile(pt, i, j);
			if (t == 0) {
				t = 255;
			}
			Pix* pixbTile = pixThresholdToBinary(pixt, t);
			pixTilingPaintTile(pixb, i, j, pixbTile, pt);
			pixDestroy(&pixt);
			pixDestroy(&pixbTile);
		}
	}
	printf("total time = %f\n", stopTimerNested(timer));
	pixDestroy(&pixTopHat);
	pixDestroy(&pixth2);
	return pixb;
}

Pix* pixTreshold(Pix* pix) {
	float scorefract = 0.05;
	int thresh;
	int nx = 2;
	int ny = 2;
	PIXTILING* pt = pixTilingCreate(pix, nx, ny, 0, 0, 0, 0);
	pixTilingGetCount(pt, &nx, &ny);
	Pix* pixth = pixCreate(nx, ny, 8);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, j, i);
			int w = pixGetWidth(pixt);
			int h = pixGetHeight(pixt);
			pixSplitDistributionFgBg(pixt, scorefract, 1, &thresh, NULL, NULL, 0);
			pixSetPixel(pixth, j, i, thresh);
			pixDestroy(&pixt);
		}
	}
	pixTilingDestroy(&pt);
	l_int32 w = pixGetWidth(pix);
	l_int32 h = pixGetHeight(pix);
	pt = pixTilingCreate(pix, nx, ny, 0, 0, 0, 0);
	Pix* pixeh = pixCreate(w, h, 1);
	l_uint32 val;
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, i, j);
			pixGetPixel(pixth, j, i, &val);
			Pix* pixbTile = pixThresholdToBinary(pixt, val);
			pixTilingPaintTile(pixeh, i, j, pixbTile, pt);
			pixDestroy(&pixt);
			pixDestroy(&pixbTile);
		}
	}
	pixTilingDestroy(&pt);
	pixDestroy(&pixth);
	return pixeh;
}

Boxa* pixFindTextRegions(Pix* pix, Pix** pixb) {
	Pix* pix_edge = pixTwoSidedEdgeFilter(pix, L_HORIZONTAL_EDGES);
	Pix* pixeh = pixTreshold(pix_edge);
	if (pixb != NULL) {
		*pixb = pixeh;
	}

	std::ostringstream s;
	s << "o1.90+c20.1";
	//vertical whitespace mask
	Pix *pixvws = pixMorphCompSequence(pixeh, s.str().c_str(), 0);
	s.str("");
	s << "o1.5+o80.1+c1.20";
	//horizontal whitespace mask
	Pix *pixhws = pixMorphCompSequence(pixeh, s.str().c_str(), 0);

	//combine whitespace masks
	l_int32 w = pixGetWidth(pixeh);
	l_int32 h = pixGetHeight(pixeh);
	pixRasterop(pixvws, 0, 0, w, h, PIX_NOT(PIX_SRC | PIX_DST), pixhws, 0, 0);

	Boxa* tl = pixConnComp(pixvws, NULL, 8);
	l_int32 comp_count = boxaGetCount(tl);
	Pixa* edge_comp = pixaCreateFromBoxa(pixeh, tl, NULL);
	Numa* areaFraction = pixaFindAreaFraction(edge_comp);
	Numa* na1 = numaMakeThresholdIndicator(areaFraction, 0.91, L_SELECT_IF_LT);
	l_int32 ival;
	Boxa* filtered_boxa = boxaCreate(0);

	for (int i = 0; i < comp_count; i++) {
		numaGetIValue(na1, i, &ival);
		if (ival == 1) {
			Box* b = boxaGetBox(tl, i, L_CLONE);
			boxaAddBox(filtered_boxa, b, L_CLONE);
			boxDestroy(&b);
		}
	}
	if (pixb == NULL) {
		pixDestroy(&pixeh);
	}
	pixDestroy(&pix_edge);
	pixDestroy(&pixvws);
	pixDestroy(&pixhws);
	boxaDestroy(&tl);
	pixaDestroy(&edge_comp);
	numaDestroy(&areaFraction);
	numaDestroy(&na1);

	return filtered_boxa;
}





// groups the array of line heights
void numaGroupTextLineHeights(Numa* numaTextHeights,Numa** numaMean, Numa** numaStdDev, Numa** numaCount){
	l_int32 n = numaGetCount(numaTextHeights);
	Numa* numaMeanResult = numaCreate(0);
	Numa* numaStdDevResult = numaCreate(0);
	Numa* numaCountResult = numaCreate(0);
	RunningTextlineStats stats(true);
	for (int x = 0; x < n; x++) {
		l_int32 ival;
		numaGetIValue(numaTextHeights, x, &ival);
		//printf("text height = %i\n",ival);
		if(stats.Fits(ival)){
			stats.Push(ival);
		} else{
			if (stats.Count()>2){
				printf("%i lines grouped, mean = %f\n",stats.Count(),stats.Mean());
				numaAddNumber(numaCountResult,stats.Count());
				numaAddNumber(numaMeanResult,stats.Mean());
				numaAddNumber(numaStdDevResult,stats.StandardDeviation());
			}
			stats.Clear();
			stats.Push(ival);
		}
	}

	if (stats.Count()>2){
		printf("%i lines grouped, mean = %f\n",stats.Count(),stats.Mean());
		numaAddNumber(numaCountResult,stats.Count());
		numaAddNumber(numaMeanResult,stats.Mean());
		numaAddNumber(numaStdDevResult,stats.StandardDeviation());
	}
	if(numaCount!=NULL){
		*numaCount =  numaCountResult;
	}
	if(numaMean!=NULL){
		*numaMean = numaMeanResult;
	}
	if (numaStdDev!=NULL){
		*numaStdDev = numaStdDevResult;
	}
}

Numa* findLineSpacingFromPeaks(Numa* numaPixelSum, Numa* peaks){
	/*
	 *     (1) The returned na consists of sets of four numbers representing
	 *         the peak, in the following order:
	 *            left edge; peak center; right edge; normalized peak area
	 */
	l_int32 n = numaGetCount(peaks);
	l_int32 size = n/4;
	l_int32 leftIndex,rightIndex;
	l_float32 peakArea;
	Numa* na = numaCreate(size);

	if (n==0){
		return na;
	}

	for (int i = 0; i < n; i+=4) {
		numaGetIValue(peaks,i,&leftIndex);
		numaGetIValue(peaks,i+2,&rightIndex);
		numaGetFValue(peaks,i+3,&peakArea);
		numaAddNumber(na, rightIndex-leftIndex);
		numaAddNumber(na, peakArea);
	}

	return na;
}

void extractPeaks(l_int32 width, Numa* numaPixelSum, Numa* peaks, Numa** resultPeaks){
	l_int32 n = numaGetCount(peaks);
	l_int32 size = n/4;
	l_float32 peakArea;
	l_int32 index,peakHeight,left,right;
	Numa* na = numaCreate(size);
	Numa* nArea = numaCreate(size);
	Numa* nLineWidths = numaCreate(size);
	Numa* numaResult = numaCreate(0);
	if (n<1){
		return;
	}

	//filter peaks based on a minimum area and minimum height
	l_int32 minimumHeight = width*0.05;
	l_float32 minimumArea = (1./n)*0.3;

	for (int i = 0; i < n; i+=4) {
		numaGetIValue(peaks,i,&left);
		numaGetIValue(peaks,i+1,&index);
		numaGetIValue(peaks,i+2,&right);
		numaGetFValue(peaks,i+3,&peakArea);
		numaAddNumber(nArea, peakArea);
		numaAddNumber(na, index);

		l_int32 leftHeight, rightHeight, centerHeight;
		numaGetIValue(numaPixelSum,left,&leftHeight);
		numaGetIValue(numaPixelSum,right,&rightHeight);
		numaGetIValue(numaPixelSum,index,&centerHeight);
		l_float32 lineHeight =centerHeight - (leftHeight+rightHeight)/2;
		//printf("%i index = %i, height = %f, area = %f\n",i,index,lineHeight, peakArea);
		numaAddNumber(nLineWidths, lineHeight );
	}
	Numa* naindex = numaGetSortIndex(na, L_SORT_INCREASING);
	Numa* naSorted = naSorted = numaSortByIndex(na,naindex);
	Numa* nAreaSorted  = numaSortByIndex(nArea,naindex);
	Numa* nLineWidthsSorted  = numaSortByIndex(nLineWidths,naindex);
	numaDestroy(&naindex);
	numaDestroy(&nArea);
	numaDestroy(&nLineWidths);
	numaDestroy(&na);


	for (int i = 0; i < size; i++) {
		numaGetIValue(naSorted,i,&index);
		numaGetIValue(nLineWidthsSorted,i,&peakHeight);
		numaGetFValue(nAreaSorted,i,&peakArea);
		if ( peakArea > minimumArea){
			numaAddNumber(numaResult,index);
			//printf("%i index = %i, peakHeight = %i, area = %f\n",i,index,peakHeight, peakArea);
		}
	}

	if(resultPeaks!=NULL){
		*resultPeaks = numaResult;
	} else {
		numaDestroy(&numaResult);
	}
	numaDestroy(&naSorted);
	numaDestroy(&nAreaSorted);
	numaDestroy(&nLineWidths);

}

NUMA * numaMakeDelta2(NUMA  *nas) {
	l_int32  i, n, prev, cur;
	NUMA    *nad;
    n = numaGetCount(nas);
    nad = numaCreate(n - 1);
    numaGetIValue(nas,0,&prev);
    for (i = 1; i < n; i++) {
        numaGetIValue(nas, i, &cur);
        numaAddNumber(nad, cur - prev);
        prev = cur;
    }
    return nad;
}


l_float32 pixGetTextLineSpacing(Pix* pixb){
	int nx = 1;
	int ny = 10;
	l_float32 tileWidth = pixGetWidth(pixb)/6;
	l_float32 tileHeight = pixGetWidth(pixb)/6;
	if (tileWidth<24){
		tileWidth = 24;
	}

	PIXTILING* pt = pixTilingCreate(pixb, 0, 0, tileWidth, tileHeight, tileWidth/3, tileHeight/3);
	pixTilingGetCount(pt, &nx, &ny);
	l_float32 sum=0;
	l_float32 lineCount=0;

	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, i, j);
			l_int32 tileWidth = pixGetWidth(pixt);
			l_float32 tileHeight = pixGetHeight(pixt);
			Numa* numaPixelSum = pixSumPixelsByRow(pixt, NULL);
			//Numa* closed = numaOpen(numaPixelSum,4);
			//numaPixelSum = closed;
			Numa* allPeaks = numaFindPeaks(numaPixelSum,30,0.9,0.01);
			Numa* peaks = NULL;
			extractPeaks(tileWidth, numaPixelSum, allPeaks,&peaks);
			Numa* delta = numaMakeDelta2(peaks);
			//Numa* delta = findLineSpacingFromPeaks(numaPixelSum,allPeaks);
			Numa* numaMean;
			Numa* numaStdDev;
			Numa* numaCount;
			//numaPlotToPix(numaPixelSum,peaks);
			numaGroupTextLineHeights(delta, &numaMean, &numaStdDev,&numaCount);
			l_int32 n = numaGetCount(numaMean);

			if(n>0){
				//sort by line count
				Numa* naindex = numaGetSortIndex(numaCount, L_SORT_INCREASING);
				l_float32 fract = 0.75;
				l_int32 start = (l_int32)(fract * (l_float32)(n - 1) + 0.5);
				l_int32 end = numaGetCount(naindex);

				//get average line spacing for the top 25% line groups (the ones with the most line numbers)
				for(int x = start;x<end;x++){
					l_int32 count;
					l_float32 mean;
					l_int32 index;
					numaGetIValue(naindex,x,&index);
					numaGetFValue(numaMean, index,&mean);
					numaGetIValue(numaCount, index,&count);
					sum+=count*mean;
					lineCount+=count;
				}
			}

			numaDestroy(&numaMean);
			numaDestroy(&numaStdDev);
			numaDestroy(&numaPixelSum);
			//numaDestroy(&extrema);
			numaDestroy(&peaks);
			numaDestroy(&allPeaks);
			numaDestroy(&delta);
			pixDestroy(&pixt);
		}
	}
	pixTilingDestroy(&pt);

	if(sum==0){
		return 0;
	}

	l_float32 avg;
	avg = sum/lineCount;
	return avg;
}

Pixa* pixGetTextBlocks(l_float32 textLineSpacing, Pix* pixb, Pix** pixmorphout){
	Pixa* pixaComp;
	Pixa* pixaOrg;
	Pixa* pixaFiltered;
	Pix *pixmorph;
	Numa* naw;
	Numa* nah;
	Numa* nawhr;
	Numa* nafr, *nafrOrg;
	Numa *na1, *na2, *na3,*na4,*na5, *nad;
	Boxa* boxa;
	ostringstream s;

	if (textLineSpacing==0) {
		return NULL;
	}
	l_int32 w = pixGetWidth(pixb);
	l_int32 h = pixGetHeight(pixb);

	s.str("");
	//remove pure black vertical areas
	//Pix* pixVertical = pixOpenBrickDwa(NULL,pixb,textLineSpacing*.5,textLineSpacing*.5);
	//Pix* pixHorizontal = pixOpenBrickDwa(NULL,pixb,textLineSpacing*.5,textLineSpacing*3);
//	pixRasterop(pixb, 0, 0, w, h, PIX_NOT(PIX_SRC) & PIX_DST, pixVertical, 0, 0);
//	pixRasterop(pixb, 0, 0, w, h, PIX_NOT(PIX_SRC) & PIX_DST, pixHorizontal, 0, 0);
//	pixDisplay(pixVertical,0,0);

	/* connect paragraphs candidates*/
	s << "c"<<(int)(textLineSpacing*.8)<<"."<<(int)(textLineSpacing*.85);
	pixmorph = pixMorphCompSequence(pixb, s.str().c_str(), 0);
	if (pixmorphout!=NULL){
		*pixmorphout = pixClone(pixmorph);
	}

	/* get bounding boxes of paragraphs candidates*/
	//pixInvert(pixmorph, pixmorph);
	pixConnCompPixa(pixmorph, &pixaComp, 4);
	l_int32 boxCount = pixaGetCount(pixaComp);
	if (boxCount==0){
		pixDestroy(&pixmorph);
		return pixaComp;
	}

	/*
	 	remove candidates that are unlikely to contain text.
		selection criteria:
		1. minimum height
		2. minimum width
		3. areafraction > 10-40%
		4. areafraction of morphed image > 30%
	*/
	pixaFindDimensions(pixaComp, &naw, &nah);
	boxa = pixaGetBoxa(pixaComp,L_CLONE);
	pixaOrg = pixaCreateFromBoxa(pixb,boxa,NULL);
	nafrOrg = pixaFindAreaFraction(pixaOrg);
	nafr = pixaFindAreaFraction(pixaComp);
//	l_int32 count = boxaGetCount(boxa);
//	printf("\n");
//	for(int i =  0; i < count; i++){
//		l_int32 x,y,w,h;
//		l_float32 afr;
//		boxaGetBoxGeometry(boxa,i,&x,&y,&w,&h);
//		numaGetFValue(nafrOrg,i,&afr);
//		printf("(%i/%i - %ix%i) -> %f\n",x,y,w,h,afr);
//	}

	nawhr = pixaFindWidthHeightRatio(pixaComp);
	// Build the indicator arrays for the set of components,
	// based on thresholds and selection criteria.
	na1 = numaMakeThresholdIndicator(nah, textLineSpacing*.85, L_SELECT_IF_GTE);
	na2 = numaMakeThresholdIndicator(naw, textLineSpacing*2, L_SELECT_IF_GTE);
	na3 = numaMakeThresholdIndicator(nafrOrg, 0.10, L_SELECT_IF_GTE);
	na4 = numaMakeThresholdIndicator(nafrOrg, 0.50, L_SELECT_IF_LTE);
	na5 = numaMakeThresholdIndicator(nafr, 0.30, L_SELECT_IF_GTE);
	// Combine the indicator arrays logically to find
	// the components that will be retained.
	nad = numaLogicalOp(NULL, na1, na2, L_INTERSECTION);
	numaLogicalOp(nad, nad, na3, L_INTERSECTION);
	numaLogicalOp(nad, nad, na4, L_INTERSECTION);
	numaLogicalOp(nad, nad, na5, L_INTERSECTION);
	pixaFiltered = pixaSelectWithIndicator(pixaComp,nad,NULL);

	numaDestroy(&naw);
	numaDestroy(&nah);
	numaDestroy(&nawhr);
	numaDestroy(&nafr);
	numaDestroy(&nafrOrg);
	numaDestroy(&na1);
	numaDestroy(&na2);
	numaDestroy(&na3);
	numaDestroy(&na4);
	numaDestroy(&na5);
	numaDestroy(&nad);
	pixaDestroy(&pixaComp);
	pixaDestroy(&pixaOrg);
	pixDestroy(&pixmorph);

//	pixDestroy(&pixVertical);
//	pixDestroy(&pixHorizontal);
	return pixaFiltered;
}


