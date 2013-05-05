/*
 * main.cpp
 *
 *  Created on: Jul 4, 2011
 *      Author: renard
 *      Sister: jeanne
 */

#include "/usr/local/include/tesseract/baseapi.h"
#include "image_processing.h"
#include "binarize.h"
#include "pageseg.h"
#include "dewarp.h"
#include "util.h"
using namespace std;
using namespace tesseract;

void doOCR(Pix* pixb, ETEXT_DESC* monitor, ostringstream* s, bool debug = false) {
	tesseract::TessBaseAPI api;
	api.Init("/Users/renard/Desktop/devel/tesseract-ocr-3.02/","deu",tesseract::OEM_DEFAULT);
	api.SetPageSegMode(tesseract::PSM_SINGLE_BLOCK);
	api.SetImage(pixb);
	//const char* hocrtext = api.GetHOCRText(monitor, 0);
	//*s << hocrtext;
	//const char* utf8text = api.GetHTMLText(20);
	const char* debugText = api.GetUTF8Text();
	//*s << utf8text;
	if (debug) {
//		std::cout<<"html: " <<utf8text<<std::endl;
		std::cout<<"utf8text: " <<debugText<<std::endl;
		std::cout << "ocr: " << stopTimer() << std::endl << "confidence: " << api.MeanTextConf() << std::endl;
	}
	delete[] debugText;
//	delete[] utf8text;
//	delete[] hocrtext;
	api.End();
}

bool onProgressChanged(int progress, int left, int right, int top, int bottom) {

	std::cout << left << ":" << top << ":" << right << ":" << bottom;
	return true;
}

void pixCallBack(Pix* pix, bool b1, bool b2) {
	pixDisplay(pix, 0, 0);
}

void messageCallback(int messageId) {

}

void onePicture(const char* filename, int index) {
	ostringstream s;
	Pix* pixFinal;
	Pix* pixOrg = pixRead(filename);

	s << index;
	printf("%i\n", index);
	L_TIMER timer = startTimerNested();

	Pix* pixtext = bookpage(pixOrg, &pixFinal, messageCallback, pixCallBack, true, true);

	printf("total time = %f", stopTimerNested(timer));
	pixWrite("binarized.bmp", pixtext, IFF_BMP);
	doOCR(pixtext,NULL,&s,true);

	pixDestroy(&pixFinal);
	pixDestroy(&pixtext);
	pixDestroy(&pixOrg);
	s.str("");
}

int main(int argc, const char* argv[]) {
	l_chooseDisplayProg(L_DISPLAY_WITH_XV);
	ostringstream s;
	if (argc == 3) {

		s << "/Users/renard/Desktop/devel/workspace/OCRTest/" << argv[1] << "/" << argv[2] << ".jpg";
		onePicture(s.str().c_str(), atoi(argv[1]));
	} else if (argc == 4) {
		int start = atoi(argv[2]);
		int end = atoi(argv[3]);
		for (int i = start; i < end; i++) {
			s << "/Users/renard/Desktop/devel/workspace/OCRTest/" << argv[1] << "/" << i << ".jpg";
			onePicture(s.str().c_str(), i);
			s.str("");
		}
	} else {
		for (int i = 3; i < 4; i++) {
			s << "/Users/renard/Desktop/devel/workspace/OCRTest/dewarp/" << i << ".jpg";
			onePicture(s.str().c_str(), i);
			s.str("");
		}
	}
	return 0;
}
