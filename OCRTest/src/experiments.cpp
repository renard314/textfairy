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
#include "image_processing.h"
using namespace std;



void binarize3(Pix* pixsg, Pix* pixhm, Pix** pixb) {
	Pix* pixg;

	/* Remove the text in the fg. */
	Pix* pixc = pixCloseGray(pixsg, 25, 25);
	/* Smooth the bg with a convolution. */
	Pix* pixsm = pixBlockconv(pixc, 15, 15);
	pixDestroy(&pixc);

	/* Normalize for uneven illumination on gray image. */
	pixBackgroundNormGrayArrayMorph(pixsm, pixhm, 4, 5, 200, &pixg);
	pixc = pixApplyInvBackgroundGrayMap(pixsg, pixg, 4, 4);
	pixDestroy(&pixg);
	pixDestroy(&pixsm);

	/* Increase the dynamic range. */
	Pix* pixd = pixGammaTRC(NULL, pixc, 1.0, 30, 180);
	pixDestroy(&pixc);

	/* Threshold to 1 bpp. */
	/* Make the fg/bg estimates */
	NUMA* na = pixGetGrayHistogram(pixd, 4);
	int thresh;
	numaSplitDistribution(na, 0.1, &thresh, NULL, NULL, NULL, NULL, NULL);
	numaDestroy(&na);
	*pixb = pixThresholdToBinary(pixd, thresh);
	pixDestroy(&pixd);

	if (pixhm != NULL) {
		pixSetMasked(*pixb, pixhm, 0);
	}
}

/**
 *older version
 */
void binarize2(Pix* pixsg, Pix* pixhm, Pix** pixb) {
	Pix* pixg;
	L_TIMER timer = startTimerNested();

	ostringstream s;

	if (pixhm != NULL) {
		pixSetMasked(pixsg, pixhm, 255);
	}

	/* Normalize for uneven illumination on gray image. */
	pixBackgroundNormGrayArrayMorph(pixsg, pixhm, 4, 9, 220, &pixg);
	Pix* pixc = pixApplyInvBackgroundGrayMap(pixsg, pixg, 4, 4);
	pixDestroy(&pixg);

	s << "background norm: " << stopTimerNested(timer) << std::endl;
	timer = startTimerNested();

	/* Increase the dynamic range. */
	pixg = pixGammaTRC(NULL, pixc, 1.0, 30, 180);
	pixDestroy(&pixc);

	/* Threshold to 1 bpp. */
	int width = pixGetWidth(pixg) >> 3;
	pixSauvolaBinarizeTiled(pixg, width, 0.05, 1, 1, NULL, pixb);
	pixDestroy(&pixg);
	s << "sauvola width " << width << std::endl;
	s << "sauvola binarize" << stopTimerNested(timer) << std::endl;
	timer = startTimerNested();

	/*remove salt and pepper noise*/
	Pix* pixacc = pixBlockconvAccum(*pixb);
	Pix* pixRank = pixBlockrank(*pixb, pixacc, 16, 16, 0.1);
	pixDestroy(&pixacc);
	pixAnd(*pixb, *pixb, pixRank);
	pixDestroy(&pixRank);

	s << "block rank filter" << stopTimerNested(timer) << std::endl;
	printf("%s",s.str().c_str());
}

Pix* twoTresholdBinarization2(Pix* pixg) {
	Pix* pixn = pixBackgroundNorm(pixg, NULL, NULL, 10, 15, 100, 50, 255, 2, 2);

	Pix* edges = pixSobelEdgeFilter(pixg, L_ALL_EDGES);
	NUMA* na = pixGetGrayHistogram(edges, 4);
	int thresh;
	numaSplitDistribution(na, 0.1, &thresh, NULL, NULL, NULL, NULL, NULL);
	numaDestroy(&na);

	Pix* pixt2 = pixThresholdToBinary(edges, thresh);
	pixInvert(pixt2, pixt2);
	Pix* pixm = pixMorphSequence(pixt2, "d21.21", 0);
	//pixDestroy(&pixt1);
	pixDestroy(&pixt2);
	int w, h;
	Pix* pixt3;
	l_uint32 val;
	pixGetDimensions(pixg, &w, &h, NULL);
	pixOtsuAdaptiveThreshold(pixg, w, h, 0, 0, 0.1, &pixt3, NULL);
	if (pixt3) {
		pixGetPixel(pixt3, 0, 0, &val);
	}
	pixDestroy(&pixt3);

	Pix* pixd = pixThresholdToBinary(pixn, val + 30);
	Pix* pixt4 = pixThresholdToBinary(pixn, 190);
	pixCombineMasked(pixd, pixt4, pixm);
	pixDestroy(&pixt4);
	pixDestroy(&pixm);
	pixDestroy(&pixn);
	return pixd;
}

void binarizeCurrent(Pix* pixsg, Pix* pixhm, Pix** pixb) {
	Pix* pixg;

	/* Remove the text in the fg. */
	Pix* pixc = pixCloseGray(pixsg, 25, 25);
	/* Smooth the bg with a convolution. */
	Pix* pixsm = pixBlockconv(pixc, 15, 15);
	pixDestroy(&pixc);

	/* Normalize for uneven illumination on gray image. */
	pixBackgroundNormGrayArrayMorph(pixsm, pixhm, 4, 5, 200, &pixg);
	pixc = pixApplyInvBackgroundGrayMap(pixsg, pixg, 4, 4);
	pixDestroy(&pixg);
	pixDestroy(&pixsm);

	/* Increase the dynamic range. */
	Pix* pixd = pixGammaTRC(NULL, pixc, 1.0, 30, 180);
	pixDestroy(&pixc);

	/* Threshold to 1 bpp. */
	/* Make the fg/bg estimates */
	NUMA* na = pixGetGrayHistogram(pixd, 4);
	int thresh;
	numaSplitDistribution(na, 0.1, &thresh, NULL, NULL, NULL, NULL, NULL);
	numaDestroy(&na);
	*pixb = pixThresholdToBinary(pixd, thresh);
	pixDestroy(&pixd);

	if (pixhm != NULL) {
		pixSetMasked(*pixb, pixhm, 0);
	}
}

