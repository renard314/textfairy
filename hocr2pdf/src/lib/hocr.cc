/*
 * The ExactImage library's hOCR to PDF parser
 * Copyright (C) 2008-2009 Ren�� Rebe, ExactCODE GmbH Germany
 * Copyright (C) 2008 Archivista
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2. A copy of the GNU General
 * Public License can be found in the file LICENSE.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANT-
 * ABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * Alternatively, commercial licensing options are available from the
 * copyright holder ExactCODE GmbH Germany.
 */

#include <string.h>

#include <iostream>
#include <fstream>
#include <iomanip>
#include <cmath>
#include <cctype>
#include <sstream>
#include <vector>

#include "../codecs/Codecs.hh"
#include "../codecs/pdf.hh"

#include "hocr.hh"
#include "entities.h"

static int res = 300;
static bool sloppy = false;
static bool straightenTextLines = false;
static PDFCodec* pdfContext = 0;
static std::ostream* txtStream = 0;
static std::string txtString;

std::string lowercaseStr(const std::string& _s) {
	std::string s(_s);
	std::transform(s.begin(), s.end(), s.begin(), tolower);
	return s;
}

// custom copy to trip newlines, likewise
bool isMyBlank(char c) {
	switch (c) {
	case ' ':
	case '\t':
	case '\r':
	case '\n':
		return true;
		break;
	default:
		return false;
	}
}

std::string peelWhitespaceStr(const std::string& _s) {
	std::string s(_s);
	// trailing whitespace
	for (int i = s.size() - 1; i >= 0 && isMyBlank(s[i]); --i)
		s.erase(i, 1);
	// leading whitespace
	while (!s.empty() && isMyBlank(s[0]))
		s.erase(0, 1);
	return s;
}

// lower-case, and strip leading/trailing white-space
std::string sanitizeStr(const std::string& _s) {
	return peelWhitespaceStr(lowercaseStr(_s));
}

// HTML decode

std::string htmlDecode(const std::string& _s) {
	char* dest = new char[_s.length()+1];
	decode_html_entities_utf8(dest,_s.c_str());
	std::string s(dest);
	delete[] dest;
	return s;
}

// state per char: bbox, bold, italic, boldItalic
// state per line: bbox, align: left, right justified

struct BBox {
	BBox() :
		x1(0), y1(0), x2(0), y2(0) {
	}

	bool operator==(const BBox& other) {
		return x1 == other.x1 && y1 == other.y1 && x2 == other.x2 && y2
				== other.y2;
	}

	double x1, y1, x2, y2;
} lastBBox;

std::ostream& operator<<(std::ostream& s, const BBox& b) {
	s << b.x1 << ", " << b.y1 << ", " << b.x2 << ", " << b.y2;
	return s;
}

enum Style {
	None = 0, Bold = 1, Italic = 2, BoldItalic = (Bold | Italic)
} lastStyle;

std::ostream& operator<<(std::ostream& s, const Style& st) {
	switch (st) {
	case Bold:
		s << "Bold";
		break;
	case Italic:
		s << "Italic";
		break;
	case BoldItalic:
		s << "BoldItalic";
		break;
	default:
		s << "None";
		break;
	}
	return s;
}

// TODO: implement parsing, if of any guidance for the PDF
enum Align {
	Left = 0, Right = 1, Justify = 2,
} lastAlign;

struct Span {
	BBox bbox;
	Style style;
	std::string text;
};

struct Textline {
	BBox bbox;
	int size;
	int descenders;
	float baseline1;
	float baseline2;

	bool hasDescenders(std::string word){
		//const char* descenders = "qpgjy";
		if (word.find('q')!=std::string::npos ||
			word.find('p')!=std::string::npos ||
			word.find('g')!=std::string::npos ||
			word.find('j')!=std::string::npos ||
			word.find('y')!=std::string::npos){
			return true;
		}
		return false;
	}

	std::vector<Span> spans;
	typedef std::vector<Span>::iterator span_iterator;

	void draw() {
		int n = 0;

		// remove trailing whitespace
		for (span_iterator it = spans.end(); it != spans.begin(); --it) {
			span_iterator it2 = it;
			--it2;
			for (int i = it2->text.size() - 1; i >= 0; --i) {
				if (isMyBlank(it2->text[i]))
					it2->text.erase(i);
				else
					goto whitespace_cleaned;
			}
		}

		whitespace_cleaned:

		/*
		double x1 = 72. * bbox.x1 / res;
		double x2 = 72. * bbox.x2 / res;
		double y1 = 72. * bbox.y1 / res;
		double y2 = 72. * bbox.y2 / res;
		pdfContext->moveTo(x1, y2);
		pdfContext->addLineTo(x2, y2);
		pdfContext->addLineTo(x2, y1);
		pdfContext->addLineTo(x1, y1);
		pdfContext->addLineTo(x1, y2);
		pdfContext->showPath();
		*/

		//the baseline params should be interpreted like this:
		//float b = (bbox.x1 * baseline1 + baseline2);
		//but we try to keep a perfect straight line to make some pdf readers happy
		float b = (baseline2);
		std::cout <<"baseline mod = "<<b<<"\n";


		int height = 0;
		if(size>0){
			height = size *	72.0 / res;
		} else {
			double y1 = 72. * bbox.y1 / res;
			double y2 = 72. * ((b/2) + bbox.y2) / res;
			height = y2-y1;
		}

		for (span_iterator it = spans.begin(); it!= spans.end(); ++it, ++n) {
			std::string text = htmlDecode(it->text);
			BBox spanbbox = it->bbox;

			const char* font = "Helvetica";
			switch (it->style) {
			case Bold:
				font = "Helvetica-Bold";
				break;
			case Italic:
				font = "Helvetica-Oblique";
				break;
			case BoldItalic:
				font = "Helvetica-BoldOblique";
				break;
			default:
				; // already initialized
			}
		double y;
			if (::straightenTextLines==true){
				y =  (72. * (b+bbox.y2) / res); //use y value from bounding box of the line
			} else {
				double t = spanbbox.y2;
				if (hasDescenders(text)){
					t-=descenders;
				}
				y = 72. * t / res;
			}

			std::cout <<text <<" "<< " x="<<(72. * spanbbox.x1  / res) <<"  y="<< y << " height="<<height<<"\n";
			pdfContext->textTo(72. * spanbbox.x1 / res, y);
			pdfContext->showText(font, text, height);
			if (txtStream) {
				txtString += text;
			}
		}
		if (txtStream) {
			txtString += "\n";
		}

	}


	void flush() {
		if (!spans.empty())
			draw();
		spans.clear();
	}

	void push_back(Span s) {
		//std::cerr << "push_back (" << s.text << ") " << s.style << std::endl;

		// do not insert newline garbage (empty string after white-
		// space peeling) at the beginning of a line
		if (spans.empty()) {
			s.text = peelWhitespaceStr(s.text);
			if (s.text.empty())
				return;
		}

		// if the direction wrapps, assume new line
//		if (!spans.empty() && s.bbox.x1 < spans.back().bbox.x1)
//			flush();

		// unify inserted spans with same properties, for now to
		// not draw them at the same position, but one text operator
		if (!spans.empty() && (spans.back().bbox == s.bbox)
				&& (spans.back().style == s.style))
			spans.back().text += s.text;
		else
			spans.push_back(s);
	}

} textline;

bool isNewLine(std::string attr) {
	const char* ocr_line = "ocr_line";
	std::string::size_type i = attr.find(ocr_line);
	if (i == std::string::npos) {
		return false;
	} else {
		return true;
	}
}

void parseBaseline(std::string attr, Textline& line) {
	const char* ocr_line = "baseline";
	std::string::size_type i = attr.find(ocr_line);
	if (i == std::string::npos) {
		return;
	}
	std::stringstream stream(attr.substr(i + strlen(ocr_line)));

	stream >> line.baseline1>>line.baseline2;
}

int parseDescenders(std::string s) {
	const char* tS = "descenders='";
	std::string::size_type i = s.find(tS);
	if (i == std::string::npos)
		return 0;

	std::string::size_type i2 = s.find("'", i + strlen(tS));
	if (i2 == std::string::npos)
		return 0;
	std::string sizeStr = s.substr(i + strlen(tS), i2 - i - strlen(tS));
	std::istringstream converter(sizeStr);
	int result;
	converter >> result;
	return result;
}
int parseSize(std::string s) {
	std::cout <<"parseSize"<< " "<<s<<"\n";;

	const char* tS = "size='";
	std::string::size_type i = s.find(tS);
	if (i == std::string::npos)
		return 0;

	std::string::size_type i2 = s.find("'", i + strlen(tS));
	if (i2 == std::string::npos)
		return 0;
	std::string sizeStr = s.substr(i + strlen(tS), i2 - i - strlen(tS));
	std::istringstream converter(sizeStr);
	int result;
	converter >> result;
	std::cout <<"size = "<< result <<"\n";;

	return result;
}

BBox parseBBox(std::string s) {
	BBox b; // self initialized to zero
	//std::cout <<"parseBBox"<< " "<<s<<"\n";;
	const char* tS = "bbox ";

	std::string::size_type i = s.find(tS);
	if (i == std::string::npos) {
		return b;
	}
//	std::cout <<"found box\n";

	std::stringstream stream(s.substr(i + strlen(tS)));
	stream >> b.x1 >> b.y1 >> b.x2 >> b.y2;

	//std::cout <<"x1="<<b.x1<<"\n";

	return b;
}

void elementStart(const std::string& _name, const std::string& _attr = "") {
	std::string name(sanitizeStr(_name)), attr(sanitizeStr(_attr));

	//std::cerr << "elementStart: '" << name << "', attr: '" << attr << "'" << std::endl;

	BBox bbox = parseBBox(attr);
	if (bbox.x2 > 0 && bbox.y2 > 0) {
		lastBBox = bbox;
	}

	bool newLine = isNewLine(attr);
	if (newLine==true) {
		textline.flush();
		textline.size = parseSize(attr);
		textline.descenders = parseDescenders(attr);
		textline.bbox = bbox;
		parseBaseline(attr, textline);
	}

	if (name == "b" || name == "strong")
		lastStyle = Style(lastStyle | Bold);
	else if (name == "i" || name == "em")
		lastStyle = Style(lastStyle | Italic);

}

void elementText(const std::string& text) {
	//std::cerr << "elementText: \"" << text << "\"" << std::endl;
	Span s;
	s.bbox = lastBBox;
	s.style = lastStyle;
	s.text += text;

	textline.push_back(s);
}

void elementEnd(const std::string& _name) {
	std::string name(sanitizeStr(_name));

	//std::cerr << "elementEnd: " << name << std::endl;

	if (name == "b" || name == "strong")
		lastStyle = Style(lastStyle & ~Bold);
	else if (name == "i" || name == "em")
		lastStyle = Style(lastStyle & ~Italic);

	// explicitly flush line of text on manual break or end of paragraph
//	else if (name == "br" || name == "p")
//		textline.flush();
}

// returns the string before the first whitespace
std::string tagName(std::string t) {
	std::string::size_type i = t.find(' ');
	if (i != std::string::npos)
		t.erase(i);
	return t;
}

bool hocr2pdf(std::istream& hocrStream, PDFCodec* pdfContext, unsigned int res,
		bool sloppy, bool straightenTextLines,std::ostream* txtStream ) {
	// TODO: soft hyphens
	// TODO: better text placement, using one TJ with spacings
	// TODO: more image compressions, jbig2, Fax

	// TODO: do not use global vars, use a state container
	::pdfContext = pdfContext;
	::res = res;
	::sloppy = sloppy;
	::txtStream = txtStream;
	::straightenTextLines = straightenTextLines;

	pdfContext->beginText();

	// minimal, cuneiform HTML ouptut parser
	char c;
	std::vector<std::string> openTags;
	std::string* curTag = 0;
	std::string closingTag;
	while (hocrStream.get(c), hocrStream.good()) {
		// consume tag element text
		if (curTag && c != '>') {
			*curTag += c;
			continue;
		}

		switch (c) {
		case '<':
			if (hocrStream.peek() != '/') {
				openTags.push_back("");
				curTag = &openTags.back();
			} else {
				closingTag.clear();
				curTag = &closingTag;
			}
			break;
		case '>':
			if (curTag != &closingTag) {
				bool closed = false;
				if (!curTag->empty() && curTag->at(curTag->size() - 1) == '/') {
					curTag->erase(curTag->size() - 1);
					closed = true;
				}

				// HTML asymetric tags, TODO: more of those (and !DOCTYPE)?
				// TODO: maybe specially treat meta & co?
				{
					std::string lowTag = lowercaseStr(tagName(*curTag));
					if (lowTag == "br" || lowTag == "img" || lowTag == "meta")
						closed = true;
				}

				//std::cout << "tag start: " << openTags.back()
				//          << (closed ? " immediately closed" : "") << std::endl;
				{
					std::string element = tagName(*curTag);
					std::string attr = *curTag;
					attr.erase(0, element.size());
					elementStart(element, attr);
				}

				if (closed) {
					elementEnd(*curTag);
					openTags.pop_back();
				}
			} else {
				// garuanteed to begin with a /, remove it
				curTag->erase(0, 1);
				// get just the tag name from the stack
				std::string lastOpenTag = (openTags.empty() ? ""
						: openTags.back());
				lastOpenTag = tagName(lastOpenTag);
				if (lastOpenTag != *curTag) {
					std::cout << "Warning: tag mismatch: '" << *curTag
							<< "' can not close last open: '" << lastOpenTag
							<< "'" << std::endl;
				} else
					openTags.pop_back();
				elementEnd(*curTag);
			}
			curTag = 0;
			break;

		default:
			elementText(std::string(1, c));
			break;
		}
	}

	while (!openTags.empty()) {
		std::string tag = tagName(openTags.back());
		openTags.pop_back();
		// skip special tags such as !DOCTYPE
		if (tag.empty() || tag[0] != '!')
			std::cerr << "Warning: unclosed tag: '" << tag << "'" << std::endl;
	}

	textline.flush();

	pdfContext->endText();

	if (txtStream) {
		// for now hypenation compensator, later to be inserted to the
		// generic code-flow to detect and write out soft-hypens on-the-go

		// regex: ([a-z])-\n([a-z]) -> \1\2
		// + insert \n at next space of next line
		for (std::string::iterator it = txtString.begin(); it
				!= txtString.end();) {
			if ((*it == '\n') && // lock on newlines
					(it != txtString.begin() && it[-1] == '-') && // hyphen in front
					(it != txtString.end() - 1 && islower(it[+1])) // and the next is lower case
			) {
				it = txtString.erase(it - 1, it + 1); // erase "\n-"

				// so, newline removed, insert a break at the next word, same line
				for (; it != txtString.end() && *it != '\n'; ++it) {
					if (*it == ' ') {
						*it = '\n';
						++it;
						break;
					}
				}
			} else
				++it;
		}

		*txtStream << txtString;
		txtString.clear();
	}

	return true; // or error
}
