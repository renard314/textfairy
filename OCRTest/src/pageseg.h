/*
 * pageseg.h
 *
 *  Created on: Feb 29, 2012
 *      Author: renard
 */

#ifndef PAGESEG_H_
#define PAGESEG_H_

#include "image_processing.h"

Pixa* pagesegGetColumns(Pix* pixtext, bool debug);
Pix* combinePixa(Pixa* pixaText, bool debug);
void segmentComplexLayout(Pix* pixOrg, Pix* pixhm, Pix* pixb, Pixa** pixaImage, Pixa** pixaText, void(*callback) (const Pix*),bool debug);
void extractImages(Pix* pixOrg, Pix** pixhm, Pix** pixg);


#endif /* PAGESEG_H_ */
