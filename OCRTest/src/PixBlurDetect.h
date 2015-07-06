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


class PixBlurDetect {
public:
	PixBlurDetect(bool debug);
	Pix* makeBlurIndicator(Pix* pix, l_float32* blurValue, Box** maxBlurBounds);
	virtual ~PixBlurDetect();

private:
	/**
	 * Creates a mask which signifies the blurred regions of pixGrey. 255-->very blurred.
	 */
	Pix* pixMakeBlurMask(Pix* pixGrey, Pix* pixMedian, l_float32* blurValue, Pix** pixBinary);

	/**
	 * Gets the average intensity ignoring OFF pixels.
	 */
	l_int32 pixGetAverageMasked(Pix *pixs, Pix* pixm,  l_float32  *pval);

	/**
	 * Tints pixd according to the intensity values in pixmask
	 */
	void pixTintMasked(Pix* pixd, Pix* pixmask);

	/**
	 * Creates a binary mask that covers the vertical edges.
	 */
	Pix* makeEdgeMask(Pix* pixs, l_int32  orientflag, l_int32* paverage);

	//void getValueBetweenTwoFixedColors(float value, int r, int g, int b, int &red, int &green, int &blue);
	void getValueBetweenTwoFixedColors(float value, int h1, int s1, int v1, int h2, int s2, int v2, int &red, int &green, int &blue);

	void getCenterOfGravity(Pix* pixs, l_uint32* cx, l_uint32* cy );
	Pix* blurTileTest(Pix* pixs, Pix* pixBlurMeasure);




	bool mDebug;
};



#endif /* BLUR_DETTECT_H_ */
