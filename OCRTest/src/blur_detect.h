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
 
 *
 * blur_detect.h
 *
 *  Created on: Mon 04.05.2015
 *      Author: renard
 */

#ifndef BLUR_DETTECT_H_
#define BLUR_DETTECT_H_

#include <allheaders.h>

/**
 * Creates a mask which signifies the blurred regions of pixGrey. 255-->very blurred.
 */
Pix* pixMakeBlurMask(Pix* pixGrey, Pix* pixMedian, l_float32* blurValue);


/**
 * tints pixd according to the intensity values in pixmask
 */
void pixTintMasked(Pix* pixd, Pix* pixmask);



#endif /* BLUR_DETTECT_H_ */
