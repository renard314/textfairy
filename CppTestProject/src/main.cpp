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

void doOcr(){
	Pix* pixOrg = pixRead("images/1.png");
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
	int rc =api.Init("/Users/renard/devel/textfairy", "eng+deu", tesseract::OEM_DEFAULT);
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



int main() {
	Pix* pixOrg = pixRead("images/47.jpg");
	PTA* orgPoints = ptaCreate(4);
	PTA* mappedPoints = ptaCreate(4);
	l_int32 x1,y1,x2,y2,x3,y3,x4,y4;
	x1=335;
	y1=125;
	x2=2128;
	y2=308;
	x3=2415;
	y3=3234;
	x4=0;
	y4=3234;

	ptaAddPt(orgPoints,x1,y1);
	ptaAddPt(orgPoints,x2,y2);
	ptaAddPt(orgPoints,x3,y3);
	ptaAddPt(orgPoints,x4,y4);

//	ptaAddPt(mappedPoints, 0, 125);
//	ptaAddPt(mappedPoints, 2415,308);
//	ptaAddPt(mappedPoints, 2415,3234);
//	ptaAddPt(mappedPoints, 0,3234);
	Box* orgBox = ptaGetBoundingRegion(orgPoints);

	pixRenderLine(pixOrg,x1,y1,x2,y2,6,L_CLEAR_PIXELS);
	pixRenderLine(pixOrg,x2,y2,x3,y3,6,L_CLEAR_PIXELS);
	pixRenderLine(pixOrg,x3,y3,x4,y4,6,L_CLEAR_PIXELS);
	pixRenderLine(pixOrg,x4,y4,x1,y1,6,L_CLEAR_PIXELS);

	l_int32  x, y, w, h;
    boxGetGeometry(orgBox, &x, &y, &w, &h);
	ptaAddPt(mappedPoints, x, y);
	ptaAddPt(mappedPoints, x + w - 1, y);
	ptaAddPt(mappedPoints, x + w - 1, y + h - 1);
	ptaAddPt(mappedPoints, x, y + h - 1);

	pixRenderBox(pixOrg,orgBox,5,L_SET_PIXELS);
	Pix* pixBilinar = pixBilinearPta(pixOrg,mappedPoints,orgPoints,L_BRING_IN_WHITE);
	pixDisplay(pixOrg,0,0);
	pixDisplay(pixBilinar,0,0);
	pixDestroy(&pixOrg);
	pixDestroy(&pixBilinar);
	return 0;
}
