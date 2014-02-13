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



Pix* bookpage(Pix* pixOrg, Pix** pixFinal, void (*messageJavaCallback)(int),
		void (*pixJavaCallback)(Pix*, bool, bool), bool showIntermediate,
		bool debug) {
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

    l_int32 border = pixGetWidth(pixb)/25;
    if (border < 30){
        border = 30;
    }
    if (debug>0){
    	log("adding %i pixel border",border);
    }
	if (debug>0){
		pixWrite("binarized.bmp", pixb, IFF_BMP);
	}

    Pix* pixWithBorder = pixAddBorder(pixb,border,0x0);
    Pix* pixOrgWithBorder = pixAddBorder(pixOrg,border,0x0);
    pixDestroy(&pixb);
    pixb =pixWithBorder;


	if (debug>0){
		pixDisplay(pixWithBorder,0,0);
		pixWrite("binarized_border.bmp", pixb, IFF_BMP);
	}

	pixDestroy(&pixsg);

	if (debug>1) {
		debugstring << "Image-Text separation: " << stopTimer() << std::endl;
		startTimer();
	}

	if (TRUE == showIntermediate) {
		pixJavaCallback(pixb, debug, TRUE);
	}

	Pix* pixtext = NULL;
	L_DEWARP *dew = dewarpCreate(pixb, 0, 5, 10, 0);
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

	if (debug>1) {
		debugstring << "dewarp : " << stopTimer() << std::endl;
	}

	if (pixhm != NULL) {
		if (debug>1) {
			startTimer();
		}

		Pix* pixi = pixConvertTo32(pixtext);
		pixInvert(pixhm, pixhm);
		pixPaintThroughMask(pixOrgWithBorder, pixhm, 0, 0, 0);
		pixDestroy(&pixhm);
		if (buildresult == 0) {
			if (applyResult == 0) {
				pixDestroy(&dew->pixd);
			}
			dewarpApplyDisparity(dew, pixOrgWithBorder, 0);
			*pixFinal = pixClone(dew->pixd);
			dewarpDestroy(&dew);
		} else {
			*pixFinal = pixClone(pixOrgWithBorder);
		}
		Pix* pixmask = pixConvertTo1(*pixFinal, 1);
		pixCombineMasked(*pixFinal, pixi, pixmask);
		pixDestroy(&pixmask);
		pixDestroy(&pixi);

		if (debug>1) {
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
    pixDestroy(&pixOrgWithBorder);
	return pixtext;
}

/**
 * translate all boxa which are below top dy pixel down
 * translate all boxa which are to the right of left dy pixel to the right
 */
void translateBoxa(Pixa* pixa, l_int32 dx, l_int32 dy, l_int32 left, l_int32 top) {
	log("translateBoxa dx=%i dy=%i, left=%i, top=%i", dx, dy, left, top);
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
			if (moveRight && x > left) {
				log("moving pix %i to %i pixel to right", j, dx);
				x += dx;
			}
			if (moveDown && y > top) {
				log("moving pix %i to %i pixel down", j, dy);
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
void combineSelectedPixa(Pixa* pixaText, Pixa* pixaImage, l_int32* textindexes, l_int32 textCount, l_int32* imageindexes, l_int32 imageCount, void (*callbackMessage)(const int), Pix** pPixFinal, Pix** pPixOcr,Boxa** pBoxaColumns, bool debug) {
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
		const l_int32 border = 40;
		Pix* p = pixaGetPix(pixaText, textIndex, L_CLONE);
		Pix* p_with_border = pixAddBorder(p, border, 0);
		pixDestroy(&p);
		Box* b = pixaGetBox(pixaText, textIndex, L_CLONE);
		boxAdjustSides(b, b, -border, border, -border, border);
		pixaAddPix(pixaSelectedColumns, p_with_border, L_INSERT);
		pixaAddBox(pixaSelectedColumns, b, L_INSERT);
		int x,y,w,h;
		boxGetGeometry(b,&x,&y,&w,&h);
		translateBoxa(pixaText,border,border,x,y);
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
				translateBoxa(pixaSelectedColumns, dx, dy, x, y);
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
		debugstring << "extend of final pix: (" << right << "," << bottom << ")"
				<< std::endl;
		debugstring << "offset: (" << xoffset << "," << yoffset << ")"
				<< std::endl;
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
	printf("%s", "after copying images");

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
		boxaAddBox(boxaColumns, boxColumn, L_INSERT);
	}

	if (pixaImage!=NULL){
		pixaDestroy(&pixaImage);
	}

	if (debug) {
		debugstring << "time to assemble final pix: " << stopTimer() << std::endl;
		printf("%s",debugstring.str().c_str());
	}

	*pPixFinal = pixFinal;
	*pPixOcr = pixOCR;
	*pBoxaColumns = boxaColumns;
}

std::string GetHTMLText(tesseract::ResultIterator* res_it, const float minConfidenceToShowColor) {
	int lcnt = 1, bcnt = 1, pcnt = 1, wcnt = 1;
	ostringstream html_str;
	bool isItalic = false;
	bool para_open = false;

	for (; !res_it->Empty(tesseract::RIL_BLOCK); wcnt++) {
		if (res_it->Empty(tesseract::RIL_WORD)) {
			res_it->Next(tesseract::RIL_WORD);
			continue;
		}

		if (res_it->IsAtBeginningOf(tesseract::RIL_PARA)) {
			if (para_open) {
				html_str << "</p>";
				pcnt++;
			}
			html_str << "<p>";
			para_open = true;
		}

		// Now, process the word...
		const char *font_name;
		bool bold, italic, underlined, monospace, serif, smallcaps;
		int pointsize, font_id;
		font_name = res_it->WordFontAttributes(&bold, &italic, &underlined,
				&monospace, &serif, &smallcaps, &pointsize, &font_id);

		float confidence = res_it->Confidence(tesseract::RIL_WORD);
		bool addConfidence = false;

		if (italic && !isItalic) {
			html_str << "<strong>";
			isItalic = true;
		} else if (!italic && isItalic) {
			html_str << "</strong>";
			isItalic = false;
		}

		char* word = res_it->GetUTF8Text(tesseract::RIL_WORD);
		bool isSpace = strcmp(word, " ") == 0;
		delete[] word;
		if (confidence < minConfidenceToShowColor && !isSpace) {
			addConfidence = true;
			html_str << "<font conf='";
			html_str << (int) confidence;
			html_str << "' color='#DE2222'>";
		}

		do {
			const char *grapheme = res_it->GetUTF8Text(tesseract::RIL_SYMBOL);
			if (grapheme && grapheme[0] != 0) {
				if (grapheme[1] == 0) {
					switch (grapheme[0]) {
					case '<':
						html_str << "&lt;";
						break;
					case '>':
						html_str << "&gt;";
						break;
					case '&':
						html_str << "&amp;";
						break;
					case '"':
						html_str << "&quot;";
						break;
					case '\'':
						html_str << "&#39;";
						break;
					default:
						html_str << grapheme;
						break;
					}
				} else {
					html_str << grapheme;
				}
			}
			delete[] grapheme;
			res_it->Next(tesseract::RIL_SYMBOL);
		} while (!res_it->Empty(tesseract::RIL_BLOCK)
				&& !res_it->IsAtBeginningOf(tesseract::RIL_WORD));

		if (addConfidence == true) {
			html_str << "</font>";
		}

		html_str << " ";
	}
	if (isItalic) {
		html_str << "</strong>";
	}
	if (para_open) {
		html_str << "</p>";
		pcnt++;
	}
	return html_str.str();
}
