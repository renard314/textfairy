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

#include "/usr/local/include/tesseract/baseapi.h"
#include "image_processing.h"
#include "binarize.h"
#include "pageseg.h"
#include "dewarp.h"
#include "util.h"
using namespace std;
using namespace tesseract;


void doOCR(Pix* pixb, ETEXT_DESC* monitor, ostringstream* s,
		int debug_level = 0) {
	tesseract::TessBaseAPI api;
	api.Init("/Users/renard/Desktop/devel/textfairy/tesseract-ocr-read-only/",
			"eng+deu", tesseract::OEM_DEFAULT);
	api.SetPageSegMode(tesseract::PSM_AUTO);
	api.SetImage(pixb);
	//const char* hocrtext = api.GetHOCRText(monitor, 0);
	//*s << hocrtext;
	//const char* debugText = api.GetHTMLText(20);
	//const char* debugText = api.GetUTF8Text();
	api.Recognize(monitor);
	ResultIterator* it = api.GetIterator();
	std::string debugText = GetHTMLText(it, 70);
	*s << debugText;
	if (debug_level>1) {
//		std::cout<<"html: " <<utf8text<<std::endl;
		std::cout << "utf8text: " <<"\n"<< debugText << std::endl;
		std::cout << "ocr: " << stopTimer() << std::endl << "confidence: "
				<< api.MeanTextConf() << std::endl;
	} else if (debug_level>0){
		std::cout << "confidence: "<< api.MeanTextConf() << std::endl;
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
	pixDisplay(pix, 0, 0);
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

	segmentComplexLayout(pixOrg, pixhm, pixb, &pixaImages, &pixaTexts,
			messageCallback, true);

	ostringstream hocr;
	ostringstream utf8text;
	l_int32 textCount = pixaGetCount(pixaTexts);
	l_int32* textIndexes = new l_int32[textCount];
	for (int i = 0; i < textCount; i++) {
		textIndexes[i] = i;
	}
	combineSelectedPixa(pixaTexts, pixaImages, textIndexes, textCount, NULL,
			NULL, messageCallback, &pixFinal, &pixOcr, &boxaColumns, true);
	pixWrite("dewarpedColumns.bmp", pixOcr, IFF_BMP);
	printf("total time = %f", stopTimerNested(timer));

	delete[] textIndexes;
	boxaDestroy(&boxaColumns);
	pixDestroy(&pixFinal);
	pixDestroy(&pixOcr);
	pixDestroy(&pixOrg);
	pixDestroy(&pixb);
	pixDestroy(&pixhm);
	pixDestroy(&pixTextlines);
	pixaDestroy(&pixaImages);
	pixaDestroy(&pixaTexts);
}

void onPictureOnlyBinarize(const char* filename, int index){
	Pix *pixhm, *pixb = NULL;
	Pix* pixOrg = pixRead(filename);
	Pix* pixsg;
	extractImages(pixOrg, &pixhm, &pixsg);
	binarize(pixsg, pixhm, &pixb);
	pixDisplay(pixb, 0, 0);
	pixWrite("binarized.bmp",pixb,IFF_BMP);

	pixDestroy(&pixb);
	pixDestroy(&pixsg);
	pixDestroy(&pixOrg);

}

void onePicture(const char* filename, int index) {
	log("%s",filename);

	int debug_level =1;
	ostringstream s;
	Pix* pixFinal;
	Pix* pixOrg = pixRead(filename);

	s << index;
	printf("%i\n", index);
	L_TIMER timer = startTimerNested();

	Pix* pixtext = bookpage(pixOrg, &pixFinal, messageCallback, pixCallBack,
			debug_level>0, debug_level>1);
	if (debug_level>1){
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
	log("%s","hi");

	ostringstream s;
	if (argc == 3) {

		s << "/Users/renard/Desktop/devel/textfairy/OCRTest/" << argv[1] << "/"
				<< argv[2] << ".jpg";
		onPictureOnlyBinarize(s.str().c_str(), atoi(argv[1]));

//		onePicture(s.str().c_str(), atoi(argv[1]));
	} else if (argc == 4) {
		int start = atoi(argv[2]);
		int end = atoi(argv[3]);
		for (int i = start; i < end; i++) {
			s << "/Users/renard/Desktop/devel/textfairy/OCRTest/" << argv[1]
					<< "/" << i << ".jpg";
			onPictureOnlyBinarize(s.str().c_str(), i);
			//onePicture(s.str().c_str(), i);
			s.str("");
		}
	} else {
		for (int i = 32; i < 33; i++) {
			s << "/Users/renard/Desktop/devel/textfairy/OCRTest/pics/" << i
					<< ".jpg";

			onePicture(s.str().c_str(), i);
			s.str("");
		}
	}
	return 0;
}
