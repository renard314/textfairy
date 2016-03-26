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

/*
 * pageseg.cpp
 *
 *  Created on: Mar 1, 2012
 *      Author: renard
 */

#include "pageseg.h"

using namespace std;

l_int32 renderTransformedBoxa(PIX *pixt, BOXA *boxa, l_int32 i) {
	l_int32 j, n, rval, gval, bval;
	BOX *box;

	n = boxaGetCount(boxa);
	rval = (1413 * i) % 256;
	gval = (4917 * i) % 256;
	bval = (7341 * i) % 256;
	for (j = 0; j < n; j++) {
		box = boxaGetBox(boxa, j, L_CLONE);
		pixRenderHashBoxArb(pixt, box, 10, 3, i % 4, 1, rval, gval, bval);
		boxDestroy(&box);
	}
	return 0;
}

l_int32 getMedianComponentHeight(Pix* pixtl, bool debug) {
	ostringstream s;
	Pixa* comp;
	Boxa* tl = pixConnComp(pixtl, &comp, 4);
	NUMA* na = pixaCountPixels(comp);

	Box* b;
	int n = boxaGetCount(tl);
	float cc = 0;
	for (int i = 0; i < n; i++) {
		b = boxaGetBox(tl, i, L_CLONE);
		float c;
		numaGetFValue(na, i, &c);
		c /= b->w;
		numaSetValue(na, i, c);
		cc += c;
		boxDestroy(&b);
	}
	if (n > 0) {
		cc /= n;
	}

	float median = 0;
	numaGetRankValue(na, 0.75,NULL,false, &median);
	//numaGetMedian(na, &median);
	numaDestroy(&na);
	pixaDestroy(&comp);
	boxaDestroy(&tl);
	if (debug) {
		std::cout << "average: " << cc << "\n" << "median: " << median << "\n";
	}
	return median;
}

void growTextBounds(Boxa* textBounds) {
	int n = boxaGetCount(textBounds);
	BOX *box;
	for (int j = 0; j < n; j++) {
		box = boxaGetBox(textBounds, j, L_CLONE);
		boxAdjustSides(box,box,-5,5,-5,5);
		boxDestroy(&box);
	}
}

Pixa* pagesegGetColumns(Pix* pixtext, bool debug) {
	ostringstream s;
	PIX *pixb, *pixBinary;
	if (debug) {
		startTimer();
	}
    l_int32 borderSize = 50;
    Pix* pixBorder = pixAddBlackOrWhiteBorder(pixtext, borderSize, borderSize, borderSize, borderSize, L_GET_WHITE_VAL);
	int w = pixGetHeight(pixBorder);
    

	s << "o1.20+c5.20+d10.5+o1." << w / 10;

	/*remove long vertical lines*/
	Pix* pixvl = pixMorphCompSequence(pixBorder, s.str().c_str(), 0);
	pixSetMasked(pixBorder, pixvl, 0);
	pixDestroy(&pixvl);

	pixBinary = pixReduceRankBinaryCascade(pixBorder, 1, 0, 0, 0);


	/*remove long horizontal lines*/
	Pix* pixhl = pixMorphCompSequence(pixBinary, "o70.1+c20.5+d5.10", 0);
	pixSetMasked(pixBinary, pixhl, 0);
	pixDestroy(&pixhl);

	pixb = pixInvert(NULL, pixBinary);

	/*create vertical whitespace mask*/
	//Pix *pixvws = pixMorphCompSequence(pixb, "o1.100+c2.1+o7.1+c7.10+c1.50", 0);
	Pix *pixvws = pixMorphCompSequence(pixb, "o1.100+c2.1+o7.1+c7.10", 0);

	/* Three steps to getting text line mask:
	 *   (1) close the characters and words in the textlines
	 *   (2) open the vertical whitespace corridors back up
	 *   (3) small opening to remove noise    */
	pixCloseSafeBrick(pixBinary, pixBinary, 60, 1);
	pixDisplay(pixBinary,0,0);
	pixSubtract(pixBinary, pixBinary, pixvws);
	pixOpenBrick(pixBinary, pixBinary, 1, 3);
	Pix* pixtl = pixMorphCompSequence(pixBinary, "o1.3+o25.1", 0);
	pixDestroy(&pixBinary);

	/*make a guess at the text size*/
	int ts = getMedianComponentHeight(pixtl, debug);
	pixBinary = pixtl;

	/*close inter word spacing*/
	pixCloseBrick(pixtl, pixtl, ts * 2, 1);
	pixOpenBrick(pixvws, pixvws, ts * 1.2, 1);

	/*make a guess at the line spacing*/
	/*create components for vertical white space between text lines */
	s.str("");
	s << "c1." << ts * 3;
	Pix* pixls = pixMorphCompSequence(pixtl, s.str().c_str(), 0);
	pixSubtract(pixls, pixls, pixtl);
	/*small opening to remove noise*/
	pixOpenBrick(pixls, pixls, 2, 2);
	int ls = getMedianComponentHeight(pixls, debug);
	pixDestroy(&pixls);

	/*create horizontal whitespace mask*/
	s.str("");
	s << "o50.1";
	Pix *pixhws = pixMorphCompSequence(pixb, s.str().c_str(), 0);
	pixDestroy(&pixb);
	//	pixOpenBrick(pixhws, pixhws, 1, ls * 1.6);
	pixOpenBrick(pixhws, pixhws, 1, ls * 1.5);

	/* Join pixels vertically to make a textblock mask */
	s.str("");
	s << "c" << ts << "." << ts * 8 << "+ o4.1";
	pixb = pixBinary;
	pixBinary = pixMorphSequence(pixb, s.str().c_str(), 0);
	pixDestroy(&pixb);
	pixSubtract(pixBinary, pixBinary, pixhws);
	pixDestroy(&pixhws);


	/* Solidify the textblock mask and remove noise:
	 *   (1) For each cc, close the blocks and dilate slightly to form a solid mask.
	 *   (2) Small horizontal closing between components.
	 *   (3) Open the white space between columns, again.
	 *   (4) Remove small components. */
	s.str("");
	s << "c" << ts << "." << ts << "+d3.3";
	Pix* pixt2 = pixMorphSequenceByComponent(pixBinary, s.str().c_str(), 8, 0, 0, NULL);
	pixDestroy(&pixBinary);
	pixCloseSafeBrick(pixt2, pixt2, 10, 1);
	pixSubtract(pixt2, pixt2, pixvws);

	Pix* pixd = pixSelectBySize(pixt2, ts * 5, ts, 4, L_SELECT_IF_EITHER, L_SELECT_IF_GTE, NULL);
	pixDestroy(&pixvws);
	pixDestroy(&pixt2);

	/*separate connected columns*/
	//	s.str("");
	//	s<<"c1.30"<<"+o1."<<ts*20;
	//	Pix* pixsep = pixMorphSequenceByComponent(pixd,"c1.30+o1.130",4,ts,ts*10,NULL);
	//	Pix* pixsep2 = pixMorphCompSequence(pixsep,"c50.1",0);
	//	pixSubtract(pixsep2,pixsep2,pixsep);
	//	pixDestroy(&pixsep);
	//	pixSubtract(pixd,pixd,pixsep2);
	//	pixDestroy(&pixsep2);
	/* Expand mask to full resolution, and do filling or
	 * small dilations for better coverage. */
	pixBinary = pixExpandReplicate(pixd, 2);
	pixDestroy(&pixd);
	pixDilateBrick(pixBinary, pixBinary, 3, 3);

	Boxa* boxatext = pixConnCompBB(pixBinary, 8);
    Boxa* translated = boxaTranslate(boxatext, -borderSize, -borderSize);
	Pixa* pixaText = pixaCreateFromBoxa(pixtext, translated, NULL);
    
	//substract
	pixDestroy(&pixBinary);
    boxaDestroy(&translated);
	boxaDestroy(&boxatext);
	if (debug) {
		Pix* pixdisplay = pixaDisplay(pixaText, 0, 0);
		pixDisplay(pixdisplay, 0, 0);
		std::cout << "pagesegmentation took: " << stopTimer() << std::endl;
	}
	return pixaText;
}






void segmentComplexLayout(Pix* pixOrg,Pix* pixhm, Pix* pixb, Pixa** pixaImage, Pixa** pixaText,void(*callback) (const Pix*), bool debug) {
	ostringstream debugstring;

	if (debug) {
		startTimer();
	}

	if (debug) {
		debugstring << "Image-Text separation: " << stopTimer() << std::endl;
		startTimer();
	}
	/*create preview image*/
	Pix* pixpreview = NULL;
	if (pixhm != NULL) {
		Pix* pixb2 = pixReduceBinary2(pixb, NULL);
		Pix* pixhm2 = pixReduceBinary2(pixhm, NULL);
		Pix* pixtext32 = pixConvertTo32(pixb2);
		Pix* pixOrg2 = pixScale(pixOrg, 0.5, 0.5);
		pixDestroy(&pixb2);
		pixInvert(pixhm2, pixhm2);
		pixPaintThroughMask(pixOrg2, pixhm2, 0, 0, 0);
		pixDestroy(&pixhm2);
		Pix* pixmask = pixConvertTo1(pixOrg2, 1);
		pixCombineMasked(pixOrg2, pixtext32, pixmask);
		pixDestroy(&pixmask);
		pixDestroy(&pixtext32);
		pixpreview = pixOrg2;
	} else {
		Pix* pixb2 = pixReduceBinary2(pixb, NULL);
		Pix* pixtext32 = pixConvertTo32(pixb2);
		pixDestroy(&pixb2);
		pixpreview = pixtext32;
	}

	if (debug) {
		debugstring << "Preview-Image generation: " << stopTimer() << std::endl;
	}
    if(callback!=NULL){
        callback(pixpreview);
    }

	if (debug) {
		startTimer();

	}
	*pixaText = pagesegGetColumns(pixb, false);
	pixDestroy(&pixb);

	if (pixhm != NULL) {
		Boxa* boxa = pixConnCompBB(pixhm, 8);
		*pixaImage = pixaCreateFromBoxa(pixOrg, boxa, NULL);
		boxaDestroy(&boxa);
		pixDestroy(&pixhm);
	} else {
		*pixaImage = NULL;
	}

	if (debug) {
		debugstring << "Page Segmentation: " << stopTimer() << std::endl;
		printf("%s",debugstring.str().c_str());
	}
}



void extractImages(Pix* pixOrg, Pix** pixhm, Pix** pixg) {

	/*do a quick and dirty binarization*/
	/* Convert the RGB image to grayscale. */
	//*pixg = pixConvertRGBToGrayFast(pixOrg);
	int depth = pixGetDepth(pixOrg);
	if (depth == 32) {
		*pixg = pixConvertRGBToLuminance(pixOrg);
	} else {
		*pixg = pixOrg;
	}

	ostringstream s;
	int width = pixGetWidth(*pixg);
	int scale = 1;
	if (width > 512 && width < 2048) {
		scale = 2;
	} else if (width > 2048) {
		scale = 4;
	}

	Pix* pixsgc = pixScaleGrayRank2(*pixg, scale);

	//Pix* pixb2 = pixMaskedThreshOnBackgroundNorm(pixsgc,NULL,10,15,25,10,2,2,0.1,NULL);
	Pix* pixb2 = pixOtsuThreshOnBackgroundNorm(pixsgc, NULL, 20, 30, 100, 100, 250, 2, 2, 0.43, NULL);
	//Pix* pixb2 = pixOtsuThreshOnBackgroundNorm(pixsgc, NULL, 20, 30, 100, 100, 200, 8, 8, 0.1, NULL);
	pixDestroy(&pixsgc);

	/*find 'colourful' pixels*/
//	Pix* pixColOrg = pixScaleByIntSubsampling(pixOrg,4);
//	pixGetAverageMaskedRGB(pixColOrg,NULL,0,0,1, L_MEAN_ABSVAL,&r,&g,&b);
//	Pix* pixcol = pixColorMagnitude(pixColOrg,abs(r),abs(g),abs(b),L_MAX_DIFF_FROM_AVERAGE_2);
//	pixDestroy(&pixColOrg);
//	pixInvert(pixcol,pixcol);
//	Pix *pixmask2 =   pixOtsuThreshOnBackgroundNorm(pixcol, NULL, 20, 30, 20, 100, 200, 0, 0, 0.1, NULL);
//
//	pixDestroy(&pixcol);
//	Pix* pixmask = pixExpandBinaryPower2(pixmask2,2);
//	pixDestroy(&pixmask2);
//	pixOr(pixb2,pixb2,pixmask);
//	pixDestroy(&pixmask);
	/*create image mask*/
	/* Get seed for halftone parts */
	Pix* pixt1 = pixReduceRankBinaryCascade(pixb2, 4, 4, 3, 0);
	Pix* pixt2 = pixOpenBrick(NULL, pixt1, 3, 3);
	int hasNoImages;
	pixZero(pixt2, &hasNoImages);
	if (!hasNoImages) {
		Pix* pixhs = pixExpandBinaryPower2(pixt2, 8);

		pixDestroy(&pixt1);
		pixDestroy(&pixt2);
		/* Get mask for connected regions */
		Pix* pixm = pixCloseSafeBrick(NULL, pixb2, 4, 4);
		/* Fill seed into mask to get halftone mask */
		Pix* pixhm1 = pixSeedfillBinary(NULL, pixhs, pixm, 4);
		pixDestroy(&pixhs);
		pixDestroy(&pixm);
		/*expand mask*/
		pixCloseBrickDwa(pixhm1, pixhm1, 30, 30);
		Pix* pixhm2 = pixFillClosedBorders(pixhm1, 4);
		pixDestroy(&pixhm1);
		*pixhm = pixExpandBinaryPower2(pixhm2, 2);
		pixDestroy(&pixhm2);
	} else {
		*pixhm = NULL;
		pixDestroy(&pixt1);
		pixDestroy(&pixt2);
	}
	pixDestroy(&pixb2);
}


