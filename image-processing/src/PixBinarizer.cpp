/*
 * PixBinarizer.cpp
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#include "PixBinarizer.h"
#include <stdio.h>
#include <iostream>
#include <sstream>
#include <cmath>
#include "image_processing.h"

using namespace std;

PixBinarizer::PixBinarizer(bool debug) {
    mDebug= debug;
}


int PixBinarizer::determineThresholdForTile(Pix* pixt, bool debug) {
    l_int32 start = 0, end = 255, i, error, closeSize = 0;
    l_float32 sum, moment, var, y, variance, mean, meanY, countPixels;
    
    NUMA* norm;
    NUMA* histo = pixGetGrayHistogram(pixt, 1);
    numaSetValue(histo, 255, 0); //ignore white pixels
    error = numaGetNonzeroRange(histo, 0, &start, &end);
    if (end == start || error == 1) {
        numaDestroy(&histo);
        return 0;
    }
    closeSize = end - start;
    if (closeSize % 2 == 0) {
        closeSize++;
    }
    norm = numaNormalizeHistogram(histo, 1);
    /*
     l_float32 median;
     numaGetMedian(histo,&median);
     printf("%2.1f\t", median);
     */
    
    l_float32 iMulty;
    for (sum = 0.0, moment = 0.0, var = 0.0, countPixels = 0, i = start;
         i < end; i++) {
        numaGetFValue(norm, i, &y);
        sum += y;
        iMulty = i * y;
        moment += iMulty;
        var += i * iMulty;
        numaGetFValue(histo, i, &y);
        countPixels += y;
    }
    variance = sqrt(var / sum - moment * moment / (sum * sum));
    mean = moment / sum;
    meanY = sum / (end - start);
    int result = 0;
    if (variance < 8) {
        result = 0;
    } else {
        if ((mean * 0.5) < variance) {
            result = mean;// + variance*0.4;
        } else {
            //high variance means we have probably have a very good separation between fore and background
            //in that case the threshold be closer to the mean
            result = mean - sqrt(variance) * 3;
            //result = mean - variance*0.66;
        }
        //result = mean - variance*0.66; //0.2 when median is activated, 0.66 otherwise
    }
    
    if (debug == true) {
        printf("mean = %f , variance = %f, meanY = %f \n", mean, variance,
               meanY);
        printf("%i ,%i\n", start, end);
        GPLOT *gplot;
        ostringstream name;
        name << mean;
        gplot = gplotCreate(name.str().c_str(), GPLOT_X11, name.str().c_str(),
                            "x", "y");
        ostringstream title;
        title << "mean = " << mean << ", " << "variance = " << variance
        << ", thresh = " << result;
        //gplotAddPlot(gplot, NULL, diff, GPLOT_LINES, "diff histogram");
        gplotAddPlot(gplot, NULL, norm, GPLOT_LINES, title.str().c_str());
        gplotMakeOutput(gplot);
        gplotDestroy(&gplot);
    }
    numaDestroy(&norm);
    numaDestroy(&histo);
    
    //TODO check std dev of y values. large value > foreground and background, low value > only background
    //idea: narrow histogramm means a high likelyhood for bg
    return result;
    
}

Pix* PixBinarizer::createEdgeMask(Pix* pixs) {
    L_TIMER timer = startTimerNested();
    ostringstream s;
    Pix* pixConv = pixBlockconvGray(pixs, NULL, 5, 5);
    Pix* pixConvEdges = pixSobelEdgeFilter(pixConv, L_ALL_EDGES);
    pixDestroy(&pixConv);
    pixInvert(pixConvEdges, pixConvEdges);
    s << "sobel edge detection: " << stopTimerNested(timer) << std::endl;
    timer = startTimerNested();
    if(mDebug){
        pixWrite("pixConvEdges.bmp", pixConvEdges, IFF_BMP);
    }
    
    NUMA* histo = pixGetGrayHistogram(pixConvEdges, 8);
    NUMA* norm = numaNormalizeHistogram(histo, 1.0);
    l_float32 median, mean, variance;
    numaGetHistogramStats(norm, 0, 1, &mean, &median, NULL, &variance);
    numaDestroy(&histo);
    numaDestroy(&norm);
    
    l_int32 thresh = 0;
    if (variance < 1.0) {
        thresh = 255;
    } else {
        thresh = 254;
    }
    if(mDebug){
        printf("mean = %f, median = %f, std dev = %f, thresh = %i\n", mean, median,
               variance, thresh);
    }
    
    Pix* pixForeground = pixThresholdToBinary(pixConvEdges, thresh);
    pixDestroy(&pixConvEdges);
    pixCloseSafeBrick(pixForeground,pixForeground,64,64);
    
    pixWrite("foreground.bmp", pixForeground, IFF_BMP);
    
    if(mDebug){
        s << "binarization of edge mask: " << stopTimerNested(timer) << std::endl;
    }
    timer = startTimerNested();
    
    Pix* pixacc = pixBlockconvAccum(pixForeground);
    Pix* pixRank = pixBlockrank(pixForeground, pixacc, 8, 8, 0.1);
    
    pixDestroy(&pixacc);
    pixDestroy(&pixForeground);
    pixInvert(pixRank, pixRank);
    Pix* pixResult = pixOpenBrick(NULL, pixRank, 10, 10);
    pixDestroy(&pixRank);
    if(mDebug){
        pixWrite("rank.bmp", pixResult, IFF_BMP);
        s << "mask generation: " << stopTimerNested(timer) << std::endl;
        printf("%s", s.str().c_str());
    }
    
    return pixResult;
}

/**
 * determines and applies a threshold for each tile separately
 */
Pix* PixBinarizer::binarizeTiled(Pix* pixs, const l_uint32 tileSize) {
    L_TIMER timer = startTimerNested();
    l_int32 thresh, w, h;
    Pix* pixb;
    ostringstream s;
    pixGetDimensions(pixs, &w, &h, NULL);
    l_int32 nx = L_MAX(1, w / tileSize);
    l_int32 ny = L_MAX(1, h / tileSize);
    l_int32 ox = L_MAX(1,nx/6);
    l_int32 oy = L_MAX(1,ny/6);
    PIXTILING* pt = pixTilingCreate(pixs, nx, ny, 0, 0, ox, oy);
    Pix* pixth = pixCreate(nx, ny, 8);
    Pix* pixt;
    bool debug = false;
    for (int i = 0; i < ny; i++) {
        for (int j = 0; j < nx; j++) {
            pixt = pixTilingGetTile(pt, i, j);
            if (i == 0 && j == 0) {
                debug = false;
            } else {
                debug = false;
            }
            thresh = determineThresholdForTile(pixt, debug);
            pixSetPixel(pixth, j, i, thresh);
            pixDestroy(&pixt);
        }
    }
    pixTilingDestroy(&pt);
    if(mDebug){
        s << "local threshhold determination: " << stopTimerNested(timer)<< std::endl;
        timer = startTimerNested();
    }
    
    pt = pixTilingCreate(pixs, nx, ny, 0, 0, 0, 0);
    pixb = pixCreate(w, h, 1);
    l_uint32 val;
    for (int i = 0; i < ny; i++) {
        for (int j = 0; j < nx; j++) {
            pixt = pixTilingGetTile(pt, i, j);
            pixGetPixel(pixth, j, i, &val);
            Pix* pixbTile = pixThresholdToBinary(pixt, val);
            pixTilingPaintTile(pixb, i, j, pixbTile, pt);
            pixDestroy(&pixt);
            pixDestroy(&pixbTile);
        }
    }
    pixTilingDestroy(&pt);
    pixDestroy(&pixth);
    if(mDebug){
        s << "local threshhold application: " << stopTimerNested(timer)<< std::endl;
        printf("%s", s.str().c_str());
    }
    
    return pixb;
}

void PixBinarizer::binarizeInternal(Pix* pixGrey, Pix* pixhm, Pix** pixb) {
    Pix* pixEdgeMask;
    l_int32 width = pixGetWidth(pixGrey);
    const l_uint32 tileSize = width/10; //size of tile during threshholding
    L_TIMER timer = startTimerNested();
    ostringstream s;
    
    pixEdgeMask = createEdgeMask(pixGrey);
    
    pixSetMasked(pixGrey, pixEdgeMask, 255);
    if (pixhm != NULL) {
        //dont allow image mask to cover text
        pixAnd(pixhm, pixhm, pixEdgeMask);
    }
    
    pixDestroy(&pixEdgeMask);
    if(mDebug){
        s << "text mask creation: " << stopTimerNested(timer) << std::endl;
        printf("%s", s.str().c_str());
        pixDisplay(pixGrey,0,0);
        pixWrite("toThresh.bmp", pixGrey, IFF_BMP);
    }
    
    /*
     timer = startTimerNested();
     Pix* pixMedian = pixRankFilterGray(pixGrey,3,3,0.25);
     printf("rank filter = %f\n",stopTimerNested(timer));
     pixWrite("rank.bmp",pixMedian,IFF_BMP);
     */
    
    *pixb = binarizeTiled(pixGrey, tileSize);
    //pixDestroy(&pixMedian);
    
    if (pixhm != NULL) {
        pixSetMasked(*pixb, pixhm, 0);
    }
}

Pix *PixBinarizer::binarize(Pix *pix, void(*previewCallBack)(Pix *)) {
    
    l_int32 depth = pixGetDepth(pix);
    Pix* pixGrey = NULL;
    Pix* pixtContrast;
    Pix* pixBinary;
    switch(depth){
        case 1:
            return pixClone(pix);
        case 8:
            pixGrey = pixClone(pix);
            break;
        case 32:
            pixGrey = pixConvertRGBToLuminance(pix);
    }
    binarizeInternal(pixGrey, NULL, &pixBinary);
    
    /* Do combination of contrast norm and sauvola */
    //	pixtContrast = pixContrastNorm(NULL, pixGrey, 100, 100, 55, 1, 1);
    //	pixSauvolaBinarizeTiled(pixtContrast, 9, 0.15, 1, 1, NULL, &pixBinary);
    //  pixDestroy(&pixtContrast);
    pixDestroy(&pixGrey);
    
    return pixBinary;
}


PixBinarizer::~PixBinarizer() {
}

