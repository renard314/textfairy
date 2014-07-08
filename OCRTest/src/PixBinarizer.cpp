/*
 * PixBinarizer.cpp
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#include "PixBinarizer.h"

PixBinarizer::PixBinarizer() {
}

 Pix* PixBinarizer::binarize(Pix* pix){
	 l_int32 depth = pixGetDepth(pix);
	 Pix* pixGrey;
	 Pix* pixtContrast;
	 Pix* pixBinary;
	 switch(depth){
		 case 1:
			 return pixClone(pix);
		 case 32:
			 pixGrey = pixConvertRGBToLuminance(pix);
	 }
	/* Do combination of contrast norm and sauvola */
	pixtContrast = pixContrastNorm(NULL, pixGrey, 100, 100, 55, 1, 1);
	pixSauvolaBinarizeTiled(pixtContrast, 9, 0.15, 1, 1, NULL, &pixBinary);

	pixDestroy(&pixtContrast);
	pixDestroy(&pixGrey);

	return pixBinary;
}


PixBinarizer::~PixBinarizer() {
}

