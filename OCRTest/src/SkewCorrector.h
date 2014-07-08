/*
 * SkewCorrector.h
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#ifndef SKEWCORRECTOR_H_
#define SKEWCORRECTOR_H_
#include <allheaders.h>

class SkewCorrector {
public:
	SkewCorrector();
	Pix* correctSkew(Pix* pix);
	virtual ~SkewCorrector();

private:
	static l_float32 const DEG_2_RAD;
};

#endif /* SKEWCORRECTOR_H_ */
