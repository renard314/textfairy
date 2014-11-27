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




int main() {
	Pix* pixOrg = pixRead("images/53.jpg");
	Pix* pixText;
	pixJavaCallback(pixOrg);

	bookpage(pixOrg, &pixText , messageJavaCallback, pixJavaCallback, false);

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
	Pix* pixd = pixaDisplayTiledAndScaled(pixaDebugDisplay, 8, 640, 3, 0, 25, 2);
	pixDisplay(pixd, 0, 0);
	pixDestroy(&pixd);

	pixaClear(pixaDebugDisplay);
	pixDestroy(&pixOrg);
	pixDestroy(&pixText);
	return 0;
}
