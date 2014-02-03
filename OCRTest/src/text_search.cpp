/*
 * text_search.cpp
 *
 *  Created on: Jan 31, 2014
 *      Author: renard
 */
#include "text_search.h"
#include <allheaders.h>
#include <sstream>

Pix* pixTreshold(Pix* pix){
	float scorefract = 0.4;
	int thresh;
	int nx = 2;
	int ny = 2;
	PIXTILING* pt = pixTilingCreate(pix, nx, ny, 0, 0, 0, 0);
	pixTilingGetCount(pt,&nx,&ny);
	Pix* pixth = pixCreate(nx, ny, 8);
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, j, i);
			int w = pixGetWidth(pixt);
			int h = pixGetHeight(pixt);
			printf("w=%i, h=%i",w,h);
			pixSplitDistributionFgBg(pixt, scorefract, 1, &thresh, NULL, NULL, 1);
			pixSetPixel(pixth, j, i, thresh);
			pixDestroy(&pixt);
		}
	}
	pixTilingDestroy(&pt);
	l_int32 w = pixGetWidth(pix);
	l_int32 h = pixGetHeight(pix);
	pt = pixTilingCreate(pix, nx, ny, 0, 0, 0, 0);
	Pix* pixeh = pixCreate(w, h, 1);
	l_uint32 val;
	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, i, j);
			pixGetPixel(pixth, j, i, &val);
			Pix* pixbTile = pixThresholdToBinary(pixt, val);
			pixTilingPaintTile(pixeh, i, j, pixbTile, pt);
			pixDestroy(&pixt);
			pixDestroy(&pixbTile);
		}
	}
	pixTilingDestroy(&pt);
	pixDestroy(&pixth);
	return pixeh;
}

Boxa* pixFindTextRegions(Pix* pix, Pix** pixb) {
	Pix* pix_edge = pixTwoSidedEdgeFilter(pix, L_HORIZONTAL_EDGES);
	Pix* pixeh = pixTreshold(pix_edge);
	if (pixb!=NULL){
		*pixb = pixeh;
	}


	std::ostringstream s;
	s << "o1.90+c20.1";
	//vertical whitespace mask
	Pix *pixvws = pixMorphCompSequence(pixeh, s.str().c_str(), 0);
	s.str("");
	s << "o1.5+o80.1+c1.20";
	//horizontal whitespace mask
	Pix *pixhws = pixMorphCompSequence(pixeh, s.str().c_str(), 0);


	//combine whitespace masks
	l_int32 w = pixGetWidth(pixeh);
	l_int32 h = pixGetHeight(pixeh);
	pixRasterop(pixvws, 0, 0, w, h, PIX_NOT(PIX_SRC | PIX_DST), pixhws, 0, 0);

	Boxa* tl = pixConnComp(pixvws, NULL, 8);
	l_int32 comp_count = boxaGetCount(tl);
	Pixa* edge_comp = pixaCreateFromBoxa(pixeh,tl,NULL);
	Numa* areaFraction = pixaFindAreaFraction(edge_comp);
	Numa* na1 = numaMakeThresholdIndicator(areaFraction, 0.91, L_SELECT_IF_LT);
	l_int32 ival;
	Boxa* filtered_boxa = boxaCreate(0);

	for (int i = 0; i < comp_count; i++) {
	        numaGetIValue(na1, i, &ival);
	        if (ival == 1) {
	        	Box* b = boxaGetBox(tl,i,L_CLONE);
	        	boxaAddBox(filtered_boxa,b,L_CLONE);
	        	boxDestroy(&b);
	        }
	}
	if(pixb==NULL) {
		pixDestroy(&pixeh);
	}
	pixDestroy(&pix_edge);
	pixDestroy(&pixvws);
	pixDestroy(&pixhws);
	boxaDestroy(&tl);
	pixaDestroy(&edge_comp);
	numaDestroy(&areaFraction);
	numaDestroy(&na1);

	return filtered_boxa;
}
