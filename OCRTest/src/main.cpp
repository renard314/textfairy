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

char* GetHTMLText(tesseract::ResultIterator* res_it, const float minConfidenceToShowColor) {
	  int lcnt = 1, bcnt = 1, pcnt = 1, wcnt = 1;

	  STRING html_str("");
	  bool isItalic = false;
	  bool isBold = false;


	  for (; !res_it->Empty(tesseract::RIL_BLOCK); wcnt++) {
	    if (res_it->Empty(tesseract::RIL_WORD)) {
	      res_it->Next(tesseract::RIL_WORD);
	      continue;
	    }

	    // Open any new block/paragraph/textline.
	    if (res_it->IsAtBeginningOf(tesseract::RIL_BLOCK)) {
	    	html_str +="<div>";
	    }
	    if (res_it->IsAtBeginningOf(tesseract::RIL_PARA)){
	    	html_str += "<p>";
	    }

	    // Now, process the word...
	    const char *font_name;
	    bool bold, italic, underlined, monospace, serif, smallcaps;
	    int pointsize, font_id;
	    font_name = res_it->WordFontAttributes(&bold, &italic, &underlined,
	                                           &monospace, &serif, &smallcaps,
	                                           &pointsize, &font_id);
	    bool last_word_in_line = res_it->IsAtFinalElement(tesseract::RIL_TEXTLINE, tesseract::RIL_WORD);
	    bool last_word_in_para = res_it->IsAtFinalElement(tesseract::RIL_PARA, tesseract::RIL_WORD);
	    bool last_word_in_block = res_it->IsAtFinalElement(tesseract::RIL_BLOCK, tesseract::RIL_WORD);

	    float confidence = res_it->Confidence(tesseract::RIL_WORD);
		bool addConfidence = false;
		if (  confidence<minConfidenceToShowColor && res_it->GetUTF8Text(tesseract::RIL_WORD)!=" "){
			addConfidence = true;
			html_str.add_str_int("<font conf='", (int)confidence);
			html_str += "' color='#DE2222'>";
		}

		/*
		if (!isBold && bold) {
			html_str += "<em>";
			isBold = true;
		}
		*/

	    if (!isItalic && italic) {
	    	html_str += "<strong>";
	    	isItalic =  true;
	    }
	    do {
	      const char *grapheme = res_it->GetUTF8Text(tesseract::RIL_SYMBOL);
	      if (grapheme && grapheme[0] != 0) {
	        if (grapheme[1] == 0) {
	          switch (grapheme[0]) {
	            case '<': html_str += "&lt;"; break;
	            case '>': html_str += "&gt;"; break;
	            case '&': html_str += "&amp;"; break;
	            case '"': html_str += "&quot;"; break;
	            case '\'': html_str += "&#39;"; break;
	            default: html_str += grapheme; break;
	          }
	        } else {
	        	html_str += grapheme;
	        }
	      }
	      delete []grapheme;
	      res_it->Next(tesseract::RIL_SYMBOL);
	    } while (!res_it->Empty(tesseract::RIL_BLOCK) && !res_it->IsAtBeginningOf(tesseract::RIL_WORD));

	    if ((isItalic &&addConfidence==true) || (!italic && isItalic) || (isItalic && (last_word_in_block || last_word_in_para))){
	    	html_str += "</strong>";
	    	isItalic = false;
	    }
	    /*
	    if ((!bold && isBold) || (isBold && (last_word_in_block || last_word_in_para))){
	    	html_str += "</em>";
	    	isBold = false;
	    }
	    */
		if (addConfidence==true){
			html_str += "</font>";
		}

	    html_str += " ";

	    if (last_word_in_para) {
	    	html_str += "</p>";
	    	pcnt++;
	    }
	    if (last_word_in_block) {
	    	html_str += "</div>";
	    	bcnt++;
	    }
	  }
	  char *ret = new char[html_str.length() + 1];
	  strcpy(ret, html_str.string());
	  delete res_it;
	  return ret;
}

void doOCR(Pix* pixb, ETEXT_DESC* monitor, ostringstream* s,
		bool debug = false) {
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
	const char* debugText = GetHTMLText(it, 70);
	*s << debugText;
	if (debug) {
//		std::cout<<"html: " <<utf8text<<std::endl;
		std::cout << "utf8text: " <<"\n"<< debugText << std::endl;
		std::cout << "ocr: " << stopTimer() << std::endl << "confidence: "
				<< api.MeanTextConf() << std::endl;
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

void onePicture(const char* filename, int index) {
	ostringstream s;
	Pix* pixFinal;
	Pix* pixOrg = pixRead(filename);

	s << index;
	printf("%i\n", index);
	L_TIMER timer = startTimerNested();

	Pix* pixtext = bookpage(pixOrg, &pixFinal, messageCallback, pixCallBack,
			true, true);

	printf("total time = %f", stopTimerNested(timer));
	pixWrite("binarized_dewarped.bmp", pixtext, IFF_BMP);
	doOCR(pixtext, NULL, &s, true);

	pixDestroy(&pixFinal);
	pixDestroy(&pixtext);
	pixDestroy(&pixOrg);
	s.str("");
}

int main(int argc, const char* argv[]) {
	l_chooseDisplayProg(L_DISPLAY_WITH_XV);
	ostringstream s;
	if (argc == 3) {

		s << "/Users/renard/Desktop/devel/textfairy/OCRTest/" << argv[1] << "/"
				<< argv[2] << ".jpg";
		onePicture(s.str().c_str(), atoi(argv[1]));
	} else if (argc == 4) {
		int start = atoi(argv[2]);
		int end = atoi(argv[3]);
		for (int i = start; i < end; i++) {
			s << "/Users/renard/Desktop/devel/workspace/OCRTest/" << argv[1]
					<< "/" << i << ".jpg";
			onePicture(s.str().c_str(), i);
			s.str("");
		}
	} else {
		for (int i = 3; i < 4; i++) {
			s << "/Users/renard/Desktop/devel/workspace/OCRTest/dewarp/" << i
					<< ".jpg";
			onePicture(s.str().c_str(), i);
			s.str("");
		}
	}
	return 0;
}
