//
//  textsize.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 24/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//
#define DEBUG 0

#include "textsize.hpp"
#include "leptonica_legacy.h"
#include "RunningStats.h"
#include "pixFunc.hpp"

#if DEBUG
#include "plot.hpp"
#endif

Numa* numaDifferentiate(Numa* na) {
    Numa* result = numaCreate(0);
    l_int32 pc = numaGetCount(na);
    l_float32 prev;
    if(pc>1){
        prev = numaGetFValue(na,0,&prev);
        for (int i = 1; i < pc; i++) {
            l_float32 val;
            numaGetFValue(na, i, &val);
            numaAddNumber(result,prev-val);
            prev = val;
        }
    }
    return result;
}

l_float32 pixGetTextLineHeightGeneral(Pix* pix, l_int32* pLineCount){
    l_float32 lineSpacing = 0;
    if (pixGetDepth(pix)==1){
        lineSpacing = pixGetTextLineHeight(pix, 1, 1, 5, pLineCount);
    } else {
        Pix* edges = run(pix, {convertTo8, reduceGray4, savGol32, edgeDetect, invert, edgeBinarize});
        lineSpacing = 4 * pixGetTextLineHeight(edges, 1, 1, 5, pLineCount);
        pixDestroy(&edges);
    }
    return lineSpacing;
}

//separate peaks from valleys using otsu
l_int32 findPeakValleyTresh(Numa* numa){
    l_int32 binSize;
    Numa* histo = numaMakeHistogram(numa, 256, &binSize, NULL);
    l_int32 index;
    numaSplitDistribution(histo, 0.1, &index, NULL, NULL, NULL, NULL, NULL);
    l_int32 thresh = index*binSize;
    
#if DEBUG
    Numa* numaThresh = numaCreate(0);
    numaAddNumber(numaThresh, thresh);
    numaPlot(histo, numaThresh, NULL, GPLOT_PNG, "histo");
    numaDestroy(&numaThresh);
#endif
    numaDestroy(&histo);
    return thresh;

}

Numa* extractTextLineHeights(Numa* numaPixelSum, l_int32 thresh){
    Numa* textLineHeights = numaCreate(0);
    
#if DEBUG
    Numa* crossings = numaCreate(0);
#endif
    
    l_int32 lastIndex = -1;
    l_int8 whiteSpace = 0;
    l_int32 count = numaGetCount(numaPixelSum);
    for(int i = 1; i < count; i++ ) {
        l_int32 yVal;
        numaGetIValue(numaPixelSum, i, &yVal);
        if(yVal<thresh){
            //cross from text line white space
            //measure last textline
            if(lastIndex!=-1 && !whiteSpace){
#if DEBUG
                numaAddNumber(crossings, lastIndex);
                numaAddNumber(crossings, i);
#endif
                numaAddNumber(textLineHeights, i-lastIndex);
                lastIndex=-1;
            }
            whiteSpace = 1;
        } else {
            //cross from whitespace to new text line
            if(whiteSpace){
                lastIndex = i;
            }
            whiteSpace=0;
        }
    }
    
#if DEBUG
    numaPlot(numaPixelSum, crossings, NULL, GPLOT_PNG, "lineHeights");
    numaDestroy(&crossings);
#endif
    
    return textLineHeights;
}

/*!
 * \brief   pixGetTextLineHeight()
 *
 * \param[in]    binary pix
 * \return  line height of text, or 0 if not at least 5 text lines were found
 */
l_float32 pixGetTextLineHeight(Pix* pix, l_int32 numTilesY, l_int32 numTilesX, l_int32 minLineCount, l_int32* pLineCount){
    int nx, ny;
    
    PIXTILING* pt = pixTilingCreate(pix, numTilesX, numTilesY, 0, 0, 0, 0);
    pixTilingGetCount(pt, &nx, &ny);
    RunningStats stats;
    l_int32 lines=0;
    
    for (int i = 0; i < ny; i++) {
        for (int j = 0; j < nx; j++) {
            Pix* pixt = pixTilingGetTile(pt, i, j);
            Numa* numaPixelSum = pixSumPixelsByRow(pixt, NULL);
            l_int32 thresh = findPeakValleyTresh(numaPixelSum);
            Numa* textLineHeights = extractTextLineHeights(numaPixelSum, thresh);
            l_int32 lineCount = numaGetCount(textLineHeights);
            l_float32 textLineHeight=0;
            if(lineCount>=minLineCount){
                numaGetRankValue(textLineHeights, 0.5, NULL, 0, &textLineHeight);
                stats.Push(textLineHeight);
            }
            
            pixDestroy(&pixt);
            numaDestroy(&textLineHeights);
            numaDestroy(&numaPixelSum);
        }
    }
    if(pLineCount!=NULL){
        *pLineCount = lines;
    }
    pixTilingDestroy(&pt);
    return stats.Mean();
}
