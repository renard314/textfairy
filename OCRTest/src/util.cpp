/*
 * util.cpp
 *
 *  Created on: Mar 4, 2012
 *      Author: renard
 *
 *      Utility functions used during the ocr processing
 */

#include "util.h"
#include "binarize.h"
#include "pageseg.h"

using namespace std;

Pix* bookpage(Pix* pixOrg, Pix** pixFinal, void(*messageJavaCallback) (int), void(*pixJavaCallback) (Pix*,bool,bool), bool showIntermediate, bool debug) {
	ostringstream debugstring;
	Pix *pixhm, *pixb = NULL;
	if (debug) {
		startTimer();
	}

	messageJavaCallback(MESSAGE_IMAGE_DETECTION);
	Pix* pixsg;
	extractImages(pixOrg, &pixhm, &pixsg);
	pixJavaCallback(pixsg, false, true);
	binarize(pixsg, pixhm, &pixb);
	pixDestroy(&pixsg);

	if (debug) {
		debugstring << "Image-Text separation: " << stopTimer() << std::endl;
		startTimer();
	}

	if (TRUE == showIntermediate) {
		pixJavaCallback(pixb, debug, TRUE);
	}

	Pix* pixtext = NULL;
	L_DEWARP *dew = dewarpCreate(pixb, 0, 5, 7, 1);
	int buildresult = dewarpBuildModel(dew, 0);
	int applyResult = -1;
	if (buildresult == 0) {
		messageJavaCallback(MESSAGE_IMAGE_DEWARP);
		applyResult = dewarpApplyDisparity(dew, pixb, 0);
		if (applyResult == 0) {
			pixtext = pixClone(dew->pixd);
		}
	} else {
		dewarpDestroy(&dew);
	}

	if (pixtext == NULL) {
		pixtext = pixb;
	} else {
		if (TRUE == showIntermediate) {
			debugstring << "showing dewarped pix" << std::endl;
			pixJavaCallback(pixtext, debug, TRUE);
		}
		pixDestroy(&pixb);
	}

	if (debug) {
		debugstring << "dewarp : " << stopTimer() << std::endl;
	}

	if (pixhm != NULL) {
		if (debug) {
			startTimer();
		}

		Pix* pixi = pixConvertTo32(pixtext);
		pixInvert(pixhm, pixhm);
		pixPaintThroughMask(pixOrg, pixhm, 0, 0, 0);
		pixDestroy(&pixhm);
		if (buildresult == 0) {
			if (applyResult==0){
				pixDestroy(&dew->pixd);
			}
			dewarpApplyDisparity(dew, pixOrg, 0);
			*pixFinal = pixClone(dew->pixd);
			dewarpDestroy(&dew);
		} else {
			*pixFinal = pixClone(pixOrg);
		}
		Pix* pixmask = pixConvertTo1(*pixFinal, 1);
		pixCombineMasked(*pixFinal, pixi, pixmask);
		pixDestroy(&pixmask);
		pixDestroy(&pixi);

		if (debug) {
			debugstring << "image restoration: " << stopTimer() << std::endl;
		}
		if (TRUE == showIntermediate) {
			pixJavaCallback(*pixFinal, debug, TRUE);
		}
	} else {
		if (dew != NULL) {
			dewarpDestroy(&dew);
		}
		startTimer();
		*pixFinal = pixConvertTo32(pixtext);
		debugstring << "image restoration: " << stopTimer() << std::endl;
	}

	return pixtext;
}



/**
 * translate all boxa which are below bottom dy pixel down
 * translate all boxa which are to the right of right dy pixel to the right
 */
void translateBoxa(Pixa* pixa, l_int32 dx, l_int32 dy, l_int32 right, l_int32 bottom) {
	log("translateBoxa dx=%i dy=%i, right=%i, bottom=%i",dx,dy,right,bottom);
	bool moveDown = dy > 0;
	bool moveRight = dx > 0;
	l_int32 count = pixaGetBoxaCount(pixa);
	if (moveDown || moveRight) {
		//move all boxes on the right side to the right
		//move all boxes on the bottom down
		for (int j = 0; j < count; j++) {
			l_int32 x, y, h, w;
			Box* b = pixaGetBox(pixa, j, L_CLONE);
			boxGetGeometry(b, &x, &y, &w, &h);
			if (moveRight && x >= right) {
				log("moving pix %i to %i pixel to right",j,dx);
				x += dx;
			}
			if (moveDown && y >= bottom) {
				log("moving pix %i to %i pixel down",j,dy);
				y += dy;
			}
			boxSetGeometry(b, x, y, w, h);
			boxDestroy(&b);
		}
	}
}

/**
 * destroys all pixa
 */
void combineSelectedPixa(Pixa* pixaText, Pixa* pixaImage, l_int32* textindexes, l_int32 textCount, l_int32* imageindexes, l_int32 imageCount,void (*callbackMessage) (const int), Pix** pPixFinal, Pix** pPixOcr, Boxa** pBoxaColumns, bool debug){
	ostringstream debugstring;

	if (debug) {
		int total = pixaGetCount(pixaText);
		debugstring << total << " Texts";
		if (pixaImage != NULL) {
			total = pixaGetCount(pixaImage);
		}
		debugstring << " and " << total << " Images in total" << std::endl;

		debugstring << textCount << " text indexes: [";
		for (int i = 0; i < textCount; i++) {
			debugstring << textindexes[i];
			if (i < textCount - 1) {
				debugstring << ",";
			}
		}
		debugstring << "]" << std::endl;

		debugstring << imageCount << " image indexes: [";
		for (int i = 0; i < imageCount; i++) {
			debugstring << imageindexes[i];
			if (i < imageCount - 1) {
				debugstring << ",";
			}
		}
		debugstring << "]" << std::endl;
	}

	//copy marked text pix into new pixa
	Pixa* pixaSelectedColumns = pixaCreate(textCount);
	for (int i = 0; i < textCount; i++) {
		int textIndex = textindexes[i];
		const l_int32 border = 10;
		Pix* p = pixaGetPix(pixaText, textIndex, L_CLONE);
		Pix* p_with_border = pixAddBorder(p,border,0);
		pixDestroy(&p);
		Box* b = pixaGetBox(pixaText, textIndex, L_CLONE);
		boxAdjustSides(b, b, -border, border, -border, border);
		pixaAddPix(pixaSelectedColumns, p_with_border, L_INSERT);
		pixaAddBox(pixaSelectedColumns, b, L_INSERT);
	}
	pixaDestroy(&pixaText);

	/*dewarp text regions*/

	for (int i = 0; i < textCount; i++) {
		Pix* pixtext = pixaGetPix(pixaSelectedColumns, i, L_CLONE);
		L_DEWARP *dew = dewarpCreate(pixtext, 7, 30, 10, 1);
		int x, y, w, dw, dh, h, dx = 0, dy = 0;
		Box* b = pixaGetBox(pixaSelectedColumns, i, L_CLONE);
		pixaGetBoxGeometry(pixaSelectedColumns, i, &x, &y, &w, &h);

		int buildresult = dewarpBuildModel(dew, 0);
		if (buildresult == 0) {
			callbackMessage(MESSAGE_IMAGE_DEWARP);
			int applyresult = dewarpApplyDisparity(dew, pixtext, 0);
			if (applyresult == 0) {
				dw = pixGetWidth(dew->pixd);
				dh = pixGetHeight(dew->pixd);
				dx = dw - w;
				dy = dh - h;
				boxSetGeometry(b, x, y, dw, dh);
				pixaReplacePix(pixaSelectedColumns, i, pixClone(dew->pixd), b);
				l_int32 right = x + w;
				l_int32 bottom = y + h;
				translateBoxa(pixaSelectedColumns, dx, dy, right, bottom);
			}
		}
		dewarpDestroy(&dew);
	}

	callbackMessage(MESSAGE_ASSEMBLE_PIX);

	int xb, yb, wb, hb;
	int left = MAX_INT16;
	int right = 0;
	int top = MAX_INT16;
	int bottom = 0;
	/*get the extend of final pix*/
	for (int i = 0; i < imageCount; i++) {
		int index = imageindexes[i];
		pixaGetBoxGeometry(pixaImage, index, &xb, &yb, &wb, &hb);
		if (left > xb) {
			left = xb;
		}
		if (right < (xb + wb)) {
			right = (xb + wb);
		}
		if (top > yb) {
			top = yb;
		}
		if (bottom < (yb + hb)) {
			bottom = (yb + hb);
		}
	}

	int dewarpCount = pixaGetCount(pixaSelectedColumns);
	for (int i = 0; i < dewarpCount; i++) {
		//int index = textindexes[i];
		pixaGetBoxGeometry(pixaSelectedColumns, i, &xb, &yb, &wb, &hb);
		if (left > xb) {
			left = xb;
		}
		if (right < (xb + wb)) {
			right = (xb + wb);
		}
		if (top > yb) {
			top = yb;
		}
		if (bottom < (yb + hb)) {
			bottom = (yb + hb);
		}
	}

	l_int32 border = 60;
	l_int32 width = (right - left) + 2 * border;
	l_int32 height = (bottom - top) + 2 * border;
	Pix* pixFinal = pixCreate(width, height, 32);
	pixSetBlackOrWhite(pixFinal, L_SET_WHITE);
	Pix* pixOCR = pixCreate(width, height, 1);
	pixSetBlackOrWhite(pixOCR, L_SET_WHITE);
	int xoffset = left - border;
	int yoffset = top - border;

	if (debug) {
		debugstring << "extend of final pix: (" << right << "," << bottom << ")" << std::endl;
		debugstring << "offset: (" << xoffset << "," << yoffset << ")" << std::endl;
		startTimer();
	}

	/*copy selected pix into final pix*/
	for (int i = 0; i < imageCount; i++) {
		int index = imageindexes[i];
		if (pixaGetBoxGeometry(pixaImage, index, &xb, &yb, &wb, &hb)) {
			continue;
		}
		Pix* pixi = pixaGetPix(pixaImage, index, L_CLONE);
		pixRasterop(pixFinal, xb - xoffset, yb - yoffset, wb, hb, PIX_SRC, pixi, 0, 0);
	}
	printf("%s","after copying images");


	Boxa* boxaColumns = boxaCreate(0);
	for (int i = 0; i < dewarpCount; i++) {
		//int index = textindexes[i];
		if (pixaGetBoxGeometry(pixaSelectedColumns, i, &xb, &yb, &wb, &hb)) {
			continue;
		}
		Pix* pixt = pixaGetPix(pixaSelectedColumns, i, L_CLONE);
		Pix* pixt32 = pixConvertTo32(pixt);
		pixRasterop(pixOCR, xb - xoffset, yb - yoffset, wb, hb, PIX_SRC, pixt, 0, 0);
		pixRasterop(pixFinal, xb - xoffset, yb - yoffset, wb, hb, PIX_SRC, pixt32, 0, 0);
		pixDestroy(&pixt32);
		Box* boxColumn = boxCreate(xb - xoffset, yb - yoffset, wb, hb);
		boxaAddBox(boxaColumns,boxColumn, L_INSERT);
	}

	pixaDestroy(&pixaImage);

	if (debug) {
		debugstring << "time to assemble final pix: " << stopTimer() << std::endl;
	}

	*pPixFinal = pixFinal;
	*pPixOcr = pixOCR;
	*pBoxaColumns = boxaColumns;
}




