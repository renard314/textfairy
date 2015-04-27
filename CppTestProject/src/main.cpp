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
#include <algorithm>    // std::max
#include <math.h>       /* pow */
using namespace std;

static Pixa* pixaDebugDisplay = pixaCreate(0);


void messageJavaCallback(int message) {
	printf("pixJavmessageJavaCallback(%i)\n",message);
}

void pixJavaCallback(Pix* pix) {
	pixaAddPix(pixaDebugDisplay, pix, L_CLONE);
}

/**
 * callback for tesseracts monitor
 */
bool progressJavaCallback(void* progress_this,int progress, int left, int right, int top, int bottom) {
	printf("progress = %i, (%i,%i) - (%i,%i)\n",progress, left, bottom,right,top);
	return true;
}

/**
 * callback for tesseracts monitor
 */
bool cancelFunc(void* cancel_this, int words) {
	return false;
}

void testScaleWithBitmap(){
	Pix* pixOrg = pixRead("images/bmpfile.bmp");
    Pix* pixd = pixRemoveColormap(pixOrg, REMOVE_CMAP_BASED_ON_SRC);
    if(pixd==NULL){
    	fprintf(stderr, "converting to grey");
    	pixd=pixConvertTo8(pixOrg, false);
    }
	l_int32 d = pixGetDepth(pixd);
	fprintf(stderr, "d = %i.",d);

	pixd = pixScaleGeneral(pixd, 0.5, 0.5,0, 0);
	pixDisplay(pixd,0,0);

}

void doOcr(){
	Pix* pixOrg = pixRead("images/bmpfile.bmp");
	Pix* pixText;
	pixJavaCallback(pixOrg);

	bookpage(pixOrg, &pixText , messageJavaCallback, pixJavaCallback, true);

	pixWrite("dewarpedTest.png",pixText,IFF_PNG);

	ETEXT_DESC monitor;
	monitor.progress_callback = progressJavaCallback;
	monitor.cancel = cancelFunc;
	monitor.cancel_this = NULL;
	monitor.progress_this = NULL;

	tesseract::TessBaseAPI api;
	int rc =api.Init("/Users/renard/devel/textfairy", "bul+eng", tesseract::OEM_DEFAULT);
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
	log("ocr time: %f",stopTimer());

	printf("%s",text);
	free(text);
	api.End();
	pixWriteImpliedFormat("testpng.png",pixText,85,0);
	Pix* pixd = pixaDisplayTiledAndScaled(pixaDebugDisplay, 8, 640, 3, 0, 25, 2);
	pixDisplay(pixd, 0, 0);
	pixDestroy(&pixd);

	pixaClear(pixaDebugDisplay);
	pixDestroy(&pixOrg);
	pixDestroy(&pixText);
}

void perspective(){
	Pix* pixOrg = pixRead("images/renard.png");

	PTA* orgPoints = ptaCreate(4);
	PTA* mappedPoints = ptaCreate(4);
	l_int32 x1,y1,x2,y2,x3,y3,x4,y4;
	x1 = 541;
	y1 = 172;
	x2 = 2235;
	y2 = 0;
	x3 = 2218;
	y3 = 1249;
	x4 = 605;
	y4 = 1002;

	ptaAddPt(orgPoints,x1,y1);
	ptaAddPt(orgPoints,x2,y2);
	ptaAddPt(orgPoints,x3,y3);
	ptaAddPt(orgPoints,x4,y4);



	pixRenderLine(pixOrg,x1,y1,x2,y2,6,L_CLEAR_PIXELS);
	pixRenderLine(pixOrg,x2,y2,x3,y3,6,L_CLEAR_PIXELS);
	pixRenderLine(pixOrg,x3,y3,x4,y4,6,L_CLEAR_PIXELS);
	pixRenderLine(pixOrg,x4,y4,x1,y1,6,L_CLEAR_PIXELS);

	l_int32  x, y, w, h;
	w = pixGetWidth(pixOrg);
	h = pixGetHeight(pixOrg);
	x =0;
	y= 0;
	Box* orgBox = boxCreate(x,y,w,h);
	ptaAddPt(mappedPoints, x, y);
	ptaAddPt(mappedPoints, x + w - 1, y);
	ptaAddPt(mappedPoints, x + w - 1, y + h - 1);
	ptaAddPt(mappedPoints, x, y + h - 1);

	pixRenderBox(pixOrg,orgBox,5,L_SET_PIXELS);
	//Pix* pixBilinar = pixBilinearPta(pixOrg,mappedPoints,orgPoints,L_BRING_IN_WHITE);
	Pix* pixBilinar =pixProjectivePtaColor(pixOrg,mappedPoints,orgPoints,0xffffff00);
	pixDisplay(pixOrg,0,0);
	pixDisplay(pixBilinar,0,0);
	pixDestroy(&pixOrg);
	pixDestroy(&pixBilinar);
}

bool numaGetStdDeviationOnInterval(Numa* na, l_int32 start, l_int32 end, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean){
	l_int32 n = numaGetCount(na);
	if (n < 2) {
		return false;
	}
	if(end>n){
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
	if(errorPercentage!=NULL){
		if(stats.Mean()>0){
			*errorPercentage = stats.PopulationStandardDeviation() / fabs(stats.Mean());
		} else {
			*errorPercentage = 0;
		}
	}
	if(mean!=NULL){
		*mean = stats.Mean();
	}
	return true;
}


bool numaGetStdDeviation(Numa* na, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean) {
	l_int32 n = numaGetCount(na);
	return numaGetStdDeviationOnInterval(na,0,n,stdDev,errorPercentage,mean);
}

void pixBlurDebug(Pix* pix){
	Pix* pixsg = pixConvertRGBToLuminance(pix);
	Pix* pixEdge = pixTwoSidedEdgeFilter(pixsg, L_VERTICAL_EDGES);
	NUMA* histo = pixGetGrayHistogram(pixEdge, 1);
	pixWrite("lines.png",pixEdge,IFF_PNG);
	Numa*  histonorm = numaNormalizeHistogram(histo,1);
	l_float32 stdDev, mean, error;
	numaGetStdDeviation(histonorm,&stdDev,&error, &mean);
	log("stdDev =%f ,mean=%f, blur = %f", stdDev,mean, mean*stdDev );
	pixDestroy(&pixEdge);
	pixDestroy(&pixsg);
	numaDestroy(&histonorm);

}

void getValueBetweenTwoFixedColors(float value, int r, int g, int b, int &red, int &green, int &blue) {
  int bR = 255; int bG = 0; int bB=0;    // RGB for our 2nd color (red in this case).

  red   = (float)(bR - r) * value + r;      // Evaluated as -255*value + 255.
  green = (float)(bG - g) * value + g;      // Evaluates as 0.
  blue  = (float)(bB - b) * value + b;      // Evaluates as 255*value + 0.
}


void blurDetect(const char* image){
	Pix* pixOrg = pixRead(image);
	Pix* pixGrey = pixConvertRGBToGrayFast(pixOrg);
	Pix* pixMedian = pixMedianFilter(pixGrey,3,3);
	Pix* edges = pixSobelEdgeFilter(pixMedian,L_ALL_EDGES);
	//Pix* edges = pixTwoSidedEdgeFilter(pixMedian, L_VERTICAL_EDGES);
	l_int32    w, h, wpld, wplb, wplm, wplo,wpls;
	l_int32    i, j, x, rval, gval, bval;
	l_uint32  *datad, *datab, *datas,*datao,*datam, *lined, *lineb, *lines, *lineo, *linem;
	w = pixGetWidth(edges);
	h = pixGetHeight(edges);
	Pix* blurMeasure = pixCreate(w,h,8);
	//Pix* pixBinary;
	//pixOtsuAdaptiveThreshold(edges,300,300,0,0,0.1,NULL, &pixBinary);
	NUMA* na = pixGetGrayHistogram(edges, 4);
	int thresh;
	numaSplitDistribution(na, 0.05, &thresh, NULL, NULL, NULL, NULL, NULL);
	numaDestroy(&na);
	Pix* pixBinary = pixThresholdToBinary(edges, thresh);
	//expand the pixels so that blurriness around edges is measured too.
	//pixAddBo
	//pixErodeBrick(pixBinary,pixBinary,6,6);
	//pixInvert(pixBinary, pixBinary);


    datao = pixGetData(pixOrg);
    datas = pixGetData(pixGrey);
    datad = pixGetData(blurMeasure);
    datab = pixGetData(pixBinary);
    datam = pixGetData(pixMedian);
    wplo = pixGetWpl(pixOrg);
    wpls = pixGetWpl(pixGrey);
    wpld = pixGetWpl(blurMeasure);
    wplb = pixGetWpl(pixBinary);
    wplm = pixGetWpl(pixMedian);
    RunningStats stats;
    l_uint32 k= 2;
    for (i = k; i < h-k; i++) {
        lineo = datao + i * wplo;
        lined = datad + i * wpld;
        lineb = datab + i * wplb;
        linem = datam + i * wplm;
        lines = datas + i * wpls;
        for (j = k; j < w-k; j++) {
            if (!GET_DATA_BIT(lineb, j)) {
            	l_uint32 dom = 0;
            	l_uint32 contrast = 0;
            	for(x = j-k;x <=j+k; x++){
            		dom+= abs((GET_DATA_BYTE(linem,x+2) - GET_DATA_BYTE(linem,x))-(GET_DATA_BYTE(linem,x) - GET_DATA_BYTE(linem,x-2)));
                	contrast+= abs(GET_DATA_BYTE(lines,x) - GET_DATA_BYTE(lines,x-1));
            	}
            	if(contrast>0){
					double sharpness = (((float)dom)/((float)contrast));
					stats.Push(sharpness);
					float val = min(255.0, sharpness*50);
					SET_DATA_BYTE(lined,j, val);
					extractRGBValues(lineo[j], &rval, &gval, &bval);
					double fraction = val/255;
					//fraction = pow(fraction,2.4);
					//float)Math.pow(input, mDoubleFactor);
					int red, green, blue;
					getValueBetweenTwoFixedColors(fraction,rval,gval,bval,red, green, blue);
					composeRGBPixel(red, green, blue, lineo + j);
            	}

            } else {
            	//SET_DATA_BYTE(lined,j, 0xff);
            }
        }
    }

    pixWrite("blurMeasure.png",blurMeasure, IFF_PNG);
    pixWrite("binary.png",pixBinary, IFF_PNG);
    pixWrite("edges.png",edges, IFF_PNG);
    pixWrite("median.png",pixMedian, IFF_PNG);
    //pixWrite("pixOrg.png",pixOrg, IFF_PNG);
    //pixBlendColorByChannel(pixOrg,pixOrg,blurMeasure,0,0,0.5,2,2,1,0xff);
    printf("%s = %f, stddev = %f\n",image, stats.Mean() ,stats.PopulationStandardDeviation());
//	pixSetMasked(pixGrey,pixEdgeMask,0);
	//pixDisplay(pixBinary,0,0);
	//pixDisplay(pixOrg,0,0);
	//pixDisplay(pixOrg,0,0);
	pixDestroy(&pixOrg);
	pixDestroy(&pixGrey);
	pixDestroy(&pixMedian);
	pixDestroy(&pixBinary);
	pixDestroy(&blurMeasure);
	pixDestroy(&edges);

}

void dewarp(){
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
	int i=0;
	NUMA         *nax, *nafit;
	l_float32     a, b, c, d, e;

	for (i = 0; i < n; i++) {
		Pta* pta = ptaaGetPta(ptaa2, i, L_CLONE);
		ptaGetArrays(pta, &nax, NULL);
		//ptaGetQuadraticLSF(pta, &a, &b, &c, &nafit);
		//ptaGetCubicLSF(pta, &a,&b,&c,&d,&nafit);
		ptaGetQuarticLSF(pta,&a,&b,&c,&d,&e,&nafit);
		Pta* ptad = ptaCreateFromNuma(nax, nafit);
		pixDisplayPta(pixt2, pixt2, ptad);
		ptaDestroy(&pta);
		ptaDestroy(&ptad);
		numaDestroy(&nax);
		numaDestroy(&nafit);
	}
	//pixDisplayWithTitle(pixt2, 300, 500, "fitted lines superimposed",1);
	Pix* pixd;
	pixDewarp(pixb,&pixd);
	pixDisplayWithTitle(pixd, 300, 500, "cubic dewarp",1);

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

	std::ifstream file( hocrPath );
	std::stringstream hocr;

	if(file) {
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

int main() {
	//createPdf("images/5.png","images/scan_test.html");
	//testScaleWithBitmap();
	blurDetect("images/62.jpg");
	//blurDetect("images/blurred.jpg");
	return 0;
}
