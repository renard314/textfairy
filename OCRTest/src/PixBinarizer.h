/*
 * PixBinarizer.h
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#ifndef PIXBINARIZER_H_
#define PIXBINARIZER_H_

#include <allheaders.h>

class PixBinarizer {
public:
	PixBinarizer();
	Pix* binarize(Pix* pix);

	virtual ~PixBinarizer();
};

#endif /* PIXBINARIZER_H_ */
