/*
 * SkewCorrector.h
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#ifndef SKEWCORRECTOR_H_
#define SKEWCORRECTOR_H_
#include "allheaders.h"

class SkewCorrector {
public:
	SkewCorrector(bool debug);
	Pix* correctSkew(Pix* pix, l_float32* angle);
	virtual ~SkewCorrector();

private:
    Pix* rotate(Pix* pix, l_float32 angle);
	static l_float32 const DEG_2_RAD;
	bool mDebug;
};

#endif /* SKEWCORRECTOR_H_ */
