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
#include <array.h>
#include <baseapi.h>
#include <bmf.h>
#include <Codecs.hh>
#include <environ.h>
#include <gplot.h>
#include <imageio.h>
#include <Image.hh>
#include <jpeg.hh>
#include <leptprotos.h>
#include <morph.h>
#include <pdf.hh>
#include <pix.h>
#include <publictypes.h>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <sstream>
#include <hocr.hh>
#include "RunningStats.h"
#include "binarize.h"
#include "pageseg.h"
#include "text_search.h"
#include "util.h"
#include <unistd.h>
#include <cfloat>
#include "SkewCorrector.h"
#include "PixBinarizer.h"
#include "TextStatFinder.h"
#include "TextBlockFinder.h"

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
int analyseTile(Pix* pix, int x, int y, l_float32 variance) {
	//printf("%s\n", "analyseTile");

	NUMA* histo = pixGetGrayHistogram(pix, 1);
	NUMA* norm = numaNormalizeHistogram(histo, 1);
	NUMA* closed = numaClose(norm, 7);
//	//NUMA* eroded = numaErode(closed,7);
	numaDestroy(&norm);
	norm = closed;

	/*
	 *     (1) The returned na consists of sets of four numbers representing
	 *         the peak, in the following order:
	 *            left edge; peak center; right edge; normalized peak area
	 */
	//numaFindExtrema(norm,)
//	NUMA* peaks = numaFindPeaks(norm, num_peaks, 0.5, 0.3);
//	NUMA* peaksY = numaCreate(0);
//	NUMA* peaksX = numaCreate(0);
//	NUMA* peak_areas = numaCreate(0);
//	int peakCount = numaGetCount(peaks);
//	int i = 0;
//	while (i < peakCount) {
//		l_int32 left, center, right;
//		l_float32 area;
//		numaGetIValue(peaks, i++, &left);
//		numaGetIValue(peaks, i++, &center);
//		numaGetIValue(peaks, i++, &right);
//		numaGetFValue(peaks, i++, &area);
//
//		numaAddNumber(peak_areas,area);
//
//		l_float32 value;
//		numaGetFValue(norm, left, &value);
//		numaAddNumber(peaksX, left);
//		numaAddNumber(peaksY, value);
//
//		numaGetFValue(norm, center, &value);
//		numaAddNumber(peaksX, center);
//		numaAddNumber(peaksY, value);
//
//		numaGetFValue(norm, right, &value);
//		numaAddNumber(peaksX, right);
//		numaAddNumber(peaksY, value);
//
//	}
	//find the peak with the largest area
	l_int32 thresh;
	l_float32 p1, p2;
	numaSplitDistribution(norm, 0.0, &thresh, NULL, NULL, &p1, &p2, NULL);
	//printf(" p1 = %f,p2 =  %f\n",p1,p2);

	NUMA* tx = numaCreate(0);
	NUMA* ty = numaCreate(0);
	numaAddNumber(ty, 0.04);
	numaAddNumber(tx, thresh);

	GPLOT *gplot;
	ostringstream name;
	name << "tile(" << x << "," << y << ")";
	ostringstream rootName;
	rootName << x << y;
	gplot = gplotCreate(rootName.str().c_str(), GPLOT_X11, name.str().c_str(), "x", "y");
	ostringstream title;
//	title << peakCount / 4 << " peaks";
	gplotAddPlot(gplot, NULL, norm, GPLOT_LINES, "histogram");
//	gplotAddPlot(gplot, peaksX, peaksY, GPLOT_IMPULSES, title.str().c_str());
	gplotAddPlot(gplot, tx, ty, GPLOT_IMPULSES, "thresh");
	//gplotMakeOutput(gplot);
	gplotDestroy(&gplot);
	bool isVarianceLow = variance < 7;
	l_float32 isP2gP1 = p2 / p1;
	printf("p2/p1 =%f, p1=%f, p2=%f", isP2gP1, p1, p2);
	if (isVarianceLow && isP2gP1 < 2) {
		thresh = 0;
	}
//	if (x==9 && y == 2){
//		pixDisplay(pix,0,0);
//	}

//	numaDestroy(&peaks);
//	numaDestroy(&peaksX);
//	numaDestroy(&peaksY);
	numaDestroy(&histo);
	numaDestroy(&norm);
	return thresh;
}
Pix* pixAnnotate(Pix* pixb, const char* textstr) {
	L_BMF *bmf;
	bmf = bmfCreate("/Users/renard/devel/textfairy/leptonica-1.68/prog/fonts", 4);
	Pix* pixt = pixAddSingleTextblock(pixb, bmf, textstr, 0x00000000, L_ADD_ABOVE, NULL);
	bmfDestroy(&bmf);
	return pixt;
}

void pixaAddPixWithTitle(Pixa* pixa, Pix* pix, const char* title) {
	if (title != NULL) {
		Pix* pixt = pixAnnotate(pix, title);
		pixaAddPix(pixa, pixt, L_INSERT);
	} else {
		pixaAddPix(pixa, pix, L_CLONE);
	}
}

void layoutDetect(const char* filename, int index) {
	Pix* pixOrg = pixRead(filename);
	Pix* pixg = pixConvertRGBToLuminance(pixOrg);
	Pix* pixb;
	Boxa* textRegions = pixFindTextRegions(pixg, &pixb);
	renderTransformedBoxa(pixOrg, textRegions, 255);
	Pix* pix32 = pixConvert1To32(NULL, pixb, 0, 0xffffffff);
	pixDisplay(pix32, 0, 0);
	pixDestroy(&pixg);
	pixDestroy(&pix32);
	pixDestroy(&pixb);
	boxaDestroy(&textRegions);
}

Boxa* findEdgesInTile(Pix* pixbTile, Pix* pixt) {
	Box* left, *right, *top, *bottom;
	l_int32 w = pixGetWidth(pixbTile);
	l_int32 h = pixGetWidth(pixbTile);
	l_int32 thickness = 3;

	left = boxCreate(0, thickness, thickness, h - thickness * 2);
	right = boxCreate(w - thickness, thickness, thickness, h - thickness * 2);
	top = boxCreate(thickness, 0, w - thickness * 2, thickness);
	bottom = boxCreate(thickness, h - thickness, w - thickness * 2, thickness);
	Boxa* edges = boxaCreate(0);
	boxaAddBox(edges, left, L_CLONE);
	boxaAddBox(edges, right, L_CLONE);
	boxaAddBox(edges, top, L_CLONE);
	boxaAddBox(edges, bottom, L_CLONE);

	//Pix* pixWithBoxa = pixSetBlackOrWhiteBoxa(pixbTile,edges,L_SET_BLACK);

	Pixa* pixaEdges = pixaCreateFromBoxa(pixbTile, edges, NULL);
	//Numa* edgePixelCount = pixaFindAreaFraction(pixaEdges);
	//l_float32* ec = numaGetFArray(edgePixelCount, L_NOCOPY);
	//printf("top = %f, left = %f, bottom = %f, right = %f\n",ec[0],ec[1],ec[2],ec[3]);
	Pixa* validEdges = pixaSelectByAreaFraction(pixaEdges, 0.3, L_SELECT_IF_GTE, NULL);
	return pixaGetBoxa(validEdges, L_CLONE);
}

void tophat(const char* filename, int index) {
	log("%s", filename);
	Pix* pixOrg = pixRead(filename);
	Pix* pixg = pixConvertRGBToGrayFast(pixOrg);
	Pixa* pixaDisplay = pixaCreate(0);

	Pix* reduced = pixScaleGrayRank2(pixg, 2);
	pixg = reduced;
	Pix* pixTopHat = pixFastTophat(pixg, 4, 4, L_TOPHAT_BLACK);
	pixaAddPixWithTitle(pixaDisplay, pixTopHat, "tophat");

	int thresh;
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
				numaDestroy(&histo);
				numaDestroy(&norm);
				return;
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
			thresh = analyseTile(pixt, i, j, variance);
			pixSetPixel(pixth2, j, i, thresh);
			printf("(%i,%i) mean =%f.2 var = %f.2, thresh = %i\n", j, i, mean, variance, thresh);
			pixDestroy(&pixt);
		}
	}
	int w = pixGetWidth(pixg);
	int h = pixGetHeight(pixg);

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

	pixaAddPixWithTitle(pixaDisplay, pixb, "binary");
	ostringstream s;
	s.str("");
	s << "c1.3+e2.2"; //create vertical whitespace mask and solidify it
	Pix* pixi = pixInvert(NULL, pixb);
	Pix *pixvws = pixMorphCompSequence(pixi, s.str().c_str(), 0);
	pixaAddPixWithTitle(pixaDisplay, pixvws, s.str().c_str());

	Pix* pixd = pixaDisplayTiledAndScaled(pixaDisplay, 8, 288, 4, 0, 25, 2);
	pixDisplay(pixd, 0, 0);

}

void analyseTiles(const char* filename, int index) {
	log("%s", filename);
	Pix* pixOrg = pixRead(filename);
	Pix* pixg = pixConvertRGBToGrayFast(pixOrg);
	Pixa* pixaDisplay = pixaCreate(0);
	pixWrite("grey.bmp", pixg, IFF_BMP);

	pixaAddPixWithTitle(pixaDisplay, pixOrg, "original");
	Pix* pixTopHat = pixFastTophat(pixg, 4, 4, L_TOPHAT_BLACK);
	pixaAddPixWithTitle(pixaDisplay, pixTopHat, "tophat");
//	Pix* pixDome = pixHDome(pixTopHat,60,4);
//	pixaAddPixWithTitle(pixaDisplay, pixDome, "hdome");
//	Pix* pixbinary = pixThresholdToBinary(pixDome,6);
//	pixaAddPixWithTitle(pixaDisplay, pixbinary, "binary");

	int thresh;
	int nx = 0;
	int ny = 0;

	PIXTILING* pt = pixTilingCreate(pixg, 0, 0, 15, 15, 0, 0);
	pixTilingGetCount(pt, &nx, &ny);
	Pix* pixth = pixCreate(nx, ny, 8);
	Pix* pixth2 = pixCreate(nx, ny, 8);
	//printf("nx = %i, ny = %i\n",nx,ny);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, i, j);

			NUMA* histo = pixGetGrayHistogram(pixt, 1);
			NUMA* norm = numaNormalizeHistogram(histo, 1);
			l_float32 sum, moment, var, y, variance, mean, meanY, countPixels;
			l_int32 start = 0, end = 255, n, error, closeSize = 0;

			error = numaGetNonzeroRange(histo, 0, &start, &end);
			if (end == start || error == 1) {
				numaDestroy(&histo);
				numaDestroy(&norm);
				return;
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
			thresh = analyseTile(pixt, i, j, variance);
			pixSetPixel(pixth2, j, i, thresh);
			printf("(%i,%i) mean =%f.2 var = %f.2, thresh = %i\n", j, i, mean, variance, thresh);
			pixDestroy(&pixt);
		}
	}

	//pixth2 = pixBlockconv(pixth2,3,3);
	int w = pixGetWidth(pixg);
	int h = pixGetHeight(pixg);
	Pix* pixb = pixCreate(w, h, 1);
	l_int32 tw, th;
	pixTilingGetSize(pt, &tw, &th);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			l_uint32 pixel;
			l_uint32 t;
			pixGetPixel(pixth, j, i, &pixel);
			pixGetPixel(pixth2, j, i, &t);
			Pix* pixt = pixTilingGetTile(pt, i, j);
			Pix* pixbTile = pixThresholdToBinary(pixt, t);

//			if (i==9 && j == 2){
//				pixWrite("debug.bmp",pixt,IFF_BMP);
//				pixDisplay(pixbTile,0,0);
//			}

			Boxa* boxaEdges = findEdgesInTile(pixbTile, pixt);

			Pix* pixWithBoxa = pixSetBlackOrWhiteBoxa(pixbTile, boxaEdges, L_SET_BLACK);

			//pixDisplay(pixWithBoxa,0,0);

			pixTilingPaintTile(pixb, i, j, pixbTile, pt);
			l_int32 w = pixGetWidth(pixt);
			l_int32 h = pixGetHeight(pixt);
			Box* box = boxCreate(j * tw, i * th, w, h);
			l_int32 px, py, pw, ph;
			boxGetGeometry(box, &px, &py, &pw, &ph);
			l_int32 error;
			if (pixel == 127) {
				error = pixRenderBoxBlend(pixOrg, box, 1, 0, 200, 0, 0.5);
			} else {
				error = pixRenderBoxBlend(pixOrg, box, 1, 200, 0, 0, 0.5);
			}
			//printf("box(%i,%i) px=%i, py=%i, error=%i\n",i,j,px,py,error);
			pixDestroy(&pixt);
			pixDestroy(&pixbTile);

			boxDestroy(&box);
		}
	}

	ostringstream s;
	//pixaAddPixWithTitle(pixaDisplay, pixo, s.str().c_str());
	pixaAddPixWithTitle(pixaDisplay, pixb, "binary");
	pixWrite("binary.bmp", pixb, IFF_BMP);

	s.str("");
	s << "o1.20+d3.3"; //create vertical whitespace mask and solidify it
	Pix* pixi = pixInvert(NULL, pixb);
	Pix *pixvws = pixMorphCompSequence(pixi, s.str().c_str(), 0);
	pixaAddPixWithTitle(pixaDisplay, pixvws, s.str().c_str());

	//open up vertical whitespace again
	s.str("");
	//pixInvert(pixb,pixb);
	s << "";

	//Pix *pixb2 = pixMorphCompSequence(pixb, s.str().c_str(), 0);
	const char *seltext = "o"
			"xXxx"
			"xxxxx";

//	SEL* selsplit = selCreateFromString(seltext, 5, 3, "selsplit");
//	Pix* pixSplit = pixHMT(NULL,pixb,selsplit);
//	pixaAddPixWithTitle(pixaDisplay,pixSplit,"split");

	//split up columns that are connected by thin lines
	const char *selSplitVertically = "o"
			"o"
			"o"
			"X"
			"o"
			"o"
			"o";
	SEL* selSplitColumns = selCreateFromString(selSplitVertically, 7, 1, "selsplit");
	Pix* pixSplitColumns = pixHMT(NULL, pixb, selSplitColumns);
	pixXor(pixSplitColumns, pixSplitColumns, pixb);
	pixaAddPixWithTitle(pixaDisplay, pixSplitColumns, "split columns vertically");

	//connect text lines
	Pix *pixb2 = pixCloseBrickDwa(NULL, pixSplitColumns, 3, 5);
	pixaAddPixWithTitle(pixaDisplay, pixb2, "connect");

	//split columns horizontally
	pixCloseBrickDwa(pixb2, pixb2, 1, 3);
	pixaAddPixWithTitle(pixaDisplay, pixb2, "split columns horizontally");
//	const char *selSplitHorizontally =
//			"oooXooo";
//	SEL* selSplitColumnsH= selCreateFromString(selSplitHorizontally, 1, 7, "selsplit");
//	Pix* pixSplitColumns = pixHMT(NULL,pixb,selSplitColumns);

	pixWrite("split.bmp", pixSplitColumns, IFF_BMP);

	//pixRasterop(pixvws, 0, 0, w, h, PIX_NOT(PIX_SRC) & PIX_DST, pixb2, 0, 0);
	//pixaAddPixWithTitle(pixaDisplay,pixvws,"PIX_NOT(PIX_SRC) & PIX_DST");
	Pix* pixd = pixaDisplayTiledAndScaled(pixaDisplay, 8, 288, 4, 0, 25, 2);

	pixDisplay(pixd, 0, 0);
	pixDestroy(&pixth);
	pixDestroy(&pixth2);
	pixTilingDestroy(&pt);
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
	pixTilingGetCount(pt, &nx, &ny);
	pixaAddPixWithTitle(pixaDisplay, pix_edge, "edge");

	Pix* pixth = pixCreate(nx, ny, 8);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, j, i);
			int w = pixGetWidth(pixt);
			int h = pixGetHeight(pixt);
			printf("w=%i, h=%i", w, h);
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

	//Pix* pixo = pixMorphCompSequence(pixeh, s.str().c_str(), 0);

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
	//Boxa* filtered = boxaSelectBySize(tl, image_width / 6, image_height / 6, L_SELECT_IF_BOTH, L_SELECT_IF_GTE, NULL);
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
		pixDisplay(pixtext, 0, 0);
		pixWrite("binarized_dewarped.bmp", pixtext, IFF_BMP);
	}
	doOCR(pixtext, NULL, &s, debug_level);

	pixDestroy(&pixFinal);
	pixDestroy(&pixtext);
	pixDestroy(&pixOrg);
	s.str("");
}



void plotNuma(Numa* numa, Numa* numaExtrema, Numa* numaDelta) {

	l_int32 n = numaGetCount(numaExtrema);
	printf("%i number of extrema", n);
	Numa* numaYValues = numaCreate(0);
	for (int i = 0; i < n; i++) {
		l_int32 index;
		l_float32 number;
		numaGetIValue(numaExtrema, i, &index);
		numaGetFValue(numa, index, &number);
		numaAddNumber(numaYValues, number);
	}
	GPLOT *gplot;
	ostringstream name;
	ostringstream rootName;
	gplot = gplotCreate("numaPLot", GPLOT_X11, name.str().c_str(), "x", "y");
	ostringstream title;
	gplotAddPlot(gplot, NULL, numa, GPLOT_LINES, "histogram");
	gplotAddPlot(gplot, numaExtrema, numaYValues, GPLOT_IMPULSES, "extrema");
	gplotMakeOutput(gplot);
	gplotDestroy(&gplot);

	gplot = gplotCreate("numaPLot2", GPLOT_X11, name.str().c_str(), "x", "y");
	gplotAddPlot(gplot, NULL, numaDelta, GPLOT_IMPULSES, "delta");
	gplotMakeOutput(gplot);
	gplotDestroy(&gplot);

	//sleep(1);
	//return pixRead("numaPLot.png");
}

void testSkew(const char* filename, int index) {
	Pix* pixOrg = pixRead(filename);
	Pix* pixb;
	L_TIMER timer = startTimerNested();
	if (pixGetDepth(pixOrg) > 1) {
		Pix* pixg = pixConvertRGBToLuminance(pixOrg);
		//Pix* pixEdge = pixTwoSidedEdgeFilter(pixg,L_HORIZONTAL_EDGES);
		Pix* pixEdge = pixSobelEdgeFilter(pixg, L_ALL_EDGES);
		l_int32 width = pixGetWidth(pixEdge);
		l_int32 height = pixGetHeight(pixEdge);
		pixOtsuAdaptiveThreshold(pixEdge, width, height, 0, 0, 0, NULL, &pixb);
		pixDestroy(&pixg);
		pixDestroy(&pixEdge);
		pixWrite("binary.bmp", pixb, IFF_BMP);
	} else {
		pixb = pixClone(pixOrg);
	}

	l_float32 angle;
	pixFindSkewSweep(pixb, &angle, 1, 15., 1.);
	printf("angle = %f, total time = %f\n", angle, stopTimerNested(timer));

	l_float32 deg2rad = 3.1415926535 / 180.;
	Pix* pixd = pixRotate(pixb, deg2rad * angle, L_ROTATE_AREA_MAP, L_BRING_IN_BLACK, 0, 0);

	pixDisplay(pixb, 0, 0);
	pixDisplay(pixd, 0, 0);

	pixDestroy(&pixb);
	pixDestroy(&pixd);
	pixDestroy(&pixOrg);
}

void numaPrint(Numa* nad) {
	l_int32 count = numaGetCount(nad);
	printf("\n");
	for (int i = 0; i < count; i++) {
		l_float32 val;
		numaGetFValue(nad, i, &val);
		printf("%i: %f\n", i, val);
	}
}

Pix* pixGreyToBinary(Pix* pixg) {
	Pix* pixb;
	Pix* pixEdge = pixSobelEdgeFilter(pixg, L_ALL_EDGES);
	/* Do combination of contrast norm and sauvola */
	Pix* pixt1 = pixContrastNorm(NULL, pixg, 100, 100, 55, 1, 1);
	pixSauvolaBinarizeTiled(pixt1, 9, 0.15, 1, 1, NULL, &pixb);
	//pixOtsuAdaptiveThreshold(pixEdge, width, height, 0, 0, 0, NULL, &pixb);
	pixDestroy(&pixEdge);
	return pixb;
}

void pixSetInBoxa(Pix* pix, Boxa* boxa) {
	if (boxa != NULL) {
		l_int32 n = boxaGetCount(boxa);
		Box* box;
		for (int i = 0; i < n; i++) {
			box = boxaGetBox(boxa, i, L_CLONE);
			pixSetInRectArbitrary(pix, box, 0);
			boxDestroy(&box);
		}
	}
}

void testbinary(const char* filename, int index) {
	Pix* pixOrg = pixRead(filename);
	Pix* pixg = pixConvertRGBToLuminance(pixOrg);
	//Pix* pixEdge = pixTwoSidedEdgeFilter(pixg,L_HORIZONTAL_EDGES);
	Pix* pixmorph;
	Pixa* pixaDisplay = pixaCreate(0);
	Pix* pixb = pixGreyToBinary(pixg);

	l_float32 angle;
	l_int32 error = pixFindSkewSweep(pixb, &angle, 1, 47., 1.);
	if (error == 1) {
		angle = 0;
	} else {
		//rotate binary image
		l_float32 deg2rad = 3.1415926535 / 180.;
		Pix* pixd = pixRotate(pixb, deg2rad * angle, L_ROTATE_AREA_MAP, L_BRING_IN_WHITE, 0, 0);
		if (pixd != NULL) {
			pixDestroy(&pixb);
			pixb = pixd;
		}
	}

	l_float32 textLineSpacing = pixGetTextLineSpacing(pixb);
	printf("linespacing = %f\n", textLineSpacing);

	Pixa* pixaFiltered = pixGetTextBlocks(textLineSpacing, pixb, &pixmorph);

	Boxa* boxa = pixaGetBoxa(pixaFiltered, L_CLONE);
	//do another run after removing the found text blocks from pixb
	//pixSetInBoxa(pixb, boxa);

	pixRenderBoxa(pixOrg, boxa, 2, L_CLEAR_PIXELS);
	pixaAddPixWithTitle(pixaDisplay, pixOrg, "original");
	pixaAddPixWithTitle(pixaDisplay, pixb, "binary");
	pixaAddPixWithTitle(pixaDisplay, pixmorph, "pixmorph");

	Pix* pixd = pixaDisplayTiledAndScaled(pixaDisplay, 32, 400, 3, 0, 25, 2);
	pixDisplay(pixd, 0, 0);

	pixWrite("pixmorph.bmp", pixmorph, IFF_BMP);
	pixaDestroy(&pixaFiltered);
	pixaDestroy(&pixaDisplay);
	boxaDestroy(&boxa);
	pixDestroy(&pixd);
	pixDestroy(&pixb);
}
void printNuma(Numa* na, const char* tag) {
	printf("%s\n", tag);
	l_int32 pc = numaGetCount(na);
	for (int i = 0; i < pc; i++) {
		l_int32 val;
		numaGetIValue(na, i, &val);
		printf("%i = %i\n", i, val);
	}
	printf("\n");
}





//void numaGetMinValue(Numa* nax,Numa* nay, l_float32* px, l_float32* py){
//	l_int32 n = numaGetCount(nax);
//	l_float32 val;
//	l_float32 min = FLT_MAX;
//	for(int i = 0; i< n; i++){
//		numaGetFValue(nay,i,&val);
//
//	}
//}




NUMA * numaMakeDelta3(NUMA *nas) {
	l_int32 i, n, prev, cur;
	NUMA *nad;
	n = numaGetCount(nas);
	nad = numaCreate(n - 1);
	numaGetIValue(nas, 0, &prev);
	for (i = 1; i < n; i++) {
		numaGetIValue(nas, i, &cur);
		numaAddNumber(nad, cur - prev);
		prev = cur;
	}
	return nad;
}







void debugTextFindingTile(const char* filename, int index) {
	TextStatFinder textFinder(true);
	Pix* pixOrg = pixRead(filename);
	l_float32 textPropability = 0;
	l_float32 textSize = 0;
	bool success = textFinder.getTextStats(pixOrg,&textSize,&textPropability);
	printf("text finding success = %s, text probability = %f\n", success==true?"true":"false",textPropability);
	pixDestroy(&pixOrg);

}

void debugTextFinding(const char* filename, int index) {
	Pix* pixOrg = pixRead(filename);

	PixBinarizer binarizer(false);
	SkewCorrector skewCorrector(false);
	TextStatFinder textStatFinder(false);
	TextBlockFinder textBlockFinder(true);
	Numa* naTextSizes = NULL;
	l_float32 angle;
	Pixa* pixaTextBlocks;
	Pix* pixTextProbability;
	Pix* pixRotatedOrg;
	Pix* pixBinarySkewed = binarizer.binarize(pixOrg);
	pixDisplay(pixBinarySkewed,0,0);
	Pix* pixBinary= skewCorrector.correctSkew(pixBinarySkewed, &angle);
	L_TIMER timer = startTimerNested();
	textStatFinder.getTextStatsTiled(pixBinary,&naTextSizes, &pixTextProbability);
	printf("time for text stats = %f\n", stopTimerNested(timer));


	l_float32 textSize=0;
	numaGetFValue(naTextSizes,0,&textSize);
	timer = startTimerNested();
	pixaTextBlocks = textBlockFinder.findTextBlocks(pixBinary,pixTextProbability,naTextSizes,NULL);
	l_int32 pixaCount = pixaGetCount(pixaTextBlocks);
	printf("time for %i text blocks = %f\n", pixaCount,stopTimerNested(timer));

	Boxa* boxa = pixaGetBoxa(pixaTextBlocks, L_CLONE);
	pixRotatedOrg = skewCorrector.rotate(pixOrg,angle);

	pixRenderBoxa(pixRotatedOrg, boxa, 2, L_CLEAR_PIXELS);
	pixDisplay(pixRotatedOrg,0,0);


	pixaDestroy(&pixaTextBlocks);
	pixDestroy(&pixBinarySkewed);
	pixDestroy(&pixBinary);
	pixDestroy(&pixTextProbability);
	pixDestroy(&pixRotatedOrg);
	pixDestroy(&pixOrg);
	boxaDestroy(&boxa);
	numaDestroy(&naTextSizes);
}

void testDFT(const char* filename, int index) {
	TextStatFinder textFinder(true);
	Pix* pix = pixRead(filename);
	Pix* pixBorder = pixAddBorder(pix, 1, 0);
	Numa* numaPixelSum = pixSumPixelsByRow(pixBorder, NULL);


	l_int32 n = numaGetCount(numaPixelSum);
	double *a = new double [n/2 + 1];
	double *b = new double [n/2 + 1];
	double *sig_out = new double [n];
//TODO test
//	    forwardDFT(sig_in, w, a, b);  // forward DFT
}

void forwardDFT(const double *s, const int &N, double *&a, double *&b)
{
    // note: this code is not optimised at all, written for clarity not speed.
    for (int k = 0; k <= N / 2; ++k) {
        a[k] = b[k] = 0;
        for (int x = 0; x < N; ++x) {
            a[k] += s[x] * cos(2 * M_PI / N * k * x);
            b[k] += s[x] * sin(2 * M_PI / N * k * x);
        }
        // normalization
        a[k] *= (k == 0 || k == N / 2) ? 1. / N : 2. / N;
        b[k] *= 2. / N;
    }
}

int main(int argc, const char* argv[]) {
	l_chooseDisplayProg(L_DISPLAY_WITH_XV);

	ostringstream s;
	if (argc == 3) {

		s << "/Users/renard/devel/textfairy/OCRTest/" << argv[1] << "/" << argv[2] << ".bmp";
		debugTextFindingTile(s.str().c_str(), atoi(argv[1]));
		//debugTextFinding(s.str().c_str(), atoi(argv[1]));
		//testbinary(s.str().c_str(), atoi(argv[1]));
		//testSkew(s.str().c_str(), atoi(argv[1]));
		//testDFT(s.str().c_str(),atoi(argv[1]));

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
