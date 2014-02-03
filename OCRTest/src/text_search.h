/*
 * text_search.h
 *
 *  Created on: Jan 31, 2014
 *      Author: renard
 */

#ifndef TEXT_SEARCH_H_
#define TEXT_SEARCH_H_

#include "image_processing.h"
#include <vector>

Pix* pixTreshold(Pix* pix);
Boxa* pixFindTextRegions(Pix* pix,Pix** pixb);

#endif /* TEXT_SEARCH_H_ */
