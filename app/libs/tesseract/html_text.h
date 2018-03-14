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
 * util.h
 *
 *  Created on: Mar 4, 2012
 *      Author: renard
 */

#ifndef HTML_TEXT_H_
#define HTML_TEXT_H_

#include "baseapi.h"
#include <string>

std::string GetHTMLText(tesseract::ResultIterator* res_it, const float minConfidenceToShowColor);

#endif /* OCR_UTIL_H_ */
