/*
 * TextBlockFinder.cpp
 *
 *  Created on: Jul 13, 2014
 *      Author: renard
 */

#include "TextBlockFinder.h"
#include <sstream>
#include "TextStatFinder.h"



l_int32
pixGetAverageValueInRect(PIX       *pixs,
                     BOX       *box,
                     l_float32  *pavgval)
{
l_int32    i, j, w, h, d, wpl, bw, bh;
l_int32    xstart, ystart, xend, yend;
l_uint32   val, sum, sumCount;
l_uint32  *data, *line;

    PROCNAME("pixGetAverageValueInRect");

    if (!pavgval)
        return ERROR_INT("nothing to do", procName, 1);
    if (pavgval) *pavgval = 0;
    if (!pixs)
        return ERROR_INT("pixs not defined", procName, 1);
    if (pixGetColormap(pixs) != NULL)
        return ERROR_INT("pixs has colormap", procName, 1);
    pixGetDimensions(pixs, &w, &h, &d);
    if (d != 8 && d != 32)
        return ERROR_INT("pixs not 8 or 32 bpp", procName, 1);

    xstart = ystart = 0;
    xend = w - 1;
    yend = h - 1;
    if (box) {
        boxGetGeometry(box, &xstart, &ystart, &bw, &bh);
        xend = xstart + bw - 1;
        yend = ystart + bh - 1;
    }

    data = pixGetData(pixs);
    wpl = pixGetWpl(pixs);
    sum = 0;
    sumCount=0;
    for (i = ystart; i <= yend; i++) {
        line = data + i * wpl;
        for (j = xstart; j <= xend; j++) {
            if (d == 8){
            	sum += GET_DATA_BYTE(line, j);
            } else { /* d == 32 */
            	sum += line[j];
            }
            sumCount++;
        }
    }

    if (pavgval) *pavgval = sum / sumCount;
    return 0;
}


Numa* pixaFindMax(Pixa* pixa, Pix* pixMap){
	l_int32 w,h;
	Numa* result = numaCreate(1);
	Pix* pixMapScaled;
	l_int32 n = pixaGetCount(pixa);
	Boxa* boxa = pixaGetBoxa(pixa,L_CLONE);
	boxaGetExtent(boxa,&w,&h,NULL);
	pixMapScaled = pixScaleToSize(pixMap,w,h);

	for(int i=0; i < n; i++){
		Box* box = pixaGetBox(pixa,i,L_CLONE);
		l_float32 avg;
		l_uint32 max;
		pixGetMaxValueInRect(pixMapScaled,box,&max,NULL,NULL);
		//pixGetAverageValueInRect(pixMapScaled,box,&avg);
		numaAddNumber(result, max);
		boxDestroy(&box);

	}
	return result;

}

void TextBlockFinder::printNuma(Numa* na, const char* tag) {
	printf("%s\n", tag);
	l_int32 pc = numaGetCount(na);
	for (int i = 0; i < pc; i++) {
		l_float32 val;
		numaGetFValue(na, i, &val);
		printf("%i = %f\n", i, val);
	}
	printf("\n");
}

l_int32 pixaFindTextPropability(Pixa* pixa, Numa** result){
    PROCNAME("pixaFindTextPropability");

    if (!pixa) {
        return ERROR_INT("pixa is NULL", procName, 1);
    }

	*result = numaCreate(1);
	TextStatFinder textStatFinder = TextStatFinder(false);
	l_int32 n = pixaGetCount(pixa);

	for(int i=0; i < n; i++){
		Pix* pix = pixaGetPix(pixa,i,L_CLONE);
		l_float32 textSize, textProbability;
		bool r = textStatFinder.getTextStats(pix,&textSize,&textProbability);
		std::ostringstream s;
		s<<"testtile"<<i<<".bmp";
		pixWrite(s.str().c_str(),pix, IFF_BMP);
		printf("tile %i = %f, %i\n",i,textProbability, r);
		numaAddNumber(*result, textProbability);
		pixDestroy(&pix);
	}

	return 0;
}

Pixa* TextBlockFinder::findTextBlocks(Pix* pixb, Pix* pixTextProbability, Numa* naTextSizes,Pix** pixmorphout){
	Pixa* pixaComp;
	Pixa* pixaOrg;
	Pixa* pixaFiltered;
	Pix *pixmorph;
	Pix *pixvws;
	Pix *pixhws;
	Pix* pixi;
	Numa* naw;
	Numa* nah;
	Numa* nawhr;
	Numa* naTextProbability;
	Numa* nafr, *nafrOrg;
	Numa *na1, *na2, *na3,*na4,*na5, *na6, *nad;
	Boxa* boxa;
	std::ostringstream s;

	l_float32 textSize=0;
	numaGetMax(naTextSizes,&textSize,NULL);
	//numaGetFValue(naTextSizes,0,&textSize);
	l_int32 height = pixGetHeight(pixb);
	l_int32 width = pixGetWidth(pixb);
	s.str("");

	/* connect paragraphs candidates*/
	s << "c"<<(int)(textSize*3)<<"."<<(int)(textSize*4);
	pixmorph = pixMorphCompSequence(pixb, s.str().c_str(), 0);

	/*whitespace mask*/
	s.str("");
	s<<"o3."<<height/4;
	pixi = pixInvert(NULL,pixb);
	pixvws = pixMorphCompSequence(pixi, s.str().c_str(), 0);
	s.str("");
	s<<"o"<<(int)(width/4)<<"."<<(int)(textSize*2);
	pixhws = pixMorphCompSequence(pixi, s.str().c_str(), 0);

	pixSubtract(pixmorph, pixmorph, pixvws);
	pixSubtract(pixmorph, pixmorph, pixhws);

	if(mDebug){
		//pixDisplay(pixhws,0,0);
		//pixDisplay(pixb,0,0);
		pixDisplay(pixmorph,0,0);
	}

	/* get bounding boxes of paragraphs candidates*/
	//pixInvert(pixmorph, pixmorph);
	boxa = pixConnCompPixa(pixmorph, &pixaComp, 4);
	l_int32 boxCount = pixaGetCount(pixaComp);
	if(mDebug){
		printf("boxount = %i\n",boxCount);
	}

	if (boxCount==0){
		boxaDestroy(&boxa);
		pixDestroy(&pixvws);
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
		5. text probability from probabilith map
	*/
	pixaFindDimensions(pixaComp, &naw, &nah);
	pixaOrg = pixaCreateFromBoxa(pixb,boxa,NULL);
	nafrOrg = pixaFindAreaFraction(pixaOrg);
	nafr = pixaFindAreaFraction(pixaComp);
	pixaFindTextPropability(pixaOrg,&naTextProbability);
	if(mDebug){
		//printNuma(naTextProbability,"text prob");
	}
	nawhr = pixaFindWidthHeightRatio(pixaComp);
	// Build the indicator arrays for the set of components,
	// based on thresholds and selection criteria.
	na1 = numaMakeThresholdIndicator(nah, textSize*2, L_SELECT_IF_GTE);
	na2 = numaMakeThresholdIndicator(naw, textSize*4, L_SELECT_IF_GTE);
	na3 = numaMakeThresholdIndicator(nafrOrg, 0.10, L_SELECT_IF_GTE);
	na4 = numaMakeThresholdIndicator(nafrOrg, 0.50, L_SELECT_IF_LTE);
	na5 = numaMakeThresholdIndicator(nafr, 0.30, L_SELECT_IF_GTE);
	na6 = numaMakeThresholdIndicator(naTextProbability, 0.70, L_SELECT_IF_GTE);
	printNuma(naTextProbability,"text prob");
	// Combine the indicator arrays logically to find
	// the components that will be retained.
	nad = numaLogicalOp(NULL, na1, na2, L_INTERSECTION);
//	numaLogicalOp(nad, nad, na3, L_INTERSECTION);
//	numaLogicalOp(nad, nad, na4, L_INTERSECTION);
//	numaLogicalOp(nad, nad, na5, L_INTERSECTION);
	numaLogicalOp(nad, nad, na6, L_INTERSECTION);
	pixaFiltered = pixaSelectWithIndicator(pixaComp,nad,NULL);
	if(pixmorphout!=NULL){
		*pixmorphout = pixClone(pixmorph);
	}

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
	numaDestroy(&na6);
	numaDestroy(&nad);
	numaDestroy(&naTextProbability);
	boxaDestroy(&boxa);
	pixaDestroy(&pixaComp);
	pixaDestroy(&pixaOrg);
	pixDestroy(&pixmorph);
	pixDestroy(&pixvws);
	pixDestroy(&pixhws);
	pixDestroy(&pixi);
	return pixaFiltered;
}


TextBlockFinder::TextBlockFinder(bool debug) {
	mDebug = debug;
}

TextBlockFinder::~TextBlockFinder() {
}



