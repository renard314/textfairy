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
 * pageseg.h
 *
 *  Created on: Feb 29, 2012
 *      Author: renard
 */

#ifndef PAGESEG_H_
#define PAGESEG_H_

#include "image_processing.h"

l_int32 renderTransformedBoxa(PIX *pixt, BOXA *boxa, l_int32 i);
Pixa* pagesegGetColumns(Pix* pixtext, bool debug);
Pix* combinePixa(Pixa* pixaText, bool debug);
void segmentComplexLayout(Pix* pixOrg, Pix* pixhm, Pix* pixb, Pixa** pixaImage, Pixa** pixaText, void(*callback) (const Pix*),bool debug);
void extractImages(Pix* pixOrg, Pix** pixhm, Pix** pixg);


#endif /* PAGESEG_H_ */
