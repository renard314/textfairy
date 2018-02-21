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

#include "ocr_util.h"

using namespace std;


string GetHTMLText(tesseract::ResultIterator* res_it, const float minConfidenceToShowColor) {
	int lcnt = 1, pcnt = 1, wcnt = 1;
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
