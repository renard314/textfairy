/*
 * binarize.h
 *
 *  Created on: Feb 28, 2012
 *      Author: renard
 */

#ifndef BINARIZE_H_
#define BINARIZE_H_

#include <allheaders.h>


Pix* createEdgeMask(Pix* pixs);
Pix* binarizeTiled(Pix* pixs, const l_uint32 tileSize);
void binarize(Pix* pixGrey, Pix* pixhm, Pix** pixb);



#endif /* BINARIZE_H_ */
