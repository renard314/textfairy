/*
 * canny.h
 *
 *  Created on: Jul 18, 2015
 *      Author: renard
 */

#ifndef CANNY_H_
#define CANNY_H_
#include "allheaders.h"


Pix* pixCannyEdge(Pix* pixs, l_float32 lower_treshhold, l_float32 upper_treshhold, bool debug);



#endif /* CANNY_H_ */
