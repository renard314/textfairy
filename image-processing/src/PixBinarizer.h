/*
 * PixBinarizer.h
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#ifndef PIXBINARIZER_H_
#define PIXBINARIZER_H_

#include "allheaders.h"

class PixBinarizer {
public:
	PixBinarizer(bool debug);
    Pix* binarize(Pix* pix,  void(*previewCallBack) (Pix*));

	virtual ~PixBinarizer();

private:
	void binarizeInternal(Pix* pixGrey, Pix* pixhm, Pix** pixb);
	Pix* binarizeTiled(Pix* pixs, const l_uint32 tileSize);
	Pix* createEdgeMask(Pix* pixs);
	int determineThresholdForTile(Pix* pixt, bool debug);




	bool mDebug;
};

#endif /* PIXBINARIZER_H_ */
