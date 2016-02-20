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
Pix* pixTreshold2(Pix* pix);
Boxa* pixFindTextRegions(Pix* pix,Pix** pixb);

Pixa* pixFindTextBlocks(Pix* pix);
Pix* pixCreateTextBlockMask(Pix* pixb);
Pix* pixThresholdToBinary(Pix* pixg);
Pixa* pixFindVerticalWhiteSpaceAtEdges(Pix* pixTextBlock);
Numa* pixaFindTextBlockIndicator(Pixa* pixaText, l_int32 width, l_int32 height, Numa** numaEdgeCandidates);
l_float32 pixGetTextLineSpacing(Pix* pixb);
void numaGroupTextLineHeights(Numa* numaTextHeights, Numa** numaMean, Numa** numaStdDev, Numa** numaCount);
Pixa* pixGetTextBlocks(l_float32 textLineSpacing, Pix* pixb,Pix** pixmorphout);


#endif /* TEXT_SEARCH_H_ */
