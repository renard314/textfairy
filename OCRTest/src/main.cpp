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
 * main.cpp
 *
 *  Created on: Jul 4, 2011
 *      Author: renard
 */

#include <allheaders.h>
#include <baseapi.h>
#include <environ.h>
#include <publictypes.h>
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <fstream>
#include <sstream>

#include "binarize.h"
#include "pageseg.h"
#include "util.h"

#include <Codecs.hh>
#include <pdf.hh>
#include <hocr.hh>
#include <jpeg.hh>
#include "text_search.h"

using namespace std;
using namespace tesseract;

void createPdf(Pix* pix, const char* hocrText) {
	printf("%s", "creating pdf");

	std::ofstream pdfOutStream("test.pdf");
	PDFCodec* pdfContext = new PDFCodec(&pdfOutStream);
	Pix* pixd = pixConvert1To8(NULL, pix, 255, 0);

	pixWrite("/Users/renard/Desktop/pdf_image.jpg", pixd, IFF_JFIF_JPEG);
	bool sloppy = false;
	bool overlayImage = false;
	Image image;
	image.w = image.h = 0;
	std::string fileName("/Users/renard/Desktop/pdf_image.jpg");
	if (!ImageCodec::Read(fileName, image)) {
		std::cout << "Error reading input file.";
	}

	if (image.resolutionX() <= 0 || image.resolutionY() <= 0) {
		std::cout << "Warning: Image x/y resolution not set, defaulting to: 300 ";
		image.setResolution(300, 300);
	}

	unsigned int res = image.resolutionX();

	std::stringstream hocr(hocrText);

	pdfContext->beginPage(72. * image.w / res, 72. * image.h / res);
	pdfContext->setFillColor(0, 0, 0);
	hocr2pdf(hocr, pdfContext, res, sloppy, !overlayImage);

	if (overlayImage) {
		pdfContext->showImage(image, 0, 0, 72. * image.w / res, 72. * image.h / res);
	}

	delete pdfContext;
}

void doOCR(Pix* pixb, ETEXT_DESC* monitor, ostringstream* s, int debug_level = 0) {
	tesseract::TessBaseAPI api;
	api.Init("/Users/renard/Desktop/devel/textfairy/tesseract-ocr-read-only/", "eng+deu", tesseract::OEM_DEFAULT);
	api.SetPageSegMode(tesseract::PSM_AUTO);
	api.SetImage(pixb);
	const char* hocrtext; //= api.GetHOCRText(monitor, 0);
	createPdf(pixb, hocrtext);
	//*s << hocrtext;
	//const char* debugText = api.GetHTMLText(20);
	//const char* debugText = api.GetUTF8Text();
	api.Recognize(monitor);
	ResultIterator* it = api.GetIterator();
	std::string debugText = GetHTMLText(it, 70);
	*s << debugText;
	if (debug_level > 1) {
//		std::cout<<"html: " <<utf8text<<std::endl;
		std::cout << "utf8text: " << "\n" << debugText << std::endl;
		std::cout << "ocr: " << stopTimer() << std::endl << "confidence: " << api.MeanTextConf() << std::endl;
	} else if (debug_level > 0) {
		std::cout << "confidence: " << api.MeanTextConf() << std::endl;
	}
//	delete[] utf8text;
//	delete[] hocrtext;
	api.End();
}

bool onProgressChanged(int progress, int left, int right, int top, int bottom) {

	std::cout << left << ":" << top << ":" << right << ":" << bottom;
	return true;
}

void pixCallBack(Pix* pix, bool b1, bool b2) {
	//pixDisplay(pix, 0, 0);
}

void messageCallback(int messageId) {

}
void messageCallback(const Pix* pix) {

}

void onePictureWithColumns(const char* filename, int index) {
	ostringstream s;
	Pix* pixTextlines = NULL;
	Pixa* pixaTexts, *pixaImages;
	Pix* pixb, *pixhm;
	Pix* pixsg;
	Pix* pixFinal;
	Pix* pixOcr;
	Pix* pixOrg = pixRead(filename);
	Boxa* boxaColumns;

	s << index;
	printf("%i\n", index);
	L_TIMER timer = startTimerNested();

	extractImages(pixOrg, &pixhm, &pixsg);
	binarize(pixsg, pixhm, &pixb);
	pixDestroy(&pixsg);

	segmentComplexLayout(pixOrg, pixhm, pixb, &pixaImages, &pixaTexts, messageCallback, true);
	ostringstream hocr;
	ostringstream utf8text;
	l_int32 textCount = pixaGetCount(pixaTexts);
	l_int32* imageIndexes = new l_int32[0];
	l_int32* textIndexes = new l_int32[2];
	textIndexes[0] = 5;
	textIndexes[1] = 7;
	textCount = 2;
	//for (int i = 0; i < textCount; i++) {
	//	textIndexes[i] = i;
	//}
	combineSelectedPixa(pixaTexts, pixaImages, textIndexes, textCount, imageIndexes, 0, messageCallback, &pixFinal, &pixOcr, &boxaColumns, true);
	pixWrite("dewarpedColumns.bmp", pixOcr, IFF_BMP);
	printf("total time = %f", stopTimerNested(timer));
	renderTransformedBoxa(pixOcr, boxaColumns, 255);
	pixDisplay(pixOcr, 0, 0);

	delete[] textIndexes;
	boxaDestroy(&boxaColumns);
	pixDestroy(&pixFinal);
	pixDestroy(&pixOcr);
	pixDestroy(&pixOrg);
	pixDestroy(&pixb);
	pixDestroy(&pixhm);
	pixDestroy(&pixTextlines);
	pixaDestroy(&pixaTexts);
}

void onPictureOnlyBinarize(const char* filename, int index) {
	Pix *pixhm, *pixb = NULL;
	Pix* pixOrg = pixRead(filename);
	Pix* pixsg;
	extractImages(pixOrg, &pixhm, &pixsg);
	binarize(pixsg, pixhm, &pixb);
	//pixDisplay(pixb, 0, 0);
	pixWrite("binarized.bmp", pixb, IFF_BMP);

	pixDestroy(&pixb);
	pixDestroy(&pixsg);
	pixDestroy(&pixOrg);

}
int analyseTile(Pix* pix, int x, int y) {
	NUMA* histo = pixGetGrayHistogram(pix, 1);
	NUMA* norm = numaNormalizeHistogram(histo, 1);

	/*
	 *     (1) The returned na consists of sets of four numbers representing
	 *         the peak, in the following order:
	 *            left edge; peak center; right edge; normalized peak area
	 */
	NUMA* peaks = numaFindPeaks(norm, 2, 0.3, 0.1);
	NUMA* peaksY = numaCreate(0);
	NUMA* peaksX = numaCreate(0);

	int peakCount = numaGetCount(peaks);
	int i = 0;
	while (i < peakCount) {
		l_int32 left, center, right;
		l_float32 area;
		numaGetIValue(peaks, i++, &left);
		numaGetIValue(peaks, i++, &center);
		numaGetIValue(peaks, i++, &right);
		numaGetFValue(peaks, i++, &area);
		l_float32 value;
		numaGetFValue(norm, left, &value);
		numaAddNumber(peaksX, left);
		numaAddNumber(peaksY, value);

		numaGetFValue(norm, center, &value);
		numaAddNumber(peaksX, center);
		numaAddNumber(peaksY, value);

		numaGetFValue(norm, right, &value);
		numaAddNumber(peaksX, right);
		numaAddNumber(peaksY, value);

	}

	GPLOT *gplot;
	ostringstream name;
	name << "tile(" << x << "," << y << ")";
	ostringstream rootName;
	rootName << x << y;
	gplot = gplotCreate(rootName.str().c_str(), GPLOT_X11, name.str().c_str(), "x", "y");
	ostringstream title;
	title << peakCount / 4 << " peaks";
	gplotAddPlot(gplot, NULL, norm, GPLOT_LINES, "histogram");
	gplotAddPlot(gplot, peaksX, peaksY, GPLOT_POINTS, title.str().c_str());
	//gplotMakeOutput(gplot);
	gplotDestroy(&gplot);

	numaDestroy(&peaks);
	numaDestroy(&peaksX);
	numaDestroy(&peaksY);
	numaDestroy(&histo);
	numaDestroy(&norm);
	return 0;
}
Pix* pixAnnotate(Pix* pixb, const char* textstr) {
	L_BMF *bmf;
	bmf = bmfCreate("/Users/renard/devel/textfairy/leptonica-1.68/prog/fonts", 4);
	Pix* pixt = pixAddSingleTextblock(pixb, bmf, textstr, 0x00ff0000, L_ADD_ABOVE, NULL);
	bmfDestroy(&bmf);
	return pixt;
}

void pixaAddPixWithTitle(Pixa* pixa, Pix* pix, const char* title) {
	Pix* pixt = pixAnnotate(pix, title);
	pixaAddPix(pixa, pixt, L_INSERT);
}
void layoutDetect(const char* filename, int index) {
	Pix* pixOrg = pixRead(filename);
	Pix* pixg = pixConvertRGBToLuminance(pixOrg);
	Pix* pixb;
	Boxa* textRegions = pixFindTextRegions(pixg,&pixb);
	renderTransformedBoxa(pixOrg, textRegions, 255);
	Pix* pix32 = pixConvert1To32(NULL,pixb,0,0xffffffff);
	pixDisplay(pix32,0,0);
	pixDestroy(&pixg);
	pixDestroy(&pix32);
	pixDestroy(&pixb);
	boxaDestroy(&textRegions);
}

void layoutDetectDebug(const char* filename, int index) {
	log("%s", filename);
	Pix* pixb;
	Pix* pixbDisplay;
	Pix* pixd;
	Pix* pixOrg = pixRead(filename);
	Pix* pixg = pixConvertRGBToLuminance(pixOrg);
	Pixa* pixaDisplay = pixaCreate(0);
	pixWrite("grey.bmp", pixg, IFF_BMP);
	pixaAddPixWithTitle(pixaDisplay, pixOrg, "original");

	Pix* pix_edge = pixTwoSidedEdgeFilter(pixg, L_HORIZONTAL_EDGES);

	float scorefract = 0.05;
	int thresh;
	int nx = 2;
	int ny = 2;

	PIXTILING* pt = pixTilingCreate(pix_edge, nx, ny, 0, 0, 0, 0);
	pixTilingGetCount(pt,&nx,&ny);
	pixaAddPixWithTitle(pixaDisplay,pix_edge,"edge");

	Pix* pixth = pixCreate(nx, ny, 8);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, j, i);
			int w = pixGetWidth(pixt);
			int h = pixGetHeight(pixt);
			printf("w=%i, h=%i",w,h);
			pixSplitDistributionFgBg(pixt, scorefract, 1, &thresh, NULL, NULL, 1);
			pixSetPixel(pixth, j, i, thresh);
			pixDestroy(&pixt);
		}
	}
	pixTilingDestroy(&pt);
	l_int32 w = pixGetWidth(pix_edge);
	l_int32 h = pixGetHeight(pix_edge);
	pt = pixTilingCreate(pix_edge, nx, ny, 0, 0, 0, 0);
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

	//Pix* pixeh = pixThresholdToBinary(pix_edge, th);

	pixaAddPixWithTitle(pixaDisplay, pixeh, "h-edges");
	//pix_edge = pixTwoSidedEdgeFilter(pixg,L_VERTICAL_EDGES);
	//Pix* pixev= pixThresholdToBinary(pix_edge,6);
	//pixaAddPix(pixaDisplay, pixev, L_CLONE);

	ostringstream s;
	s << "o3.3";

	Pix* pixo = pixMorphCompSequence(pixeh, s.str().c_str(), 0);

	//pixaAddPixWithTitle(pixaDisplay, pixo, s.str().c_str());

	s.str("");
	s << "o1.90+c20.1";
	//vertical whitespace mask
	Pix *pixvws = pixMorphCompSequence(pixeh, s.str().c_str(), 0);
	//pixaAddPixWithTitle(pixaDisplay, pixvws, s.str().c_str());

	s.str("");
	s << "o1.5+o80.1+c1.20";
	//horizontal whitespace mask
	Pix *pixhws = pixMorphCompSequence(pixeh, s.str().c_str(), 0);
	//pixaAddPixWithTitle(pixaDisplay, pixhws, s.str().c_str());

	//combine whitespace masks
	pixRasterop(pixvws, 0, 0, w, h, PIX_NOT(PIX_SRC | PIX_DST), pixhws, 0, 0);
	pixaAddPixWithTitle(pixaDisplay, pixvws, "combined");

	int image_width = 144;
	int image_height = 176;

	Pixa* comp;
	Boxa* tl = pixConnComp(pixvws, &comp, 8);
	l_int32 comp_count = pixaGetCount(comp);
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
		}
	}

	for (int i = 0; i < comp_count; i++) {
		Pix* singleTextBox = pixaGetPix(comp, i, L_CLONE);
		l_float32 fract;
		pixFindAreaFraction(singleTextBox, NULL, &fract);
		s.str("");
		s << fract;
		printf("fract = %f.2\n", fract);

		//pixaAddPixWithTitle(pixaDisplay,singleTextBox,s.str().c_str());
		pixDestroy(&singleTextBox);
	}

	//pixConformsToRectangle()

	//filter
	//0. find boxes that have a minimum ratio of fg to area
	//1. find boxed that touch the edges. those might be text areas
	//2 find boxes that do not touch the edges and that have a minimum height
	Boxa* filtered = boxaSelectBySize(tl, image_width / 6, image_height / 6, L_SELECT_IF_BOTH, L_SELECT_IF_GTE, NULL);
	renderTransformedBoxa(pixOrg, filtered_boxa, 255);
	pixaAddPixWithTitle(pixaDisplay, pixOrg, "text areas");

//	pixOpenBrick(pixo,pixo,4,4);
//	pixaAddPix(pixaDisplay, pixo, L_INSERT);
/*
	float scorefract = 0.1;
	int thresh;
	int nx = 2;
	int ny = 2;

	PIXTILING* pt = pixTilingCreate(pixg, nx, ny, 0, 0, 0, 0);
	Pix* pixth = pixCreate(nx, ny, 8);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, j, i);
			thresh = analyseTile(pixt, j, i);
			pixSetPixel(pixth, j, i, thresh);
			//pixaAddPix(pixaDisplay, pixt, L_CLONE);
			pixDestroy(&pixt);
		}
	}
	pixTilingDestroy(&pt);
	*/

	pixOtsuAdaptiveThreshold(pixg, 16, 16, 0, 0, 0.1, NULL, &pixb);
	pixSplitDistributionFgBg(pixg, scorefract, 1, &thresh, NULL, NULL, 1);
	pixbDisplay = pixConvert1To8(NULL, pixb, 255, 0);
	//pixaAddPix(pixaDisplay, pixbDisplay, L_CLONE);
	pixWrite("binary.bmp", pixb, IFF_BMP);
	printf("thresh  = %i\n", thresh);
	pixd = pixaDisplayTiledAndScaled(pixaDisplay, 8, 400, 3, 0, 25, 2);
	pixDisplay(pixd, 0, 0);
	//pixd = pixaDisplayTiledInRows(comp, 1, 400, 3, 0, 25, 0);
	//pixDisplay(pixd, 0, 0);

	pixaDestroy(&pixaDisplay);
	pixDestroy(&pixbDisplay);
	pixDestroy(&pixb);
	pixDestroy(&pixth);
	pixDestroy(&pix_edge);
	pixDestroy(&pixeh);
	//pixDestroy(&pixOrg);
	pixDestroy(&pixg);
}

void onePicture(const char* filename, int index) {
	log("%s", filename);

	int debug_level = 2;
	ostringstream s;
	Pix* pixFinal;
	Pix* pixOrg = pixRead(filename);

	s << index;
	printf("%i\n", index);
	L_TIMER timer = startTimerNested();

	Pix* pixtext = bookpage(pixOrg, &pixFinal, messageCallback, pixCallBack, debug_level > 0, debug_level > 1);
	if (debug_level > 1) {
		printf("total time = %f", stopTimerNested(timer));
		pixWrite("binarized_dewarped.bmp", pixtext, IFF_BMP);
	}
	doOCR(pixtext, NULL, &s, debug_level);

	pixDestroy(&pixFinal);
	pixDestroy(&pixtext);
	pixDestroy(&pixOrg);
	s.str("");
}

int main(int argc, const char* argv[]) {
	l_chooseDisplayProg(L_DISPLAY_WITH_XV);

	ostringstream s;
	if (argc == 3) {

		s << "/Users/renard/devel/textfairy/OCRTest/" << argv[1] << "/" << argv[2] << ".jpg";
		layoutDetectDebug(s.str().c_str(), atoi(argv[1]));
		//onePictureWithColumns(s.str().c_str(), atoi(argv[1]));

		//onePicture(s.str().c_str(), atoi(argv[1]));
	} else if (argc == 4) {
		int start = atoi(argv[2]);
		int end = atoi(argv[3]);
		for (int i = start; i < end; i++) {
			s << "/Users/renard/Desktop/devel/textfairy/OCRTest/" << argv[1] << "/" << i << ".jpg";
			onPictureOnlyBinarize(s.str().c_str(), i);
			//onePicture(s.str().c_str(), i);
			s.str("");
		}
	} else {
		for (int i = 32; i < 33; i++) {
			s << "/Users/renard/Desktop/devel/textfairy/OCRTest/pics/" << i << ".jpg";

			onePicture(s.str().c_str(), i);
			s.str("");
		}
	}
	return 0;
}
