/*
 * SkewCorrector.cpp
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#include "SkewCorrector.h"

l_float32 const SkewCorrector::DEG_2_RAD = 3.1415926535 / 180.;

SkewCorrector::SkewCorrector(bool debug) {
	mDebug = debug;
}


Pix* SkewCorrector::correctSkew(Pix* pix){
    PROCNAME("SkewCorrector::correctSkew");
	if(pix==NULL){
		l_warning("pixs not defined",procName);
		return NULL;
	}
	l_int32 depth = pixGetDepth(pix);
	if(depth!=1){
		l_warning("pixs not 1 bpp",procName);
		return NULL;
	}

	Pix* pixRotated;
	l_float32 angle;
	l_int32 error = pixFindSkewSweep(pix, &angle, 1, 47., 1.);
	if (error == 1) {
		pixRotated = pixClone(pix);
	} else {
		if(mDebug){
			printf("rotating image by %f degrees\n", angle);
		}
		//rotate binary image
		pixRotated = pixRotate(pix, DEG_2_RAD * angle, L_ROTATE_AREA_MAP, L_BRING_IN_WHITE, 0, 0);
	}
	return pixRotated;
}


SkewCorrector::~SkewCorrector() {
}

