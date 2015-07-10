//============================================================================
// Name        : HelloWorld2.cpp
// Author      : 
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include <iostream>
#include <allheaders.h>
#include <image_processing_util.h>
#include "baseapi.h"
#include <ocrclass.h>
#include "binarize.h"
#include "dewarp_textfairy.h"
#include "RunningStats.h"
#include <stdlib.h>
#include "PixBlurDetect.h"

using namespace std;

static Pixa* pixaDebugDisplay = pixaCreate(0);

void messageJavaCallback(int message) {
	printf("pixJavmessageJavaCallback(%i)\n", message);
}

void pixJavaCallback(Pix* pix) {
	pixaAddPix(pixaDebugDisplay, pix, L_CLONE);
}

/**
 * callback for tesseracts monitor
 */
bool progressJavaCallback(void* progress_this, int progress, int left, int right, int top, int bottom) {
	printf("progress = %i, (%i,%i) - (%i,%i)\n", progress, left, bottom, right, top);
	return true;
}

/**
 * callback for tesseracts monitor
 */
bool cancelFunc(void* cancel_this, int words) {
	return false;
}

void testScaleWithBitmap() {
	Pix* pixOrg = pixRead("images/bmpfile.bmp");
	Pix* pixd = pixRemoveColormap(pixOrg, REMOVE_CMAP_BASED_ON_SRC);
	if (pixd == NULL) {
		fprintf(stderr, "converting to grey");
		pixd = pixConvertTo8(pixOrg, false);
	}
	l_int32 d = pixGetDepth(pixd);
	fprintf(stderr, "d = %i.", d);

	pixd = pixScaleGeneral(pixd, 0.5, 0.5, 0, 0);
	pixDisplay(pixd, 0, 0);

}

void doOcr() {
	Pix* pixOrg = pixRead("images/bmpfile.bmp");
	Pix* pixText;
	pixJavaCallback(pixOrg);

	bookpage(pixOrg, &pixText, messageJavaCallback, pixJavaCallback, true);

	pixWrite("dewarpedTest.png", pixText, IFF_PNG);

	ETEXT_DESC monitor;
	monitor.progress_callback = progressJavaCallback;
	monitor.cancel = cancelFunc;
	monitor.cancel_this = NULL;
	monitor.progress_this = NULL;

	tesseract::TessBaseAPI api;
	int rc = api.Init("/Users/renard/devel/textfairy", "bul+eng", tesseract::OEM_DEFAULT);
	if (rc) {
		fprintf(stderr, "Could not initialize tesseract.\n");
		exit(1);
	}
	startTimer();

	api.SetPageSegMode(tesseract::PSM_AUTO);
	api.SetInputName("pdfimage");
	api.SetImage(pixText);
	api.Recognize(&monitor);
	char* text = api.GetUTF8Text();
	log("ocr time: %f", stopTimer());

	printf("%s", text);
	free(text);
	api.End();
	pixWriteImpliedFormat("testpng.png", pixText, 85, 0);
	Pix* pixd = pixaDisplayTiledAndScaled(pixaDebugDisplay, 8, 640, 3, 0, 25, 2);
	pixDisplay(pixd, 0, 0);
	pixDestroy(&pixd);

	pixaClear(pixaDebugDisplay);
	pixDestroy(&pixOrg);
	pixDestroy(&pixText);
}

void perspective() {
	Pix* pixOrg = pixRead("images/renard.png");

	PTA* orgPoints = ptaCreate(4);
	PTA* mappedPoints = ptaCreate(4);
	l_int32 x1, y1, x2, y2, x3, y3, x4, y4;
	x1 = 541;
	y1 = 172;
	x2 = 2235;
	y2 = 0;
	x3 = 2218;
	y3 = 1249;
	x4 = 605;
	y4 = 1002;

	ptaAddPt(orgPoints, x1, y1);
	ptaAddPt(orgPoints, x2, y2);
	ptaAddPt(orgPoints, x3, y3);
	ptaAddPt(orgPoints, x4, y4);

	pixRenderLine(pixOrg, x1, y1, x2, y2, 6, L_CLEAR_PIXELS);
	pixRenderLine(pixOrg, x2, y2, x3, y3, 6, L_CLEAR_PIXELS);
	pixRenderLine(pixOrg, x3, y3, x4, y4, 6, L_CLEAR_PIXELS);
	pixRenderLine(pixOrg, x4, y4, x1, y1, 6, L_CLEAR_PIXELS);

	l_int32 x, y, w, h;
	w = pixGetWidth(pixOrg);
	h = pixGetHeight(pixOrg);
	x = 0;
	y = 0;
	Box* orgBox = boxCreate(x, y, w, h);
	ptaAddPt(mappedPoints, x, y);
	ptaAddPt(mappedPoints, x + w - 1, y);
	ptaAddPt(mappedPoints, x + w - 1, y + h - 1);
	ptaAddPt(mappedPoints, x, y + h - 1);

	pixRenderBox(pixOrg, orgBox, 5, L_SET_PIXELS);
	//Pix* pixBilinar = pixBilinearPta(pixOrg,mappedPoints,orgPoints,L_BRING_IN_WHITE);
	Pix* pixBilinar = pixProjectivePtaColor(pixOrg, mappedPoints, orgPoints, 0xffffff00);
	pixDisplay(pixOrg, 0, 0);
	pixDisplay(pixBilinar, 0, 0);
	pixDestroy(&pixOrg);
	pixDestroy(&pixBilinar);
}

bool numaGetStdDeviationOnInterval(Numa* na, l_int32 start, l_int32 end, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean) {
	l_int32 n = numaGetCount(na);
	if (n < 2) {
		return false;
	}
	if (end > n) {
		return false;
	}

	l_int32 val;
	RunningStats stats;
	for (int i = start; i < end; i++) {
		numaGetIValue(na, i, &val);
		stats.Push(val);
	}
	if (stdDev != NULL) {
		*stdDev = stats.PopulationStandardDeviation();
	}
	if (errorPercentage != NULL) {
		if (stats.Mean() > 0) {
			*errorPercentage = stats.PopulationStandardDeviation() / fabs(stats.Mean());
		} else {
			*errorPercentage = 0;
		}
	}
	if (mean != NULL) {
		*mean = stats.Mean();
	}
	return true;
}

bool numaGetStdDeviation(Numa* na, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean) {
	l_int32 n = numaGetCount(na);
	return numaGetStdDeviationOnInterval(na, 0, n, stdDev, errorPercentage, mean);
}

void pixBlurDebug(Pix* pix) {
	Pix* pixsg = pixConvertRGBToLuminance(pix);
	Pix* pixEdge = pixTwoSidedEdgeFilter(pixsg, L_VERTICAL_EDGES);
	NUMA* histo = pixGetGrayHistogram(pixEdge, 1);
	pixWrite("lines.png", pixEdge, IFF_PNG);
	Numa* histonorm = numaNormalizeHistogram(histo, 1);
	l_float32 stdDev, mean, error;
	numaGetStdDeviation(histonorm, &stdDev, &error, &mean);
	log("stdDev =%f ,mean=%f, blur = %f", stdDev, mean, mean * stdDev);
	pixDestroy(&pixEdge);
	pixDestroy(&pixsg);
	numaDestroy(&histonorm);

}

void dewarp() {
	Pix* pixOrg = pixRead("images/renard.png");
	Pix* pixsg = pixConvertRGBToLuminance(pixOrg);
	Pix*pixb;

	binarize(pixsg, NULL, &pixb);
	Ptaa* ptaa1 = dewarpGetTextlineCenters(pixb, 0);
	Pix* pixt1 = pixCreateTemplate(pixOrg);
	Pix* pixt2 = pixDisplayPtaa(pixt1, ptaa1);
	//pixDisplayWithTitle(pixt2, 0, 500, "textline centers", 1);

	/* Remove short lines */
	Ptaa* ptaa2 = dewarpRemoveShortLines(pixb, ptaa1, 0.8, 0);

	/* Fit to quadratic */
	int n = ptaaGetCount(ptaa2);
	int i = 0;
	NUMA *nax, *nafit;
	l_float32 a, b, c, d, e;

	for (i = 0; i < n; i++) {
		Pta* pta = ptaaGetPta(ptaa2, i, L_CLONE);
		ptaGetArrays(pta, &nax, NULL);
		//ptaGetQuadraticLSF(pta, &a, &b, &c, &nafit);
		//ptaGetCubicLSF(pta, &a,&b,&c,&d,&nafit);
		ptaGetQuarticLSF(pta, &a, &b, &c, &d, &e, &nafit);
		Pta* ptad = ptaCreateFromNuma(nax, nafit);
		pixDisplayPta(pixt2, pixt2, ptad);
		ptaDestroy(&pta);
		ptaDestroy(&ptad);
		numaDestroy(&nax);
		numaDestroy(&nafit);
	}
	//pixDisplayWithTitle(pixt2, 300, 500, "fitted lines superimposed",1);
	Pix* pixd;
	pixDewarp(pixb, &pixd);
	pixDisplayWithTitle(pixd, 300, 500, "cubic dewarp", 1);

}

#include <iostream>
#include <fstream>
#include <sstream>

#include "Codecs.hh"
#include "pdf.hh"
#include "hocr.hh"
#include "jpeg.hh"

void createPdf(const char* imagePath, const char* hocrPath) {
	printf("%s", "creating pdf");

	ofstream pdfOutStream("test.pdf");
	PDFCodec* pdfContext = new PDFCodec(&pdfOutStream);
	bool sloppy = false;
	bool overlayImage = false;
	Image image;
	image.w = image.h = 0;
	std::string fileName(imagePath);
	if (!ImageCodec::Read(fileName, image)) {
		std::cout << "Error reading input file.";
	}

	if (image.resolutionX() <= 0 || image.resolutionY() <= 0) {
		std::cout << "Warning: Image x/y resolution not set, defaulting to: 300 ";
		image.setResolution(300, 300);
	}

	unsigned int res = image.resolutionX();

	std::ifstream file(hocrPath);
	std::stringstream hocr;

	if (file) {
		hocr << file.rdbuf();
		file.close();
		pdfContext->beginPage(72. * image.w / res, 72. * image.h / res);
		pdfContext->setFillColor(0, 0, 0);
		hocr2pdf(hocr, pdfContext, res, sloppy, !overlayImage);

		if (overlayImage) {
			pdfContext->showImage(image, 0, 0, 72. * image.w / res, 72. * image.h / res);
		}
	}

	delete pdfContext;
}

l_int32 pixGetAverageValueInRectIgnoreBlack(PIX *pixs, BOX *box, l_float32 *pavgval) {
	l_int32 i, j, w, h, d, wpl, bw, bh;
	l_int32 xstart, ystart, xend, yend;
	l_uint32 sum, sumCount;
	l_uint32 *data, *line;

	PROCNAME("pixGetAverageValueInRect");

	if (!pavgval)
		return ERROR_INT("nothing to do", procName, 1);
	if (pavgval)
		*pavgval = 0;
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
	sumCount = 0;
	for (i = ystart; i <= yend; i++) {
		line = data + i * wpl;
		for (j = xstart; j <= xend; j++) {
			if (d == 8) {
				l_uint8 data_byte = GET_DATA_BYTE(line, j);
				if (data_byte > 0) {
					sum += data_byte;
					sumCount++;
				}
			} else { /* d == 32 */
				l_uint32 color = line[j];
				if (color > 0) {
					sum += color;
					sumCount++;
				}
			}

		}
	}

	if (pavgval) {
		if (sumCount > 0) {
			*pavgval = sum / sumCount;
		} else {
			*pavgval = 0;
		}
	}
	return 0;
}

void dewarpTest(const char* image) {
	Pix* pixOrg = pixRead(image);
	Pix* pixb, *pixDewarped;
	Pix* pixsg = pixConvertRGBToLuminance(pixOrg);
	binarize(pixsg, NULL, &pixb);
	l_int32 result = pixDewarp(pixb, &pixDewarped);
	if(!result){
		pixWrite("dewarped.png", pixDewarped, IFF_PNG);
		pixDisplay(pixDewarped, 0, 0);
		pixDestroy(&pixDewarped);
	}
	pixDestroy(&pixb);
	pixDestroy(&pixOrg);
	pixDestroy(&pixsg);
}

void dewarpTestAll(const char* images) {
	SARRAY* sa = getSortedPathnamesInDirectory(images, NULL, 0, 0);
	l_int32 count = sarrayGetCount(sa);
	Pix* pixOrg;
	char *str;
	for (int i = 0; i < count; i++) {
		str = sarrayGetString(sa, i, L_NOCOPY);
		if ((pixOrg = pixRead(str)) != NULL) {
			startTimer();
			Pix* pixb, *pixDewarped;
			Pix* pixsg = pixConvertRGBToLuminance(pixOrg);
			binarize(pixsg, NULL, &pixb);
			l_int32 result = pixDewarp(pixb, &pixDewarped);
			if(!result){
				printf("%s dewarped in %f, \n", str, stopTimer());
				pixDisplay(pixDewarped, 0, 0);
				pixDestroy(&pixDewarped);
			}
			pixDestroy(&pixb);
			pixDestroy(&pixOrg);
			pixDestroy(&pixsg);
		}
	}
}

void blurDetect(const char* image) {
	Pix* pixOrg = pixRead(image);
	PixBlurDetect blurDetector(true);

	startTimer();
	l_float32 blurValue;
	Box* maxBlurLoc = NULL;
	Pix* pixBlended = blurDetector.makeBlurIndicator(pixOrg, &blurValue, &maxBlurLoc);
	pixRenderBox(pixBlended, maxBlurLoc, 2, L_SET_PIXELS);
	l_int32 x, y, w, h;
	boxGetGeometry(maxBlurLoc, &x, &y, &w, &h);
	printf("blur=%f, \n", blurValue);
	pixWrite("image.jpg", pixBlended, IFF_PNG);

	boxDestroy(&maxBlurLoc);
	pixDestroy(&pixBlended);
	pixDestroy(&pixOrg);
}

void testAllBlur() {

	blurDetect("images/sharp2.png");
	blurDetect("images/sharp3.png");
	blurDetect("images/sharp4.png");
	blurDetect("images/sharp5.png");
	blurDetect("images/sharp6.png");
	blurDetect("images/sharp7.png");
	blurDetect("images/sharp8.png");
	blurDetect("images/sharp9.png");
	printf("BLURRED\n");
	blurDetect("images/blur1.jpg");
	blurDetect("images/blur2.jpg");
	blurDetect("images/blur3.jpg");
	blurDetect("images/blur4.jpg");
	blurDetect("images/blur5.jpg");
	blurDetect("images/blur6.jpg");
	blurDetect("images/blur7.jpg");
}
void blurDetectTest(const char* images) {
	PixBlurDetect blurDetector(false);
	SARRAY* sa = getSortedPathnamesInDirectory(images, NULL, 0, 0);
	l_int32 count = sarrayGetCount(sa);
	Pix* pixOrg;
	char *str;
	for (int i = 0; i < count; i++) {
		str = sarrayGetString(sa, i, L_NOCOPY);
		if ((pixOrg = pixRead(str)) != NULL) {
			startTimer();
			l_float32 blurValue;
			//printf("Loading %s",str);
			Box* maxBlurLoc = NULL;
			Pix* pixBlended = blurDetector.makeBlurIndicator(pixOrg, &blurValue, &maxBlurLoc);
			printf("%s - blur = %f\n", str, blurValue);
			pixDestroy(&pixBlended);
			pixDestroy(&pixOrg);
			boxDestroy(&maxBlurLoc);
		}
	}
}

int main() {
	//createPdf("images/5.png","images/scan_test.html");
	//testScaleWithBitmap();
	//blurDetect("images/sharp9.jpg");
	//blurDetect("images/48.jpg");
	//testAllBlur();
	//blurDetect("images/blurred/endless.jpg");
	//blurDetectTest("images/blurred");
	//blurDetectTest("images/sharp");
	dewarpTest("images/dewarp/2.jpg");
	//dewarpTestAll("images/dewarp");

	return 0;
}
